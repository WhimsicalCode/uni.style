(ns uni.style.impl.animation
  "Animation definitions and animation-related utilities."
  (:refer-clojure :rename {not not'})
  (:require [clojure.string :as str]
            [uni.style.impl.compiler :as compiler]
            [uni.style.impl.composition :as composition]
            [uni.style.impl.value :as value]))

(def animation-option-keys
  #{:layer :duration :easing :delay :iterations :direction :fill-mode :play-state})

(def direction-values #{"normal" "reverse" "alternate" "alternate-reverse"})
(def fill-mode-values #{"none" "forwards" "backwards" "both"})
(def play-state-values #{"running" "paused"})

(defn- resolve-string-option [env options option default]
  (let [form (get options option default)
        resolved (compiler/resolve-compile-time-constant env form)
        resolved (cond
                   (and (keyword? resolved) (nil? (namespace resolved))) (name resolved)
                   (string? resolved) resolved
                   :else nil)]
    (when-not (and resolved (not' (str/blank? resolved)))
      (throw (ex-info (str "Animation " (name option) " must be a non-empty compile-time string or keyword")
                      {:form form :value resolved :type option})))
    resolved))

(defn- resolve-enum-option [env options option default allowed]
  (let [resolved (resolve-string-option env options option default)]
    (when-not (contains? allowed resolved)
      (throw (ex-info (str "Invalid animation " (name option))
                      {:form (get options option default)
                       :value resolved
                       :allowed allowed
                       :type option})))
    resolved))

(defn- resolve-iterations [env options]
  (let [form (get options :iterations 1)
        resolved (compiler/resolve-compile-time-constant env form)]
    (cond
      (and (value/finite-number? resolved) (not' (neg? resolved))) (str resolved)
      (= :infinite resolved) "infinite"
      (= "infinite" resolved) "infinite"
      :else
      (throw (ex-info "Animation iterations must be a non-negative compile-time number or infinite"
                      {:form form :value resolved :type :iterations})))))

(defn resolve-animation-options [env options]
  (let [unknown-keys (seq (remove animation-option-keys (keys options)))]
    (when unknown-keys
      (throw (ex-info "Unsupported animation options"
                      {:options options :unknown-keys unknown-keys})))
    {:layer (compiler/resolve-layer env (get options :layer :components))
     :duration (resolve-string-option env options :duration "0s")
     :easing (resolve-string-option env options :easing "ease")
     :delay (resolve-string-option env options :delay "0s")
     :iterations (resolve-iterations env options)
     :direction (resolve-enum-option env options :direction "normal" direction-values)
     :fill-mode (resolve-enum-option env options :fill-mode "none" fill-mode-values)
     :play-state (resolve-enum-option env options :play-state "running" play-state-values)}))

(defn resolve-step [env step-form]
  (let [resolved (compiler/resolve-compile-time-constant env step-form)
        steps (if (and (sequential? resolved) (not' (string? resolved)))
                resolved
                [resolved])]
    (when-not (and (seq steps)
                   (every? #(and (value/finite-number? %)
                                 (<= 0 % 100))
                           steps))
      (throw (ex-info "Animation steps must be compile-time numbers from 0 through 100"
                      {:form step-form :value resolved :type :animation-step})))
    (str/join "," (map #(str % "%") steps))))

(defn- collect-frame-declaration [env rules]
  (when-not (and (vector? rules) (seq rules))
    (throw (ex-info "Animation frame rules must be a non-empty vector of uni.style utilities"
                    {:rules rules :type :animation-frame-rules})))
  (let [collector (atom {:declarations []
                         :registrations 0
                         :layer compiler/unset-layer})]
    ;; Frame declarations are emitted inside keyframes, outside the source
    ;; class's cascade layer. Normalize their collection layer so composed
    ;; classes and ordinary utilities can be used together in one frame.
    (binding [compiler/*layer* compiler/unset-layer
              compiler/*keyframes?* true]
      (doseq [rule rules]
        (composition/collect-style-form! env collector rule)))
    (let [{:keys [declarations]} @collector]
      (when-not (seq declarations)
        (throw (ex-info "An animation frame requires at least one utility declaration"
                        {:rules rules :type :animation-frame-rules})))
      (when (some (comp seq :modifiers) declarations)
        (throw (ex-info "Animation frame utilities cannot use style modifiers"
                        {:rules rules :type :animation-frame-modifier})))
      (:declaration (first declarations)))))

(defn resolve-frames [env frames]
  (when-not (and (map? frames) (seq frames))
    (throw (ex-info "An animation requires a non-empty :frames map"
                    {:frames frames :type :animation-frames})))
  (mapv (fn [[step-form rules]]
          {:selector (resolve-step env step-form)
           :declaration (collect-frame-declaration env rules)})
        frames))

(defn animation-value [animation-name options]
  (let [{:keys [duration easing delay iterations direction fill-mode play-state]} options]
    (str/join " " [duration easing delay iterations direction fill-mode play-state
                   animation-name])))

(defn animation-css [animation-name layer frames]
  (let [css (str "@keyframes " animation-name "{"
                 (str/join ""
                           (map (fn [{:keys [selector declaration]}]
                                  (str selector "{"
                                       (compiler/declaration-css declaration)
                                       "}"))
                                frames))
                 "}")]
    (str "@layer " (:name layer) "{" css "}")))

(defn def-animation* [env sym props]
  (when-not (symbol? sym)
    (throw (ex-info "An animation definition requires a symbol"
                    {:symbol sym :type :animation-symbol})))
  (when-not (map? props)
    (throw (ex-info "An animation definition requires a map of properties"
                    {:symbol sym :props props :type :animation-properties})))
  (let [options (resolve-animation-options env (dissoc props :frames))
        frames (resolve-frames env (:frames props))
        layer (:layer options)]
    (composition/def-class*
      env sym layer
      (fn [class-name]
        (compiler/register-global!
          env (compiler/caller-ns env)
          (str "uni-style-animation-" class-name)
          (animation-css class-name layer frames))
        [{:modifiers []
          :declaration {:animation (animation-value class-name options)}}]))))

(defn animation-none* [env]
  (compiler/simple-utility* env "animation-none" {:animation :none}))
