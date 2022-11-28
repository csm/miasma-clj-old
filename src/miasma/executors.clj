(ns miasma.executors
  (:require [manifold.deferred :as d]
            [manifold.executor :as e]))

(defmacro with-executor
  [executor & body]
  `(let [d# (d/deferred)]
     (e/with-executor ~executor
       (try
         (let [res# (do ~@body)]
           (d/success! d# res#))
         (catch Exception e#
           (d/error! d# e#)))
       d#)))
