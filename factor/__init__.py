import os
import os.path
import sqlite3
from contextlib import contextmanager
from uuid import uuid4

from dearpygui.core import *
from dearpygui.simple import *
import typer

from .state import State

INIT = []

from .item_editor import show_item_editor
from .factory import create_and_open_factory


def main():

    add_data(
        "State",
        State(
            {
                "world": {
                    "items": {},
                    "processors": {},
                    "recipes": {},
                    "factories": {},
                },
                "ui": {},
            }
        ),
    )

    [init() for init in INIT]

    with window("Factory Calculator"):

        with menu_bar("Main Menu Bar"):

            with menu("Factory"):
                add_menu_item("New Factory...", callback=create_and_open_factory)

            with menu("World"):
                add_menu_item("Edit items...", callback=show_item_editor)

            with menu("Debug"):
                add_menu_item("About window...", callback=show_about)
                add_menu_item("Debug window...", callback=show_debug)
                add_menu_item("Log window...", callback=show_logger)
                add_menu_item("Style editor...", callback=show_style_editor)

    try:
        add_additional_font("C:\\Windows\\Fonts\\Consola.ttf", 16)
    except:
        log_error("Failed loading font")
    set_theme("Light")

    start_dearpygui(primary_window="Factory Calculator")


def cli():
    typer.run(main)
