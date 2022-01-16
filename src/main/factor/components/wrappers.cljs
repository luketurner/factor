(ns factor.components.wrappers
  (:require [factor.components.macros :refer-macros [defwrapper]]
            [factor.util :refer [cl]]
            ["@blueprintjs/core" :as b]
            ["@blueprintjs/select" :as bs]
            ["ag-grid-react" :refer [AgGridReact]]))

;; WRAPPER COMPONENTS
;; These components are simple wrappers around HTML/Blueprint elements.

(defwrapper alert            (cl b/Alert))
(defwrapper anchor-button    (cl b/AnchorButton))
(defwrapper breadcrumbs      (cl b/Breadcrumbs))
(defwrapper breadcrumb       (cl b/Breadcrumb))
(defwrapper button           (cl b/Button))
(defwrapper callout          (cl b/Callout))
(defwrapper card             (cl b/Card))
(defwrapper control-group    (cl b/ControlGroup))
(defwrapper divider          (cl b/Divider))
(defwrapper form-group       (cl b/FormGroup))
(defwrapper hotkeys-provider (cl b/HotkeysProvider))
(defwrapper hotkeys-target   (cl b/HotkeysTarget2))
(defwrapper icon             (cl b/Icon))
(defwrapper input-group      (cl b/InputGroup))
(defwrapper menu             (cl b/Menu))
(defwrapper menu-divider     (cl b/MenuDivider))
(defwrapper menu-item        (cl b/MenuItem))
(defwrapper navbar           (cl b/Navbar))
(defwrapper navbar-divider   (cl b/Navbar.Divider))
(defwrapper navbar-group     (cl b/Navbar.Group))
(defwrapper navbar-heading   (cl b/Navbar.Heading))
(defwrapper non-ideal-state  (cl b/NonIdealState))
(defwrapper textarea         (cl b/TextArea))
(defwrapper tree             (cl b/Tree))
(defwrapper tree-node        (cl b/TreeNode))

; these wrappers are further encapsulated by other components
; and usually shouldn't be used directly
(defwrapper numeric-input (cl b/NumericInput))
(defwrapper ag-grid          (cl AgGridReact))
(defwrapper popover       (cl b/Popover))
(defwrapper select        (cl bs/Select))
(defwrapper omnibar       (cl bs/Omnibar))

;; SEMI-WRAPPERS
;; These are wrappers that adjust the default props without adding their own rendering/logic/etc.

(defwrapper card-lg            card         {:class-name "w-20 m-1"})
(defwrapper card-md            card         {:class-name "w-16 m-1"})
(defwrapper card-sm            card         {:class-name "w-12 m-1"})
(defwrapper navbar-group-left  navbar-group {:align :left})
(defwrapper navbar-group-right navbar-group {:align :right})