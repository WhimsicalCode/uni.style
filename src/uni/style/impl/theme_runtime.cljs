(ns uni.style.impl.theme-runtime
  "DOM theme application, separate from token data and development CSS reload."
  (:require [clojure.string :as str]
            [uni.style.impl.tokens :as tokens]))

(defn controller
  "Create a theme controller. Explicit host arguments keep lifecycle tests DOM-independent."
  [root match-media]
  (let [unsubscribe- (atom nil)
        stop! (fn []
                (when-let [unsubscribe @unsubscribe-]
                  (unsubscribe)
                  (reset! unsubscribe- nil)))
        validate! (fn [theme]
                    (when-not (or (nil? theme) (contains? (tokens/themes) theme))
                      (throw (ex-info "Unknown theme" {:theme theme}))))
        apply! (fn [theme]
                 (when root
                   (let [classes (.-classList root)]
                     ;; The reserved prefix also cleans server-applied and renamed
                     ;; theme classes not encountered by this controller yet.
                     (doseq [class (vec (array-seq (js/Array.from classes)))]
                       (when (str/starts-with? class "us-theme-")
                         (.remove classes class)))
                     (when theme (.add classes theme)))))]
    {:set-theme! (fn [theme]
                   (validate! theme)
                   (stop!)
                   (apply! theme))
     :set-theme-system! (fn [light dark]
                          (validate! light)
                          (validate! dark)
                          (stop!)
                          (if (and root match-media (or light dark))
                            (let [query (match-media "(prefers-color-scheme: dark)")
                                  listener (fn [event] (apply! (if (.-matches event) dark light)))]
                              (listener query)
                              (.addEventListener query "change" listener)
                              (reset! unsubscribe- #(.removeEventListener query "change" listener)))
                            (apply! light)))}))

(defonce ^:private controller- (atom nil))

(defn- browser-controller []
  (when (exists? js/document)
    (or @controller-
        (let [c (controller (.-documentElement js/document)
                            (when (and (exists? js/window) (.-matchMedia js/window))
                              #(.matchMedia js/window %)))]
          (reset! controller- c)
          c))))

(defn set-theme! [theme]
  (when-let [c (browser-controller)]
    ((:set-theme! c) theme)))

(defn set-theme-system! [light dark]
  (when-let [c (browser-controller)]
    ((:set-theme-system! c) light dark)))
