;; Copyright (C) by Vesa Karvonen

(ns poc.cml.mre
  (:require
    [poc.cml :refer [choose wrap]]
    [poc.cml.util :refer [chan transfer put!]]))

(defn create
  ([set?]
    (let [set (chan)
          unset (chan)]
      (put! (if set? set unset) :state)
      [set unset])))

(defn signal
  ([[set unset]]
    (transfer (choose set unset) set)))

(defn reset
  ([[set unset]]
    (transfer (choose set unset) unset)))

(defn wait
  ([[set _]]
    (transfer set set)))
