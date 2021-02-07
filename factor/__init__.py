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

from .menu import add_main_menu

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
        add_main_menu()

    try:
        add_additional_font("C:\\Windows\\Fonts\\Consola.ttf", 16)
    except:
        log_error("Failed loading font")

    set_theme("Light")
    start_dearpygui(primary_window="Factory Calculator")


def cli():
    typer.run(main)
