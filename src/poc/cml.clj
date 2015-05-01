;; Copyright (C) by Vesa Karvonen

(ns poc.cml
  (:require
    [clojure.core.async :refer [<! >! alts! chan go put!]]
    [clojure.core.match :refer [match]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; There are two key differences between what is provided by core.async and
;; what CML-style events provide:
;;
;; - core.async alts cannot be nested.
;; - core.async does not provide negative acknowledgments.
;;
;; In this proof-of-concept implementation we express CML-events as trees.  See
;; the match clause in the `inst` function below to get the idea.  Instantiation
;; linearizes the tree of events into a `port->op` mapping of primitive
;; operations.  Primitive operations are of the form `[wrappers i prim]` where
;; `wrappers` are post synchronization actions, `i` is the linearized index of
;; the op and `prim` is a primitive core.async operation, namely either a put
;; `[xC x]` or a get `xC` operation.
;;
;; To implement negative acknowledgments, we keep a vector of nacks while we
;; instantiate the tree of events.  Each nack is represented as `[lo up nC]`
;; where `lo` and `up` are the lower and upper bound indices of linearized
;; primitive operations corresponding to the event and `nC` is a channel that
;; serves as the event.  After synchronizing on a primitive operation, the
;; vector of nacks is processed and all nacks whose range is outside of the
;; index of the chosen primitive operation are enabled.
;;
;; Finally, after nacks have been enabled, the wrappers are executed to produce
;; the final result.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-op [[nacks port->op] port wrappers prim]
  (if (contains? port->op port)
    (throw
      (Exception.
        "Composable choice over duplicate ports is not enabled by core.async"))
    [nacks (assoc port->op port [wrappers (count port->op) prim])]))

(defn- add-nack [[_ port->op-before] [nacks port->op] nE]
  (let [lo (count port->op-before)
        up (count port->op)]
    [(conj nacks [lo up nE]) port->op]))

(defn- op->prim [[_ _ prim]] prim)

(defn- inst [wrappers all xE]
  (match xE
    [:choose xEs]       (reduce #(inst wrappers %1 %2) all xEs)
    [:wrap yE y->x]     (inst (conj wrappers y->x) all yE)
    [:guard ->xE]       (inst wrappers all (->xE))
    [:with-nack nE->xE] (let [nE (chan)]
                          (add-nack all (inst wrappers all (nE->xE nE)) nE))
    (:or [xC _] xC)     (add-op all xC wrappers xE)))

(defn- instantiate [xE] (inst [] [[] {}] xE))

(defn- >!-forever [xC x] (go (loop [] (>! xC x) (recur))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pute
  "Creates an event that is synchronized by giving a value on the specified
  channel.  The result of the event is true if a value was given and false if
  the channel was closed.  Note: `(pute xC x) = [xC x]`."
  ([xC x] [xC x]))

(defn gete
  "Creates an event that is synchronized by taking a value on the specified
  channel.  The result of the event is the value taken or nil if the channel was
  closed.  Note: `(gete xC) = xC`."
  ([xC] xC))

(defn choose
  "Creates an event that is instantiated by instantiating all the given events
  and synchronized by non-deterministically synchronizing one of them."
  ([xE] xE)
  ([xE & xEs] [:choose (apply vector xE xEs)]))

(defn wrap
  "Creates an event that is instantiated and synchronized like the given event
  after which the result is passed through the given function."
  ([yE y->x] [:wrap yE y->x]))

(defn guard
  "Creates an event that is instantiated by invoking the given thunk that must
  return an event which is then instantiated.  The event is synchronized by
  synchronizing the event returned by the thunk."
  ([->xE] [:guard ->xE]))

(defn with-nack
  "Creates an event that is instantiated by passing a new negative
  acknowledgment event to the given function that must return an event which is
  then instantiated.  The event is synchronized by synchronizing the event
  returned by the given function.  In case the returned event will not be
  synchronized, the negative acknowledgment event becomes enabled."
  ([nE->xE] [:with-nack nE->xE]))

(defn go-sync!
  "Starts a go block inside of which the given events are instantiated and
  non-deterministically at most one of them is synchronized."
  ([xE] (go (let [[nacks port->op] (instantiate xE)
                  [result port] (alts! (map op->prim (vals port->op)))
                  [wrappers i _] (get port->op port)]
              (doseq [[lo up nE] nacks]
                (when (or (< i lo) (<= up i))
                  (>!-forever nE :nack)))
              (reduce #(%2 %1) result (rseq wrappers)))))
  ([xE & xEs] (go-sync! (apply choose xE xEs))))

(defmacro sync!
  "Instantiates the given events and non-deterministically synchronizes at most
  one of them.  This must be used inside a `go` block."
  ([xE & xEs] `(<! (go-sync! ~xE ~@xEs))))
