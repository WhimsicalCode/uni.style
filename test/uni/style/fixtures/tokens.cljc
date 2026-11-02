(ns uni.style.fixtures.tokens
  "Tokens for utility tests: def-token names are deterministic hashes of their
   namespace and symbol, so tests assert the generated names."
  (:require [uni.style.core :as s]))

(s/def-token bg-base "#fff")
(s/def-token fg-base "#333")
(s/def-token border-base "#ddd")
(s/def-token border-subtle "#eee")
(s/def-token focus-ring "#2563eb")
(s/def-token shadow-subtle "0 1px 2px rgba(0, 0, 0, 0.1)")
(s/def-token shadow-filter-subtler "drop-shadow(0 1px 1px rgba(0, 0, 0, 0.1))")
