from uuid import uuid4

from dearpygui.core import *
from dearpygui.simple import *

from . import INIT


def create_and_open_factory():
  factoryid = str(uuid4())
  get_data("State").patch({
    "world": {
      "factories": {
        factoryid: {
          "goals": {},
          "name": "Unnamed Factory"
        }
      }
    }
  })
  show_factory(factoryid)


def show_factory(factoryid):
  factory = get_data("State").get_in(["world", "factories", factoryid])
  if not factory:
    return
  if does_item_exist(f"Factory-{factoryid}"):
    if not is_item_shown(f"Factory-{factoryid}"):
      show_item(f"Factory-{factoryid}")
  else:
    with window(f"Factory-{factoryid}", label=factory["name"]):
      with tab_bar(f"Factory-{factoryid}-Tabbar"):
        with tab(f"Factory-{factoryid}-Statistics", label="Statistics"):
          pass
        with tab(f"Factory-{factoryid}-ProductionLine", label="Production Line"):
          pass


def _observer(patch, new, old):
  pass


def _init():
    state = get_data("State")
    state.observe("factory", _observer)

INIT.append(_init)
