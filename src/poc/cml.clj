;; Copyright (C) by Vesa Karvonen

(ns poc.cml
  (:require
    [clojure.core.async :refer [<! >! alts! chan go put!]]
    [clojure.core.match :refer [match]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-op [[nacks port->op] port wrappers prim]
  (if (contains? port->op port)
    (throw (Exception. "Composable choice over duplicate ports is not enabled by core.async"))
    [nacks
     (assoc port->op port [wrappers (count port->op) prim])]))

(defn- add-nack [[_ port->op-before] [nacks port->op] nack]
  (let [lo (count port->op-before)
        hi (count port->op)]
    [(conj nacks [lo hi nack]) port->op]))

(defn- get-prim [op]
  (match op [_ _ prim] prim))

(defn- inst [wrappers all xE]
  (match xE
    [:choose xEs]         (reduce (fn [all xE] (inst wrappers all xE)) all xEs)
    [:wrap yE y->x]       (inst (conj wrappers y->x) all yE)
    [:guard ->xE]         (inst wrappers all (->xE))
    [:with-nack nack->xE] (let [nack (chan)]
                            (add-nack all (inst wrappers all (nack->xE nack)) nack))
    (:or [xC _] xC)       (add-op all xC wrappers xE)))

(defn- instantiate [xE]
  (inst [] [[] {}] xE))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pute
  "Creates an event that is synchronized by giving a value on the specified
  channel.  The result of the event is true if a value was given and false if
  the channel was closed."
  ([xC x] [xC x]))

(defn gete
  "Creates an event that is synchronized by taking a value on the specified
  channel.  The result of the event is the value taken or nil if the channel was
  closed.  Note: `gete` is the identity function.  In other words, you can just
  use an ordinary channels as a get event."
  ([xC] xC))

(defn choose
  "Creates an event that is instantiated by instantiating all the given events
  and synchronized by non-deterministically synchronizing upon one of them."
  ([& xEs] [:choose xEs]))

(defn wrap
  "Creates an event that is instantiated and synchronized like the given event
  after which the result is passed through the given function."
  ([xE x->y] [:wrap xE x->y]))

(defn guard
  "Creates an event that is instantiated by invoking the given thunk that
  returns an event which is then instantiated and possibly synchronized upon."
  ([->xE] [:guard ->xE]))

(defn with-nack
  "Creates an event that is instantiated by passing a new negative
  acknowledgment event to the given function that returns an event which is then
  instantiated and possibly synchronized upon.  In case the returned event will
  not be synchronized upon, the negative acknowledgement event becomes enabled."
  ([n->xE] [:with-nack n->xE]))

(defn go-sync!
  "Starts a go block inside of which the given event is instantiated and
  synchronized upon."
  ([xE]
    (go
      (let [[nacks port->op] (instantiate xE)
            [result port] (alts! (map get-prim (vals port->op)))
            [wrappers i _] (get port->op port)]
        (doseq [[lo hi nack] nacks]
          (when (or (< i lo) (<= hi i))
            (go (loop []
                  (>! nack i)
                  (recur)))))
        (loop [result result
               wrappers wrappers]
          (if (empty? wrappers)
            result
            (recur ((peek wrappers) result) (pop wrappers))))))))

(defmacro sync!
  "Instantiates and synchronizes on the given event.  This must be used inside a
  `go` block."
  ([xE]
    `(<! (go-sync! ~xE))))
