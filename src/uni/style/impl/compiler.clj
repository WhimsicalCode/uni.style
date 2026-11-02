(ns uni.style.impl.compiler
  "Shared compiler context, constants, naming, and style registration."
  (:require [clojure.string :as str]
            [uni.style.impl.constant :as constant]
            [uni.style.impl.host :as host]
            [uni.style.impl.naming :as naming]
            [uni.style.impl.registry :as registry]
            [uni.style.impl.resolution :as resolution]
            [uni.style.impl.serialize :as serialize]
            [uni.style.impl.value :as value]))

(def ^:dynamic *variants* [])
(def ^:dynamic *variant-transforms* [])
(def ^:dynamic *layer* nil)
(def ^:dynamic *class-declaration-collector* nil)
(def ^:dynamic *selector-declaration-collector?* false)
;; True while collecting animation frames, where composed custom-property
;; utilities emit direct values so keyframes interpolate without @property.
(def ^:dynamic *keyframes?* false)

(def unset-layer :uni.style.core/unset-layer)

(def resolve-compile-time-constant constant/resolve-compile-time-constant)
(def content-hash naming/content-hash)
(def register-atomic! registry/register-atomic!)
(def register-composed! registry/register-composed!)
(def register-selector! registry/register-selector!)
(def register-global! registry/register-global!)
(def declaration-css serialize/declaration-css)

(defn caller-ns [env]
  (or (get-in env [:ns :name])
      (ns-name *ns*)))

(defn variant-class-name [class-name]
  (let [class-name (if (seq *variants*)
                     (str (str/join ":" (map :prefix *variants*)) ":" class-name)
                     class-name)]
    (if *layer*
      (str (:name *layer*) ":" class-name)
      class-name)))

(defn variant-selectors []
  (mapv :selector *variants*))

(defn- build-state []
  (some-> (host/cljs-compiler) deref :shadow.build.cljs-bridge/state))

(defn- release-build? [env]
  (let [build-state (build-state)]
    (or (= :release (:shadow.build/mode env))
        (= :release (:shadow.build/mode build-state))
        (= :release (:mode build-state))
        (and (host/cljs-compiler) (naming/release-build?)))))

(defn- short-class-names?
  "Whether generated class names are hashed us-<hash> names. In ClojureScript:
   the `:short-class-names` CSS option, or else whether this is a release build.
   On the JVM: the uni.style.short-class-names system property, true by default,
   since JVM macro expansion has no build mode."
  [env]
  (if (host/jvm? env)
    (not (false? (naming/jvm-option :short-class-names)))
    (let [option (naming/short-class-names-option)]
      (if (some? option)
        option
        (release-build? env)))))

(defn qualified-component-classes?
  "Whether development component class names should include their namespace.
   In ClojureScript, configured as `:qualified-component-classes` under
   `:css-options`: Shadow's build config, or the compiler options of
   ClojureScript ES module builds. On the JVM, by the
   uni.style.qualified-component-classes system property."
  [env]
  (true? (if (host/jvm? env)
           (naming/jvm-option :qualified-component-classes)
           (naming/css-option :qualified-component-classes))))

(defn composed-class-definition [env sym class-name stable-dev-name? preserve-dev-dashes?]
  (naming/composed-class-definition (short-class-names? env) sym class-name
                                    stable-dev-name? preserve-dev-dashes?))

(defn utility-class-name [env class-name]
  (let [class-name (variant-class-name class-name)]
    (if (short-class-names? env)
      (naming/hashed-class-name [:uni.style/utility class-name])
      class-name)))

(defn- merge-declaration [declarations modifiers declaration]
  (let [existing (some #(when (= modifiers (:modifiers %)) %) declarations)]
    (conj (into [] (remove #(= modifiers (:modifiers %))) declarations)
          {:modifiers modifiers
           :declaration (merge (:declaration existing) declaration)})))

(defn collect-declaration! [collector declaration modifiers layer]
  (swap! collector
         (fn [{existing-layer :layer :as collector}]
           (when (and (not= existing-layer unset-layer)
                      (not= existing-layer layer))
             (throw (ex-info "A class definition cannot mix cascade layers"
                             {:existing-layer existing-layer :layer layer})))
           (-> collector
               (assoc :layer layer)
               (update :declarations merge-declaration modifiers declaration)
               (update :registrations inc)))))

(defn style-ir? [value]
  (resolution/style-ir? value))

(defn emit-style-value [value]
  (if (style-ir? value)
    (resolution/emit-style-ir value)
    value))

(defn combine-style-values [env values]
  (if (every? string? values)
    (str/join " " values)
    (->> `(uni.style.core/c ~@values)
         (resolve-compile-time-constant env)
         emit-style-value)))

(defn str-values* [env component-forms]
  (when-not (seq component-forms)
    (throw (ex-info "A CSS string requires at least one component"
                    {:forms component-forms :type :css-string})))
  (->> component-forms
       (map #(value/str-component % (resolve-compile-time-constant env %)))
       (apply str)))

(defn- numeric-result* [value-forms values label type operation]
  (when-not (every? value/finite-number? values)
    (throw (ex-info (str label " requires finite compile-time numbers")
                    {:forms value-forms :values values :type type})))
  (let [result (operation values)]
    (when-not (value/finite-number? result)
      (throw (ex-info (str label " must produce a finite number")
                      {:forms value-forms :values values :value result :type type})))
    result))

(defn- numeric-operation* [env value-forms label type operation]
  (numeric-result* value-forms
                   (mapv #(resolve-compile-time-constant env %) value-forms)
                   label type operation))

(defn inc-value* [env value-form]
  (numeric-operation* env [value-form] "Increment" :increment
                      #(inc (first %))))

(defn dec-value* [env value-form]
  (numeric-operation* env [value-form] "Decrement" :decrement
                      #(dec (first %))))

(defn add-values* [env value-forms]
  (numeric-operation* env value-forms "Addition" :addition
                      #(apply + %)))

(defn subtract-values* [env value-forms]
  (when-not (seq value-forms)
    (throw (ex-info "Subtraction requires at least one compile-time number"
                    {:forms value-forms :type :subtraction})))
  (numeric-operation* env value-forms "Subtraction" :subtraction
                      #(apply - %)))

(defn multiply-values* [env value-forms]
  (numeric-operation* env value-forms "Multiplication" :multiplication
                      #(apply * %)))

(defn multiply-resolved-values* [value-forms values]
  (numeric-result* value-forms values "Multiplication" :multiplication
                   #(apply * %)))

(defn divide-values* [env value-forms]
  (when-not (seq value-forms)
    (throw (ex-info "Division requires at least one compile-time number"
                    {:forms value-forms :type :division})))
  (try
    (numeric-operation* env value-forms "Division" :division
                        #(let [result (apply / %)]
                           (if (ratio? result) (double result) result)))
    (catch ArithmeticException e
      (throw (ex-info "Division must produce a finite number"
                      {:forms value-forms :type :division}
                      e)))))

(defn- atomic-class-suffix [property]
  (-> (name property)
      (str/replace #"[^a-zA-Z0-9_-]" "-")))

(defn register-utility! [env class-name declaration]
  (let [modifiers (variant-selectors)
        atomic-declarations (vec (resolution/expand-declaration declaration))
        split? (< 1 (count atomic-declarations))
        classes (mapv (fn [[property _]]
                        (utility-class-name
                          env
                          (cond-> class-name
                                  split? (str "--" (atomic-class-suffix property)))))
                      atomic-declarations)]
    (when (and (not *selector-declaration-collector?*)
               (some (comp resolution/important? second) atomic-declarations))
      (throw (ex-info "Metadata-aware uni.style utilities do not support !important"
                      {:class-name class-name :declaration declaration :type :important})))
    (if *class-declaration-collector*
      (collect-declaration! *class-declaration-collector* declaration modifiers *layer*)
      (doseq [[[property value] atomic-class-name]
              (map vector atomic-declarations classes)]
        (binding [registry/*atomic-meta*
                  {:condition-rank (resolution/condition-rank modifiers)
                   :property-rank (resolution/property-rank property)
                   :property property}]
          (if *layer*
            (register-atomic! env (caller-ns env) atomic-class-name
                              {property value} modifiers *layer*)
            (register-atomic! env (caller-ns env) atomic-class-name
                              {property value} modifiers)))))
    (let [condition (resolution/resolution-condition modifiers *layer*)
          operations (vec
                       (mapcat (fn [[[property _] atomic-class-name]]
                                 (resolution/descriptor-operation condition property
                                                                  atomic-class-name))
                               (map vector atomic-declarations classes)))
          class-value (str/join " " classes)]
      (if (or (host/jvm? env) (contains? env :context))
        (resolution/emit-style-ir
          (resolution/style-ir class-value
                               {:operations operations :classes classes}))
        class-value))))

(def predefined-layers
  {:base {:name "base" :order nil}
   :components {:name "components" :order nil}
   :variants {:name "variants" :order nil}})

(defn resolve-layer [env layer-form]
  (let [layer-name (resolve-compile-time-constant env layer-form)]
    (or (get predefined-layers layer-name)
        (throw (ex-info "Layer must be one of :base, :components, or :variants"
                        {:form layer-form :value layer-name :type :layer})))))

(defn simple-utility* [env class-name declaration]
  (register-utility! env class-name declaration))

(defn raw* [env declaration-form]
  (let [declaration (resolve-compile-time-constant env declaration-form)]
    (when-not (serialize/valid-declaration? declaration)
      (throw (ex-info "Raw styles require a non-empty flat compile-time declaration map of static values"
                      {:form declaration-form :value declaration :type :raw-declaration})))
    (when (and (not *selector-declaration-collector?*)
               (some (fn [[property _]] (resolution/unknown-shorthand? property)) declaration))
      (throw (ex-info "Raw styles cannot use a shorthand without conflict metadata; use longhand declarations"
                      {:form declaration-form :value declaration :type :raw-shorthand})))
    (simple-utility* env (str "raw-" (content-hash declaration)) declaration)))

(defn length-utility* [env utility-prefix property value-form label type]
  (let [resolved (resolve-compile-time-constant env value-form)
        [class-part css-value] (value/length-parts value-form resolved label type true)]
    (register-utility! env (str utility-prefix "-" class-part) {property css-value})))

(defn signed-length-utility* [env utility-prefix property value-form label type]
  (let [resolved (resolve-compile-time-constant env value-form)
        [class-part css-value] (value/length-parts value-form resolved label type false)]
    (register-utility! env (str utility-prefix "-" class-part) {property css-value})))

(defn integer-utility* [env utility-prefix property value-form label type]
  (let [resolved (resolve-compile-time-constant env value-form)
        [class-part css-value] (value/integer-literal-parts value-form resolved label type)]
    (register-utility! env (str utility-prefix "-" class-part) {property css-value})))

(defn css-var-utility* [env utility-prefix property value-form label type]
  (let [resolved (resolve-compile-time-constant env value-form)
        property-name (value/css-var-property-name value-form resolved label type)]
    (register-utility! env (str utility-prefix property-name)
                       {property (str "var(" property-name ")")})))

(defn dimension-utility* [env utility-prefix properties value-form]
  (let [resolved (resolve-compile-time-constant env value-form)
        [class-part css-value]
        (value/non-negative-literal-parts value-form resolved "Dimension" :dimension)
        declaration (into (array-map)
                          (map (fn [property] [property css-value]))
                          properties)]
    (register-utility! env (str utility-prefix "-" class-part) declaration)))
