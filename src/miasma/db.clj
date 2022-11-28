(ns miasma.db
  (:require [com.stuartsierra.component :as component]
            [miasma.executors :as ex]
            [taoensso.nippy :as nippy]
            [clojure.tools.logging :as log]
            [miasma.util :refer [hex]])
  (:import [org.rocksdb RocksDB ColumnFamilyHandle ColumnFamilyDescriptor DBOptions]
           (java.nio.charset StandardCharsets)
           (java.util LinkedList)))

(defprotocol Db
  (put [this cf k v])
  (get [this cf k])
  (delete [this cf k])
  (-get-column-family [this cf]))

(def default-column-family-descriptor (ColumnFamilyDescriptor. RocksDB/DEFAULT_COLUMN_FAMILY))
(def users-column-family-descriptor (ColumnFamilyDescriptor. (.getBytes "users" StandardCharsets/UTF_8)))

(defrecord DbImpl [path db writer reader cfs executor]
  component/Lifecycle
  (start [this]
    (let [cfs (LinkedList.)
          db (RocksDB/open (DBOptions.) path
                           [default-column-family-descriptor users-column-family-descriptor]
                           cfs)
          cfs (reduce #(assoc %1 (keyword (String. (.getName ^ColumnFamilyHandle %2))) %2)
                      {} cfs)]
      (log/info "cfs:" cfs)
      (assoc this :db db
                  :cfs cfs)))

  (stop [this]
    (when-let [d db]
      (.close d))
    (assoc this :db nil))

  Db
  (put [this cf k v]
    (ex/with-executor executor
      (.put db (-get-column-family this cf)
            (nippy/freeze k)
            (nippy/freeze v))))

  (get [this cf k]
    (ex/with-executor executor
      (when-let [v (.get db (-get-column-family this cf) (nippy/freeze k))]
        (nippy/thaw v))))

  (delete [this cf k]
    (ex/with-executor executor
      (.delete db (-get-column-family this cf) (nippy/freeze k))))

  (-get-column-family [this cf]
    (cond (keyword? cf)
          (clojure.core/get cfs cf)
          (string? cf) (-get-column-family this (keyword cf))
          (instance? ColumnFamilyHandle cf) cf)))