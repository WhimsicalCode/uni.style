(ns uni.style.preflight-test
  (:require [cljs.test :refer-macros [deftest is]]
            [uni.style.preflight :as preflight]))

(preflight/import-preflight
  {:font-family "Inter"
   :font-feature-settings "'cv02'"
   :font-variation-settings "'opsz' 14"
   :mono-font-family "Berkeley Mono"
   :mono-font-feature-settings "'zero'"
   :mono-font-variation-settings "'slnt' 0"})

(deftest import-preflight-test
  (is true))
