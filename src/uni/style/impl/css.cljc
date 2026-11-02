(ns uni.style.impl.css
  "Portable CSS value and theme-rule serialization, independent of any registry.")

(def ^:const layer-priority-css "@layer theme,base,components,variants;")

(def custom-property-name-re #"--[-_a-zA-Z][-_a-zA-Z0-9]*")

(def box-shadow-properties
  "Custom properties composed, topmost first, into every layered box-shadow."
  ["--us-inset-shadow" "--us-inset-ring-shadow" "--us-ring-shadow" "--us-shadow"])

(def ^:const empty-shadow "0 0 #0000")

(def registered-properties
  "Non-inheriting custom properties behind composed utilities, with initial values."
  (into (mapv (fn [property] [property empty-shadow]) box-shadow-properties)
        [["--us-translate-x" "0"]
         ["--us-translate-y" "0"]
         ["--us-scale-x" "1"]
         ["--us-scale-y" "1"]]))

;; Matches Safari before 16.4 and Firefox before 128, which lack @property.
(def ^:private no-at-property-query
  (str "((-webkit-hyphens:none) and (not (margin-trim:inline)))"
       " or ((-moz-orient:inline) and (not (color:rgb(from red r g b))))"))

(def property-css
  "Non-inheriting registrations, so unset or ancestor-set values stay initial,
   plus a base-layer reset for browsers without @property."
  (str (apply str
              (for [[property initial-value] registered-properties]
                (str "@property " property "{syntax:\"*\";inherits:false;initial-value:"
                     initial-value ";}")))
       "@layer base{@supports " no-at-property-query "{*,::before,::after,::backdrop{"
       (apply str (for [[property initial-value] registered-properties]
                    (str property ":" initial-value ";")))
       "}}}"))

(defn value-css [form value]
  (cond
    (keyword? value)
    (let [property-name (str "--" (name value))]
      (when (namespace value)
        (throw (ex-info "CSS string token keywords must not be namespaced"
                        {:form form :value value :type :css-string})))
      (when-not (re-matches custom-property-name-re property-name)
        (throw (ex-info "Malformed CSS custom-property reference"
                        {:form form :value value :type :css-string})))
      (str "var(" property-name ")"))

    (string? value) value

    (and (number? value)
         #?(:clj (Double/isFinite (double value))
            :cljs (js/Number.isFinite value)))
    (str value)

    :else
    (throw (ex-info "CSS string components must be compile-time strings, token keywords, finite numbers, or alpha colors"
                    {:form form :value value :type :css-string}))))

(defn token-css
  "Render selected definition entries for a theme, or root defaults for nil.
   Entries are already validated by the caller. No registry or dependency traversal."
  [theme entries]
  (if (seq entries)
    (str "@layer theme{"
         (if theme (str ":root." theme ",." theme) ":root")
         "{"
         (apply str
                (for [{token :name :keys [default values]} (sort-by (comp name :name) entries)
                      :let [value (get values theme default)]]
                  (str "--" (name token) ":" (value-css value value) ";")))
         "}}")
    ""))
