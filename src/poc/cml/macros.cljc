;; Copyright (C) by Vesa Karvonen

(ns poc.cml.macros
  #?@(:clj  [(:require
               [clojure.core.async :refer [<!]]
               [poc.cml :refer [go-sync!]])]
      :cljs [(:require
               [cljs.core.async :refer [<!]]
               [poc.cml :refer [go-sync!]])]))

;; Convenience ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro sync!
  "Instantiates the given events and non-deterministically synchronizes at most
  one of them.  Parks until synchronized.  This must be used inside a `go`
  block."
  ([xE & xEs] `(<! (go-sync! ~xE ~@xEs))))
