# factor

Factor is a Web app that aims to be an alternative to spreadsheets for calculating production rates and ratios in factory games (like Factorio or Satisfactory).

Try it online at: https://factor.luketurner.org. (Or you can run it yourself! See the [Development](#Development) section.)

Factor is...

- Entirely client-side (everything is stored in your browser's local storage.)
- Game-agnostic (The available items/recipes/etc. are fully editable.)
- Written with [Clojurescript](https://clojurescript.org/) and [re-frame](https://github.com/day8/re-frame) (among other dependencies!)

Basic usage:

1. Input all the **Items**, **Machines**, and **Crafting Recipes** available in the game you're playing.
    - Note that in Factor, different types of items -- fluid, gas, etc. -- are all treated the same way.
2. Create a **Factory** and set the Desired Output of the factory to the items you want to produce (e.g. "1 blue science per second").
3. Factor will calculate a **Production Graph** for the factory that indicates exactly what machines and recipes you need to meet your desired output.


## Features

The following is a list of high-level features/capabilities that I want to build into Factor.

- [x] Checked-off features like this are **supported**.
- [ ] Unchecked features are **unsupported**, but may be supported in the future.

Here's the list:

- [x] Create/edit *Items* (e.g. `Iron ore`).
- [x] Create/edit *Machines* (e.g. `Smelter`).
  - [x] Machines can have a *power*.
  - [x] Machines can have a *speed*.
- [x] Create/edit *Recipes* (e.g. `Smelt iron ore`).
  - [x] Recipes can have any number of inputs and outputs.
  - [x] Recipes can specify a list of compatible machines.
  - [ ] The list of compatible machines can be prioritized.
  - [ ] Recipes can specify a power modifier.
  - [ ] Recipes can include *catalysts*.
- [x] Create/edit *Factories* (e.g. `Starter base`)
  - [x] Factories have configurable *desired outputs* (e.g. 1 blue research per second).
  - [x] Factories can generate a *Production Graph* that shows what recipes can be used to produce the desired output.
    - [x] The production graph is rendered as a Production Tree for users to visualize.
    - [ ] Circular dependencies are supported. (when Recipe A's output is required for Recipe B, and Recipe B's output is required for a predecessor of Recipe A)
      - [ ] The "seed items" required for the circular dependency are calculated and included in the factory's catalysts.
    - [ ] Production graph calculation takes machine speed into account.
  - [ ] Factories can specify machines/recipes/items to exclude (e.g. don't use Recipe A for given factory.)
  - [ ] Factories can report the overall power usage required for all machines.
  - [ ] Factories include a "checklist" of all the machines needed to build the factory.
- [ ] All quantities/rates have *units* (e.g. MJ, kW, items/sec, etc.)
  - [ ] Units are configurable in settings.
- [ ] URL-based routing (including pushing history and supporting deep links to sub-pages.)
  - [ ] Ability to generate "share URLs" that contain your whole world in the URL.
- [x] Import/Export
  - [x] World data can be exported as EDN
  - [x] EDN world data can be imported.
  - [x] You can load presets from popular games
    - [ ] Factorio
- [ ] Bulk-Editing
  - [ ] Ability to bulk-edit recipes (e.g. remove a certain machine from a bunch of recipes at once)
- [ ] Conveyor/Inserter Calculator
## Development

This section of the README describes how to compile and run Factor on your own computer. Note that this isn't necessary for normal usage (you can visit https://factor.luketurner.org instead), but is necessary if you want to hack on Factor yourself.

Factor source code is available in two locations:

- https://git.sr.ht/~luketurner/factor
- https://github.com/luketurner/factor

The project is written with Clojurescript and compiled with [shadow-cljs](https://github.com/thheller/shadow-cljs). In order to compile the site, you will need:

- Java SDK 8
- Node 6+ (w/NPM)

Before you build the site, install Node dependencies (this will pull in the `shadow-cljs` toolchain as well as runtime JS dependencies):

``` bash
git clone https://git.sr.ht/~luketurner/factor
cd factor
npm install
```

All common operations in the project are exposed as NPM scripts. To generate a minified, production-ready bundle, run:

``` bash
npm run build
```

The resulting site will be in the `public/` directory, and can be served by any HTTP server, for example:

``` bash
cd public
python -m http.server
```

For building/running Factor during development, run:

``` bash
npm run start
```

This will:

- Compile Factor and automatically recompile when source files are modified.
- Run tests and automatically re-run when source files are modified.
- Serve Factor at `localhost:8080`
- Provide an nREPL server

(Tip: The VSCode extension [Calva](https://github.com/BetterThanTomorrow/calva) provides quick-and-easy nREPL integration: Install the extension, run `npm run start`, and do `Ctrl-Alt-c Ctrl-Alt-c`, then hit `Enter` three times, and you've got a REPL!)

My "official" version of Factor ([factor.luketurner.org](https://factor.luketurner.org)) is published from https://git.sr.ht/~luketurner/factor, via [builds.sr.ht](https://builds.sr.ht/) and [Netlify](https://www.netlify.com/).

To run tests like the CI/CD pipeline will, use:

``` bash
npm run test
```