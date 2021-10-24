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
  - [x] Recipes have a configurable :duration
  - [ ] The list of compatible machines can be prioritized.
  - [ ] Recipes can specify a power modifier.
  - [x] Recipes can include *catalysts* (items only required at startup, not per craft).
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
- [x] All quantities/rates have *units* (e.g. J, W, items/sec, etc.)
  - [x] Units are configurable in settings.
  - [ ] Units are presented in human-readable form (e.g. 2.3 MW instead of 2300 W)
  - [ ] Units support conversions between them (e.g. changing unit from items/sec to items/min converts values by multiplying by 60)
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

## Glossary

- **Catalyst**: A catalyst is a one-time requirement for crafting recipes. Catalysts can either be items that aren't consumed in crafting (e.g. a "Gear casting mold" used to make gears) or that are consumed to kickstart a self-sustaining reaction (e.g. a hohlraum).
- **Factory**: Represents a player-built, self-contained production system that produces some *desired outputs*. For example, perhaps your factory's desired output is "One supercomputer per second." The factory contains all the various machines required to produce the output, which could include multiple stages of complex crafting.
- **Item**: Represents an in-game item that is produced/consumed/conveyed/stored. e.g. "Iron ore" would be an item, and so would "Water" and "Research cube."
- **Preset**: A preset is a built-in *world* that ships with Factor and can be imported from the Settings page. (Note: I haven't built out any presets yet, but the framework is in place.)
- **Production Graph (pgraph)**: A directed, cyclic graph that represents the flow of *items* within a *factory*. The nodes of the graph represent a bank of machines in the factory, all processing the same recipe (e.g. three Smelters using the "Smelting iron ore" recipe would be grouped in a single node.) The edges of the graph represent the flow of items between the banks of machines. Factor can automatically calculate a Production Graph for your factories.
- **Production Tree**: A simplified view of a Production Graph, used for display purposes. Some types of pgraphs can be directly represented as a tree, but many pgraphs don't have a tree-like structure, in which case the calculated production tree will have to list some nodes more than once (as children of different parents).
- **Recipe**: Represents an in-game crafting recipe. The recipe describes a crafting operation that consumes certain *inputs* to produce some *outputs*. (e.g. consuming iron ore to produce iron plates.) Recipes can also specify *catalysts* required to bootstrap crafting, what *machines* can be used to craft, and a *duration* of the crafting operation. (Note that even resource collection is modeled as a recipe: the ability to mine coal would be represented as a recipe that outputs coal and has no inputs.)
- **World**: The "world" is a piece of data that encapsulates everything that models in-game entities -- namely, all *items*, *machines*, *recipes*, and *factories*. It *doesn't* contain personal configuration (e.g. units) or UI state (e.g. which factory is open by default). Factor can import/export worlds; it's recommended to export your world occasionally for backup purposes. You can also share it with your friends!

## Developer-facing terms

Used in Factor's codebase and developer documentation.

- **Database**: Factor's "database" is just an *in-memory map* accessed using the conventions of [re-frame](https://github.com/day8/re-frame). There is NO server-side database in Factor (or indeed, any network access at all!)
- **Migration**: In the context of Factor, a "migration" is a function that performs some modifications on application data when it's loaded from local storage or imported by the user. Whenever a breaking change is made in the schema, a corresponding migration function is defined to update old versions of the database. All migrations are idempotent and are always applied; there is no state tracking of which migrations have/haven't been run.
- **Quantity map (qmap)**: Widely used in the codebase, a `qmap` is a hash-map where keys are opaque strings (usually the IDs of items/machines) and values are a numeric quantity. Quantities can be floating-point, but zero or negative values are not allowed in a `qmap`.
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