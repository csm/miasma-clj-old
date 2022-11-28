(ns miasma.logger
  (:require [clojure.tools.logging.impl :as l]
            [manifold.stream :as s])
  (:import (com.stuartsierra.component Lifecycle)))

(defrecord AsyncLogger [executor ns wrapped]
  l/Logger
  (enabled? [_ level] (l/enabled? wrapped level))
  (write! [_ level throwable message]))

(defrecord AsyncLoggerFactory [executor factory levels]
  l/LoggerFactory
  (name [_] "async-logger-factory")
  (get-logger [_ ns])

  Lifecycle
  (start [this]))