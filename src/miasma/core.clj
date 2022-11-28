(ns miasma.core
  (:require [com.stuartsierra.component :as component]
            [miasma.system :as s]
            [clojure.tools.logging :as log]))

(defn -main [& args]
  (let [system (component/start-system
                 (s/system {}))]
    (log/info "Miasma started")))