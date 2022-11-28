(ns miasma.util
  (:require [clojure.string :as string]))

(defn parse-webfinger-resource
  [s]
  (when-let [result (re-matches #"acct:@?([a-zA-Z0-9]+)(@[-_\.a-zA-Z0-9]+)" s)]
    {:username (second result)
     :domain   (nth result 2)}))

(defn hex
  [b]
  (string/join (map #(format "%02x" %) b)))

(defn unhex
  [s]
  (byte-array (map #(Byte/parseByte (string/join %) 16) (partition 2 s))))