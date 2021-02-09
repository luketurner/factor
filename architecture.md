# Factor Application Architecture

As a GUI application, Factor does not execute code in a predictably linear fashion. It has two main phrases:

1. Initialization code (which _is_ predictable) that constructs the GUI and wires up callbacks.
2. The main loop while the GUI is running, during which Factor responds to user input and redraws UI elements accordingly.

A key challenge is managing state: responding to user input and updating the GUI accordingly. 

## Initialization

Initialization tasks are collected in the `factor.INIT` array. Modules add their initialization functions to this array, and when the GUI is started the init functions are called sequentially.

For GUI components, init functions usually do three things:

1. Build any static GUI elements that are not dependent on state. (e.g. windows, containers)
2. Initialize any "default state" the component might need.
3. Wire an observer to detect and respond to state changes.

## Main Loop

The main loop is basically a `while True` loop that checks if there was any events in the GUI, and triggers the appropriate event handlers/callbacks. For Factor, this loop is internal to the `dearpygui` library, and all we have to do is handle the callbacks that `dearpygui` calls for us.

During this phase of execution, Factor uses the principles of _unidirectional data flow_ and _centralized state_ to simplify implementation:

1. All application state (including transient UI state) is stored in a nested dictionary called a "state tree."
2. GUI callbacks can query and mutate the state tree, and nothing else.
3. UI updates (adding/removing widgets, changing values, etc.) can only happen in observer functions (or init functions).

(As a side-note, this approach was inspired by my experience with state management for Web applications, which I [wrote about](https://blog.luketurner.org/posts/unidirectional-data-flow-js/) in more detail on my blog.)
