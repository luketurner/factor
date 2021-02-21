# factor

> **Warning:** This project isn't finished -- it probably doesn't work yet!

A companion Web app for factory-builder games like Factorio or Satisfactory.

Factor is game-agnostic, so it needs to be "told" about what items, recipes, etc. are available in the game you're playing.

- **Items** are that which flows through the factory and get used up or produced by Recipes. (Note that, for example, "fluids" are treated as items in this system.)
- **Recipes** are all the available ways items can be created or destroyed or transformed within the system. Recipes are performed at Processors.
- **Machines** are the things that create, destroy, or transform items in accordance with the available Recipes.

These things are usually pretty consistent, and don't need to be changed unless you wish to use the tool for a different game (or a different selection of mods, or you unlocked new research, etc.)

The next step, when we'll actually start doing useful calculations, is creating a **Factory**. Your world can have many Factories. Each Factory can specify desired outputs (in the form of items), and will calculate various rates of processing required to achieve the desired output.


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