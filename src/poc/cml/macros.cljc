;; Copyright (C) by Vesa Karvonen

(ns poc.cml.macros
  #?@(:clj  [(:require
               [clojure.core.async :as async :refer [<!]]
               [clojure.core.match :refer [match]])]
      :cljs [(:require-macros
               [cljs.core.async.macros :as async]
               [cljs.core.match :refer [match]])
             (:require
               [cljs.core.async :refer [<!]])])
  (:require
    [poc.cml :as cml]))

;; Convenience ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro go
  "Alias for core.async `go`."
  ([first & rest]
    `(async/go ~first ~@rest)))

(defmacro sync!
  "Instantiates the given events and non-deterministically synchronizes at most
  one of them.  Parks until synchronized.  This must be used inside a `go`
  block."
  ([xE & xEs] `(<! (cml/go-sync! ~xE ~@xEs))))

(defmacro wrapm
  "Experimental shorthand macro: `(wrapm <event> <case>+)`, where each `<case>`
  is of the form `<pat> <expr>`, is equivalent to `(wrap <event> (fn [x#]
  (match x# <case>+)))`."
  ([xE xP1 xB1 & rest]
    `(cml/wrap ~xE
       (fn [x#]
         (match x#
           ~xP1 ~xB1
           ~@rest)))))

(defmacro guardm
  "Experimental shorthand macro: `(guardm <body>+)` is equivalent to `(guard (fn
  [] <body>+)`."
  ([first & rest]
    `(cml/guard (fn [] ~first ~@rest))))

(defmacro with-nackm
  "Experimental shorthand macro: `(with-nackm <body>+)` is equivalent to
  `(with-nack (fn [nack] <body>+)`."
  ([first & rest]
    `(cml/with-nack (fn [~'nack] ~first ~@rest))))

(defmacro wrap-abortm
  "Experimental shorthand macro: `(wrap-abortm <event> <body>+)` is equivalent
  to `(wrap-abort <event> (fn [] <body>+)`."
  ([xE first & rest]
    `(cml/wrap-abort ~xE (fn [] ~first ~@rest))))
