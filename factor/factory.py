from uuid import uuid4

from dearpygui.core import *
from dearpygui.simple import *
from toolz import get_in

from . import INIT
from .state import TOMBSTONE, patcher


def create_and_show_factory():
  factory_id = str(uuid4())
  get_data("State").patch({
    "world": {
      "factories": {
        factory_id: {
          "goals": {},
          "name": "Unnamed Factory"
        }
      }
    }
  })
  log_debug(f"Created factory: {factory_id}")
  show_factory(factory_id)


def _factory_observer(factory_id):
  def observer(patch, new, old):
    if get_in(["world", "factories", factory_id], patch) == TOMBSTONE:
      delete_item(f"Factory-{factory_id}")
      get_data("State").unobserve(f"factory-{factory_id}")
    elif "world" in patch: # note -- any world changes may alter factory calculations
      delete_item(f"Factory-{factory_id}")
      add_factory_window(factory_id)
  return observer


def _add_goal(factory_id):
  def callback():
    state = get_data("State")
    goal = state.get_in(["ui", "factory", factory_id, "creategoal"])
    return state.patch({"world": {"factories": {factory_id: {"goals": {"output": {goal["item"]: int(goal["num"])} }}}}})
  return callback


def add_factory_window(factory_id):
  state = get_data("State")
  factory = state.get_in(["world", "factories", factory_id])
  factory_name = factory.get("name")
  state.observe(f"factory-{factory_id}", _factory_observer(factory_id))
  with window(f"Factory-{factory_id}", label=f"{factory_name} ({factory_id})"):
    with tab_bar(f"Factory-{factory_id}-Tabbar"):
      with tab(f"Factory-{factory_id}-Statistics", label="Statistics"):
        with collapsing_header(f"Factory-{factory_id}-Statistics-Goals", label="Factory Goals"):
          all_items = [item["name"] + f" ({item_id})" for item_id, item in state.get_in(["world", "items"]).items()]
          add_button("Factory-{factory_id}-Statistics-Goals-Add", label="+", callback=_add_goal(factory_id))
          add_input_int(f"Factory-{factory_id}-Statistics-Goals-Create-Num", default_value=1, callback=patcher(["ui", "factory", factory_id, "creategoal", "num"]))
          add_combo(f"Factory-{factory_id}-Statistics-Goals-Create-Combo", items=all_items, callback=patcher(["ui", "factory", factory_id, "creategoal", "item"]))

          for item_id, value in get_in(["goals", "output"], factory, default={}).items():
            item_name = state.get_in(["world", "items", item_id, "name"])
            add_input_int(f"Factory-{factory_id}-Statistics-Goals-Output-{item_id}-Num", default_value=value)
            add_same_line()
            add_combo(f"Factory-{factory_id}-Statistics-Goals-Output-{item_id}-Combo", default_value=item_name + f" ({item_id})")
            add_same_line()
            add_text(f"Factory-{factory_id}-Statistics-Goals-Output-{item_id}-Units", default_value="per second")

      with tab(f"Factory-{factory_id}-ProductionLine", label="Production Line"):
        pass


def show_factory(factory_id):
  if does_item_exist(f"Factory-{factory_id}"):
    if not is_item_shown(f"Factory-{factory_id}"):
      show_item(f"Factory-{factory_id}")
  else:
    add_factory_window(factory_id)

def _init():
  pass

INIT.append(_init)
