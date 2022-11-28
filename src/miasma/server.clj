(ns miasma.server
  (:require [aleph.http :as aleph]
            [com.stuartsierra.component :as component]
            [miasma.handler :as h]))

(defrecord ServerImpl [server handler port]
  component/Lifecycle
  (start [this]
    (assoc this :server
                (aleph/start-server (h/handler handler) {:port port})))
  (stop [this]
    (when-let [s server]
      (.close server))
    (assoc this :server nil)))