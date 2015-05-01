;; Copyright (C) by Vesa Karvonen

(require '[poc.cml :refer [gete pute choose wrap guard with-nack go-sync! sync!]])
(require '[clojure.core.async :refer [<! <!! chan go put! timeout]])

;; This is a very simple toy example that uses most of the features of CML-style
;; composable events.  The idea is that we create a `ping` event that basically
;; works like a timeout of 1000ms.  Internally the event uses an asynchronous
;; server that takes ping requests.  It then waits for 1000ms or until the ping
;; request's negative acknowledgment (nack) becomes available.  If 1000ms passes
;; without negative acknowledgment, the ping server attempts to reply to the
;; request or cancels in case the negative acknowledgment becomes available.

;; Obviously this is just a toy example, but this demonstrates the idea that
;; a non-trivial asynchronous client-server protocol can be encapsulated as a
;; composable event (in this case the `ping` event).  Plain core.async `alt!`
;; does not support such composition.

(def ping
  (let [request-ch (chan)] ;; The internal ping request channel.
    (go
      ;; The internal asynchronous ping server loop.
      (loop []
        (let [;; Get a ping request.
              [nack reply-ch]
                (<! request-ch)
              ;; Wait for 1000ms or nack.
              already-nack
                (sync!
                  (choose
                    nack
                    (wrap (timeout 1000) (fn [_] false))))]
          ;; Then try to reply or cancel via nack.
          (sync!
            (choose
              (wrap nack (fn [_] (println "nack") true))
              (wrap [reply-ch "pong"] (fn [_] (println "put"))))))
        (recur)))

    ;; The client side event definition.
    (with-nack
      (fn [nack]
        (let [reply-ch (chan)]
          ;; The nack and the reply channel are sent to the server.
          (put! request-ch [nack reply-ch])
          ;; Then we return an event that synchronizes on the reply.
          reply-ch)))))

;; First we try to get a ping within 500ms.
(println
  (<!! (go-sync!
         (choose
           ping
           (wrap (timeout 500) (fn [_] "timeout 500"))))))

;; Then we try to get a ping within 1500ms.
(println
  (<!! (go-sync!
         (choose
           ping
           (wrap (timeout 1500) (fn [_] "timeout 1500"))))))
