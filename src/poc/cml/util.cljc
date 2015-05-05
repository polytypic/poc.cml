;; Copyright (C) by Vesa Karvonen

(ns poc.cml.util
  (:require
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    [poc.cml :refer [choose wrap]]))

(def chan async/chan)
(def put! async/put!)

(defn on
  ([xE y]
    (wrap xE (fn [_] y))))

 (defn transfer
   ([xE xC]
     (wrap xE
       (fn [x] (async/put! xC x)))))
