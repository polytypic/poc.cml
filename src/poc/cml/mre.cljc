;; Copyright (C) by Vesa Karvonen

(ns poc.cml.mre
  #?@(:clj  [(:require [clojure.core.async :refer [chan put!]])]
      :cljs [(:require [cljs.core.async    :refer [chan put!]])])
  (:require [poc.cml :refer [choose wrap]]))

(defn create
  ([set?]
    (let [set (chan)
          unset (chan)]
      (put! (if set? set unset) :state)
      [set unset])))

(defn signal
  ([[set unset]]
    (wrap (choose set unset)
      (fn [v] (put! set v)))))

(defn reset
  ([[set unset]]
    (wrap (choose set unset)
      (fn [v] (put! unset v)))))

(defn wait
  ([[set _]]
    (wrap set
      (fn [v] (put! set v)))))
