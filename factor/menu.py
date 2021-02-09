from dearpygui.core import *
from dearpygui.simple import *
from toolz import get_in

from . import INIT
from .factory import create_and_show_factory, show_factory
from .item_editor import show_item_editor

def add_main_menu():
    with menu_bar("Main Menu Bar"):

        with menu("Factory"):
            add_menu_item("New Factory...", callback=create_and_show_factory)
            with menu("MainMenuFactoryOpen", label="Open factory", enabled=False):
                pass

        with menu("World"):
            add_menu_item("Edit items...", callback=show_item_editor)

        with menu("Debug"):
            add_menu_item("About window...", callback=show_about)
            add_menu_item("Debug window...", callback=show_debug)
            add_menu_item("Log window...", callback=show_logger)
            add_menu_item("Style editor...", callback=show_style_editor)

def _observer(patch, new, old):
    if get_in(["world", "factories"], patch):
        delete_item("MainMenuFactoryOpen", children_only=True)
        new_factories = get_in(["world", "factories"], new)
        if new_factories:
            configure_item("MainMenuFactoryOpen", enabled=True)
            for factoryid, factory in new_factories.items():
                add_menu_item(f"MainMenuFactoryOpen-{factoryid}", label=factory["name"], parent="MainMenuFactoryOpen", callback=lambda: show_factory(factoryid))
        else:
            configure_item("MainMenuFactoryOpen", enabled=False)


def _init():
    state = get_data("State")
    state.observe("menu", _observer)

INIT.append(_init)