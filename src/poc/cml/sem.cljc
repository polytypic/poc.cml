;; Copyright (C) by Vesa Karvonen

(ns poc.cml.sem
  (#?(:clj :require :cljs :require-macros)
    [poc.cml.macros :refer [go sync!]])
  (:require
    [poc.cml :refer [choose wrap]]
    [poc.cml.util :refer [chan on put!]]))

(defn create
  ([n]
    (let [inc (chan)
          dec (chan)]
      (go (loop [n n]
            (recur (sync! (if (< 0 n)
                            (choose (on inc (+ n 1)) (on dec (- n 1)))
                            (on inc (+ n 1)))))))
      [inc dec])))

(defn release
  ([[inc _]] [inc :ignored]))

(defn wait
  ([_ dec] [dec :ignored]))
