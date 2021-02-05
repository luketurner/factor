import os
import os.path
import sqlite3
from contextlib import contextmanager

from dearpygui.core import *
from dearpygui.simple import *
import typer

_dbfilename = None


@contextmanager
def get_conn(readonly=True):
    global _dbfilename

    # Note -- in-memory db uses cache=shared so it persists across connections/threads
    conn = (
        sqlite3.connect(_dbfilename)
        if _dbfilename
        else sqlite3.connect("file:tempdb?mode=memory&cache=shared", uri=True)
    )
    try:
        yield conn
        conn.commit()
    except:
        conn.rollback()
    finally:
        conn.close()
        if not readonly:
            refresh_data()


# TODO -- for now, all refresh functions are wired up here...
def refresh_data():
    if is_dearpygui_running():
        is_item_shown("Item List") and item_list_refresh()
        is_item_shown("Item Editor") and item_editor_refresh()
        is_item_shown("Recipe List") and recipe_list_refresh()
        is_item_shown("Recipe Editor") and recipe_editor_refresh()
        is_item_shown("Processor List") and processor_list_refresh()
        is_item_shown("Processor Editor") and processor_editor_refresh()


def set_database_file(filename):
    global _dbfilename
    _dbfilename = filename


def migrate():
    with get_conn(readonly=False) as conn:
        log_debug("Migrating...")
        conn.executescript(migration_query)


def open_database(sender, data):
    open_file_dialog(callback=open_database_apply)


def open_database_apply(sender, data):
    database_file = os.path.join(data[0], data[1])
    if not os.path.exists(database_file):
        raise Error("Database doesn't already exist")
    log_debug("Connecting to database: " + database_file)
    set_database_file(database_file)


def rename_database(sender, data):
    open_file_dialog(callback=rename_database_apply)


def rename_database_apply(sender, data):
    new_filename = os.path.join(data[0], data[1])
    if os.path.exists(new_filename):
        raise Error("Database already exists")
    log_debug("Saving world as: " + new_filename)
    with get_conn() as old_conn:
        set_database_file(new_filename)
        with get_conn(readonly=False) as new_conn:
            old_conn.backup(new_conn)
    log_debug("Done saving: " + new_filename)


##
## ITEM LIST
##


def item_list_open(sender, data):
    show_item("Item List")
    item_list_refresh()


def item_list_update(sender, data):
    ids = item_list_get_selected()
    if len(ids) > 0:
        set_value("Item Editor ID", ids[0])
        item_editor_refresh()


def item_list_add(sender, data):
    with get_conn(readonly=False) as conn:
        conn.execute("insert into item (name) values ('Unnamed Item')")


def item_list_get_selected():
    return [
        get_table_item("Items", row, 0) for row, col in get_table_selections("Items")
    ]


def item_list_delete(sender, data):
    with get_conn(readonly=False) as conn:
        [
            conn.execute("delete from item where id = ?", (x,))
            for x in item_list_get_selected()
        ]


def item_list_refresh():
    clear_table("Items")
    with get_conn() as conn:
        for row in conn.execute("select * from item"):
            add_row("Items", row)


##
## ITEM EDITOR
##


def item_editor_open():
    show_item("Item Editor")
    item_editor_refresh()


def item_editor_refresh():
    rowid = get_value("Item Editor ID")
    with get_conn() as conn:
        row = conn.execute("select * from item where id = ?", (rowid,)).fetchone()
    if row:
        set_value("Item Editor Name", row[1])
    else:
        set_value("Item Editor Name", "UNKNOWN ITEM ID")


def item_editor_update_name():
    value = get_value("Item Editor Name")
    rowid = get_value("Item Editor ID")
    if rowid:
        with get_conn(readonly=False) as conn:
            conn.execute("update item set name = ? where id = ?", (value, rowid))


##
## PROCESSOR LIST
##


def processor_list_open():
    show_item("Processor List")
    processor_list_refresh()


def processor_list_update():
    ids = processor_list_get_selected()
    if len(ids) > 0:
        set_value("Processor Editor ID", ids[0])
        processor_editor_refresh()


def processor_list_add():
    with get_conn(readonly=False) as conn:
        conn.execute("insert into processor (name) values ('Unnamed Processor')")


def processor_list_get_selected():
    return [
        get_table_item("Processors", row, 0)
        for row, col in get_table_selections("Processors")
    ]


def processor_list_delete():
    with get_conn(readonly=False) as conn:
        [
            conn.execute("delete from processor where id = ?", (x,))
            for x in processor_list_get_selected()
        ]


def processor_list_refresh():
    clear_table("Processors")
    with get_conn() as conn:
        for row in conn.execute("select * from processor"):
            add_row("Processors", row)


##
## PROCESSOR EDITOR
##


def processor_editor_open():
    show_item("Processor Editor")
    processor_editor_refresh()


def processor_editor_refresh():
    rowid = get_value("Processor Editor ID")
    with get_conn() as conn:
        row = conn.execute("select * from processor where id = ?", (rowid,)).fetchone()
    if row:
        set_value("Processor Editor Name", row[1])
    else:
        set_value("Processor Editor Name", "UNKNOWN PROCESSOR ID")


def processor_editor_update_name():
    value = get_value("Processor Editor Name")
    rowid = get_value("Processor Editor ID")
    if rowid:
        with get_conn(readonly=False) as conn:
            conn.execute("update processor set name = ? where id = ?", (value, rowid))


##
## RECIPE LIST
##


def recipe_list_open():
    show_item("Recipe List")
    recipe_list_refresh()


def recipe_list_update():
    ids = recipe_list_get_selected()
    if len(ids) > 0:
        set_value("Recipe Editor ID", ids[0])
        recipe_editor_refresh()


def recipe_list_add():
    with get_conn(readonly=False) as conn:
        conn.execute("insert into recipe (name) values ('Unnamed Recipe')")


def recipe_list_get_selected():
    return [
        get_table_item("Recipes", row, 0)
        for row, col in get_table_selections("Recipes")
    ]


def recipe_list_delete():
    with get_conn(readonly=False) as conn:
        [
            conn.execute("delete from recipe where id = ?", (x,))
            for x in recipe_list_get_selected()
        ]


def recipe_list_refresh():
    clear_table("Recipes")
    with get_conn() as conn:
        for item in conn.execute("select * from recipe"):
            add_row("Recipes", item)


##
## RECIPE EDITOR
##


def recipe_editor_open():
    show_item("Recipe Editor")
    recipe_editor_refresh()


def recipe_editor_refresh():
    rowid = get_value("Recipe Editor ID")
    with get_conn() as conn:
        row = conn.execute("select * from recipe where id = ?", (rowid,)).fetchone()
    if row:
        set_value("Recipe Editor Name", row[1])
    else:
        set_value("Recipe Editor Name", "UNKNOWN RECIPE ID")


def recipe_editor_update_name():
    value = get_value("Recipe Editor Name")
    rowid = get_value("Recipe Editor ID")
    if rowid:
        with get_conn(readonly=False) as conn:
            conn.execute("update recipe set name = ? where id = ?", (value, rowid))


##
## GENERAL
##


def main():

    try:
        add_additional_font("C:\\Windows\\Fonts\\Consola.ttf", 16)
    except:
        log_error("Failed loading font")

    with window("Item List"):
        add_button("Item List Add", label="Add", callback=item_list_add)
        add_button("Item List Delete", label="Delete", callback=item_list_delete)
        add_table("Items", ["ID", "Name"], callback=item_list_update)

    with window("Item Editor"):
        add_input_text("Item Editor ID", label="ID", callback=item_editor_refresh)
        add_input_text(
            "Item Editor Name", label="Name", callback=item_editor_update_name
        )

    with window("Processor List"):
        add_button("Processor List Add", label="Add", callback=processor_list_add)
        add_button(
            "Processor List Delete", label="Delete", callback=processor_list_delete
        )
        add_table("Processors", ["ID", "Name"], callback=processor_list_update)

    with window("Processor Editor"):
        add_input_text(
            "Processor Editor ID", label="ID", callback=processor_editor_refresh
        )
        add_input_text(
            "Processor Editor Name", label="Name", callback=processor_editor_update_name
        )

    with window("Recipe List"):
        add_button("Recipe List Add", label="Add", callback=recipe_list_add)
        add_button("Recipe List Delete", label="Delete", callback=recipe_list_delete)
        add_table("Recipes", ["ID", "Name"], callback=recipe_list_update)

    with window("Recipe Editor"):
        add_input_text("Recipe Editor ID", label="ID", callback=recipe_editor_refresh)
        add_input_text(
            "Recipe Editor Name", label="Name", callback=recipe_editor_update_name
        )

    with window("Factory Calculator"):

        with menu_bar("Main Menu Bar"):

            with menu("File"):
                add_menu_item("Open world...", callback=open_database)
                add_menu_item("Save world as...", callback=rename_database)

            with menu("View"):
                add_menu_item("Item list...", callback=item_list_open)
                add_menu_item("Item editor...", callback=item_editor_open)
                add_menu_item("Processor list...", callback=processor_list_open)
                add_menu_item("Processor editor...", callback=processor_editor_open)
                add_menu_item("Recipe list...", callback=recipe_list_open)
                add_menu_item("Recipe editor...", callback=recipe_editor_open)
                add_menu_item("Debug logs...", callback=show_logger)

    set_theme("Light")

    # Need to always maintain at least one connection to in-memory db, lest the temporary data be dropped
    with get_conn() as inmem_conn:
        migrate()
        start_dearpygui(primary_window="Factory Calculator")


def cli():
    typer.run(main)


migration_query = """
  create table if not exists processor (
    id integer primary key,
    name text,
    energycost integer
  );

  create table if not exists recipe (
    id integer primary key,
    name text
  );

  create table if not exists item (
    id integer primary key,
    name text
  );

  create table if not exists recipe_input (
    id integer primary key,
    recipe integer,
    item integer
  );

  create table if not exists recipe_processor (
    id integer primary key,
    recipe integer,
    processor integer,
    priority integer,
    foreign key(recipe) references recipe(id) on delete cascade,
    foreign key(processor) references processor(id) on delete cascade
  );
  """
