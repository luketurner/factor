(ns factor.components)

(defmacro defcomponent
  "Macro for conveniently defining Reagent components, especially wrapper components.
   Works like (defn) except the component always receives two arguments: a map of props
   and a vec of children. When used, though, props are optional and if multiple props maps are specified,
   they are merged together with higher priority going to keys further to the right. (Any parameters that
   are maps are assumed to be prop-maps.)

   Also note that unlike defn, components can only include one body-form. If you want multiple forms,
   wrap them in a (do)
   
   Define a component:

   (defcomponent h1 [p c] (into [:h1 p] c))
   
   Some valid usages:
   
   [h1 \"foobar\"]
   [h1 {:id \"foo\"} \"foobar\"]
   [h1 \"foobar\" {:id \"foo\" :title \"foo\"} {:title \"baz\"}]"
  ([name [props children] body]
   `(defn ~name
      [& props-or-children#]
      (let [{props-maps# true ~children false} (group-by map? props-or-children#)
            ~props (apply merge props-maps#)]
        ~body)))
  ([name doc [props children] body]
   `(defn ~name ~doc
      [& props-or-children#]
      (let [{ props-maps# true ~children false } (group-by map? props-or-children#)
            ~props (apply merge props-maps#)]
        ~body))))

(defmacro defwrapper
  "Macro for quickly writing components whose only purpose is to wrap an external React component
   and provide a more comfortable API.
   
   Works exactly like defcomponent, except instead of specifying params and a body, you just
   specify the component (or Hiccup keyword e.g. :h1) to wrap.
   
   Can also accept a map of props to be merged into the wrapped component's props:

   (defwrapper card-lg card {:class \"w-32\"})
   "
  ([name form]
   `(defcomponent ~name [props# children#]
      (into [~form props#] children#)))
  ([name form default-props]
   `(defcomponent ~name [props# children#]
      (into [~form (merge ~default-props props#)] children#))))