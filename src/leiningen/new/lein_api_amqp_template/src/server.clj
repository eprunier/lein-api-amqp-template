(ns {{name}}.server
    (:require [{{name}}.config :refer [config]]
              [langohr.core :as rmq]
              [langohr.channel :as lch]
              [langohr.exchange :as le]
              [langohr.queue :as lq]
              [langohr.consumers :as lc]
              [langohr.basic :as lb])
    (:gen-class))

(def ^:const exchange-name "{{name}}-exchange")
(def ^:const queue-name "{{name}}-queue")
(def ^:const routing-key "{{name}}")

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn start
  []
  (let [connection (rmq/connect)
        channel (lch/open connection)]
    (try
      (le/direct channel exchange-name :auto-delete true)
      (let [queue (-> channel
                      (lq/declare queue-name :exclusive false :auto-delete true)
                      (.getQueue))]
        (lq/bind channel queue exchange-name :routing-key routing-key)
        (lc/subscribe channel queue-name message-handler :auto-ack true))
      (println (str "Service started on channel " (.getChannelNumber channel)))
      {:connection connection
       :channel channel}
      (catch Exception e
        (rmq/close channel)
        (rmq/close connection)         
        (.printStackTrace e)))))

(defn stop
  [{channel :channel
    connection :connection
    consumer :consumer}]
  (rmq/close channel)
  (rmq/close connection)
  (println "Service stopped"))

(defn -main
  []
  (start))
