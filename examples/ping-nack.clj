;; Copyright (C) by Vesa Karvonen

(require '[poc.cml :refer [gete pute choose wrap guard with-nack go-sync!]])
(require '[clojure.core.async :refer [<! >! chan go put! timeout]])

(def pong-ch (chan))
(def ping-ch (chan))

(go
  (loop []
    (let [[nack ch m]
           (<! (go-sync!
                 (choose
                   (wrap (gete pong-ch) (fn [[nack ch]] [nack ch "ping "]))
                   (wrap (gete ping-ch) (fn [[nack ch]] [nack ch "pong "])))))]
      (<! (timeout 1000))
      (<! (go-sync!
            (choose
              (wrap nack (fn [_] (println "nack")))
              (wrap (pute ch m) (fn [_] (println "put" m)))))))
    (recur)))

(go
  (println
    (<! (go-sync!
          (choose
            (with-nack
              (fn [nack]
                (let [reply-ch (chan)]
                  (put! pong-ch [nack reply-ch])
                  (gete reply-ch))))
            (wrap (gete (timeout 1500))
              (fn [_] "timeout1"))))))

  (println
    (<! (go-sync!
          (choose
            (with-nack
              (fn [nack]
                (let [reply-ch (chan)]
                  (put! ping-ch [nack reply-ch])
                  (gete reply-ch))))
            (wrap (gete (timeout 500))
              (fn [_] "timeout2")))))))
