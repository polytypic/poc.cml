;; Copyright (C) by Vesa Karvonen

(ns poc.cml
  (:require
    [clojure.core.async :refer [>! alts! chan go put!]]
    [clojure.core.match :refer [match]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-op [all port stack op]
  (match all
    [all-nacks all-ops]
      (if (contains? all-ops port)
        (throw (Exception. "Composable choice over duplicate ports is not enabled by core.async"))
        [all-nacks
         (assoc all-ops port (conj (conj stack (count all-ops)) op))])))

(defn- add-nack [all-bef all nack]
  (match all-bef
    [_ ops-bef]
      (match all
        [all-nacks all-ops]
          [(conj all-nacks [(count ops-bef) (count all-ops) nack])
           all-ops])))

(defn- count-ops [all]
  (match all
    [_ all-ops] (count all-ops)))

(defn- inst [stack all xE]
  (match xE
    [:choose xEs]         (reduce (fn [all xE] (inst stack all xE)) all xEs)
    [:wrap yE y->x]       (inst (conj stack y->x) all yE)
    [:guard ->xE]         (inst stack all (->xE))
    [:with-nack nack->xE] (let [nack (chan)]
                            (add-nack all (inst stack all (nack->xE nack)) nack))
    (:or [xC _] xC)       (add-op all xC stack xE)))

(defn- instantiate [xE] (inst [] [[] {}] xE))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pute
  ([xC x] [xC x]))
(defn gete
  ([xC] xC))
(defn choose
  ([& xEs] [:choose xEs]))
(defn wrap
  ([xE x->y] [:wrap xE x->y]))
(defn guard
  ([->xE] [:guard ->xE]))
(defn with-nack
  ([n->xE] [:with-nack n->xE]))

(defn go-sync!
  ([xE]
    (go
      (let [[nacks ops] (instantiate xE)
            [val port] (alts! (into [] (map peek (vals ops))))
            stack (pop (get ops port))
            i (peek stack)]
        (doseq [[lo hi nack] nacks]
          (when (or (< i lo) (<= hi i))
            (go (loop []
                  (>! nack i)
                  (recur)))))
        (loop [val val
               fns (pop stack)]
          (if (empty? fns)
            val
            (recur ((peek fns) val) (pop fns))))))))
