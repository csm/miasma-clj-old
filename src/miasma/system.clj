(ns miasma.system
  (:require [com.stuartsierra.component :as component]
            [miasma.db :as d]
            [miasma.handler :as h]
            [miasma.server :as s]
            [manifold.executor :as e])
  (:import (java.util.concurrent ExecutorService)))

(extend-protocol component/Lifecycle
  ExecutorService
  (start [this] this)
  (stop [this]
    (.shutdown this)
    this))

(defn system
  [{:keys [db-path port domain] :or {db-path "miasma.data" port 3000 domain "localhost:3000"}}]
  (component/system-map
    :executor (e/utilization-executor 0.1)
    :db       (component/using
                (d/map->DbImpl {:path db-path})
                {:executor :executor})
    :handler  (component/using
                (h/map->HandlerImpl {:domain domain})
                {:db :db})
    :server   (component/using
                (s/map->ServerImpl {:port port})
                {:handler :handler
                 :executor :executor})))
