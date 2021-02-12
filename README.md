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

``` bash
# install deps
npm i

# start dev server with reloading and nREPL
npm run start

# build production
npm run build
```