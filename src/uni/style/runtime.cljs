(ns uni.style.runtime
  "Development-time runtime support loaded by uni.style.core.")

(defonce ^:private reload-sequence- (atom 0))

(defn ^:dev/after-load reload-css! []
  ;; This namespace is also reachable from optimized Node renderer builds. Avoid
  ;; touching browser globals there while keeping the browser dependency minimal.
  (when (exists? js/document)
    (when-let [el (.querySelector js/document "#bundle-css")]
      (let [url (js/URL. (.-href el))
            ;; Date.now preserves cache busting across page loads; the sequence makes
            ;; repeated recompiles in the same millisecond distinct as well.
            version (str (js/Date.now) "-" (swap! reload-sequence- inc))]
        (.set (.-searchParams url) "v" version)
        (set! (.-href el) url)))))

(when ^boolean goog/DEBUG
  ;; Prime the stylesheet once on startup. Shadow invokes reload-css! again after
  ;; every namespace reload because of its :dev/after-load metadata.
  (defonce __init @(delay (reload-css!))))
