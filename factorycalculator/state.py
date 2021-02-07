"""
State module

Procides the State class, which is the core of the app's state management.

All application state is centrally stored in a single instance of the State class.
The class provides methods for querying the state, updating it, and for adding
"observers" that are informed of all state changes.

The state itself takes the form of a nested dictionary -- a tree.
The "leaves" of the tree can be any value. Using the patch() method,
a subset of the tree can be updated or deleted, or new values can be added.
"""
from uuid import uuid4

import toolz
from dearpygui.core import get_data, get_value

TOMBSTONE = f"TOMB-{uuid4()}"


def deepmerge(dicts):
    """Deeply-merges a dict according to these rules:
    - Dicts are merged with toolz.merge
    - All non-dict values are "merged" by selecting the right-most instance of the value
    - Values where the right-most instance is TOMBSTONE will have the
      corresponding key removed altogether."""
    global TOMBSTONE
    if isinstance(dicts[-1], dict):
        return toolz.valfilter(
            lambda x: x != TOMBSTONE, toolz.merge_with(deepmerge, *dicts)
        )
    else:
        return dicts[-1]


def patcher(keys):
    """Helper function for capturing state updates in callbacks.
    Given an array of keys (like that used for get_in), returns a callback function that updates
    the specified "place" with the value of the widget it's a callback for.

    Usage:
        add_input_text("MyInput", callback=patcher(["my", "state", "keys"]))"""
    return lambda s: get_data("State").assoc_in(keys, get_value(s))


class State(object):
    def __init__(self, initial_state={}):
        self.state = initial_state
        self.observers = {}

    def init(self, state_patch):
        """Like patch(), but doesn't notify observers. Used for initializing state without triggering a state change."""
        self.state = deepmerge([self.state, state_patch])

    def patch(self, state_patch):
        """Accepts a dict that represents the desired change to the state tree. The dict is "patched" over the state,
        meaning that any existing values in the state are preserved, and only the values included in the patch are overwritten.

        If you wish to delete a key from the state tree, set that key's value to the TOMBSTONE, and it will be pruned.
        Note that the TOMBSTONE value will not end up in the state (instead, the key will just be gone);
        it's only meaningful within a patch dict."""
        new_state = deepmerge([self.state, state_patch])
        [h(state_patch, new_state, self.state) for h in self.observers.values()]
        self.state = new_state

    def observe(self, obs_name, obs_fn):
        """Register an observer that will be called with (patch_dict, new_state, old_state) whenever the state is changed.
        Observers are registered with a unique name, such that if another observer registers with the same name, it will overwrite
        the previous observer."""
        self.observers[obs_name] = obs_fn

    def assoc_in(self, keys, value):
        patch = toolz.assoc_in({}, keys, value)
        self.patch(patch)

    def get_in(self, keys):
        return toolz.get_in(keys, self.state)
