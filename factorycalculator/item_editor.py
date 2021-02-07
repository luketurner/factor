from dearpygui.core import *
from dearpygui.simple import *
from toolz import get_in

from uuid import uuid4

from .state import TOMBSTONE, patcher
from . import INIT


def show_item_editor():
    show_item("ItemEditor")


def _init():
    state = get_data("State")
    state.observe("item_editor", _observer)
    state.init({"ui": {"item_editor": {"selected": {}, "form": {}}}})
    with window("ItemEditor"):
        with child("ItemEditorList"):
            add_text("ItemEditorListEmptyText", default_value="No items!")
        with group("ItemEditorForm"):
            add_button(
                "ItemEditorFormUpdate",
                label="Update Selected",
                callback=_update,
            )
            add_same_line()
            add_button("ItemEditorFormCreate", label="Create", callback=_create)
            add_same_line()
            add_button("ItemEditorFormDelete", label="Delete", callback=_delete)
            add_input_text(
                "ItemEditorFormName",
                label="Name",
                callback=patcher(["ui", "item_editor", "form", "name"]),
            )


def _observer(patch, new, old):

    selection_patch = get_in(["ui", "item_editor", "selected"], patch)
    if selection_patch:
        for itemid, value in selection_patch.items():
            set_value(f"ItemEditorList-{itemid}-Checkbox", value != TOMBSTONE)

    item_patch = get_in(["world", "items"], patch)
    if item_patch:
        selected_items = get_in(["ui", "item_editor", "selected"], new)
        for itemid, item in item_patch.items():
            if item == TOMBSTONE:
                delete_item(f"ItemEditorList-{itemid}")
            elif itemid in get_in(["world", "items"], old):
                set_value(f"ItemEditorList-{itemid}-Checkbox", itemid in selected_items)
                set_item_label(f"ItemEditorList-{itemid}-Checkbox", label=item["name"])
            else:
                with group(f"ItemEditorList-{itemid}", parent="ItemEditorList"):
                    add_checkbox(
                        f"ItemEditorList-{itemid}-Checkbox",
                        label=item["name"],
                        callback=_select,
                        callback_data=itemid,
                        default_value=itemid in selected_items,
                    )


def _create():
    state = get_data("State")
    formdata = state.get_in(["ui", "item_editor", "form"])
    state.patch({"world": {"items": {str(uuid4()): formdata}}})


def _update():
    state = get_data("State")
    formdata = state.get_in(["ui", "item_editor", "form"])
    state.patch(
        {
            "world": {
                "items": {
                    x: formdata for x in state.get_in(["ui", "item_editor", "selected"])
                }
            }
        }
    )


def _select(sender, data):
    get_data("State").assoc_in(
        ["ui", "item_editor", "selected", data], get_value(sender) or TOMBSTONE
    )


def _delete(sender, data):
    state = get_data("State")
    tombstones = {
        rowid: TOMBSTONE for rowid in state.get_in(["ui", "item_editor", "selected"])
    }
    state.patch(
        {
            "world": {"items": tombstones},
            "ui": {"item_editor": {"selected": tombstones}},
        }
    )


INIT.append(_init)
