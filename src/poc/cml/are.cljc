;; Copyright (C) by Vesa Karvonen

(ns poc.cml.are
  (:require
    [poc.cml :refer [choose wrap]]
    [poc.cml.util :refer [chan put! transfer]]))

(defn create
  ([set?]
    (let [set (chan)
          unset (chan)]
      (put! (if set? set unset) :state)
      [set unset])))

(defn signal
  ([[set unset]]
    (transfer (choose set unset) set)))

(defn wait
  ([[set unset]]
    (transfer set unset)))
