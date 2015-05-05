;; Copyright (C) by Vesa Karvonen

(ns poc.cml.sem
  #?@(:clj  [(:require
               [clojure.core.async :refer [chan go put!]]
               [poc.cml.macros :refer [sync!]])]
      :cljs [(:require-macros
               [cljs.core.macros :refer [go]]
               [poc.cml.macros :refer [sync!]])
             (:require
               [cljs.core.async :refer [chan put!]])])
  (:require [poc.cml :refer [choose wrap]]))

(defn- on [xE y]
  (wrap xE (fn [_] y)))

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
