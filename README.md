# factory-calculator

A GUI companion tool for factory-builder games like Factorio or Satisfactory.

Usage:

```
poetry run factorycalculator
```

The tool is game-agnostic, so it needs to be "told" about what items, recipes, etc. are available in the game you're playing.

- **Items** are that which flows through the factory and get used up or produced by Recipes.
- **Recipes** are all the available ways items can be created or destroyed or transformed within the system. Recipes are performed at Processors.
- **Processors** are the things that create, destroy, or transform items in accordance with the available Recipes. (i.e. in a game like Factorio, a processor is a machine.)

These things are all "fixed," and don't need to be changed unless you wish to use the tool for a different game (or a different selection of mods, or you unlocked new research, etc.) So, once they're all added, make sure to **save your world** (which creates a sqlite file) because otherwise it'll all be lost when you close the window!

The next step, when we'll actually start doing useful calculations, is creating a **Factory**. Your world can have many Factories. Each Factory can specify desired outputs (in the form of items), and will calculate various rates of processing required to achieve the desired output.