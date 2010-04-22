(ns spariev.logsearch.db
  (:use [clojure.contrib.str-utils2 :as strutils :only [join]]
	[cupboard.utils])
  (:require [cupboard.core :as cb])
  (:import (org.apache.lucene.document CompressionTools)
	   (com.sleepycat.je DatabaseEntry)))

(def path-sep
      (java.io.File/separator))

(cb/defpersist log-entry
  ((:id :index :unique)
   (:body)))


(defn save-log-entry [cb-db lines]
  (let [entry-id (java.util.UUID/randomUUID)]
    (cb/make-instance
     log-entry
     [entry-id
      (DatabaseEntry.
       (CompressionTools/compressString (strutils/join "\n" lines)))]
     :cupboard cb-db)
    entry-id))

(defn load-log-entry [cb-db entry-id]
  (let [log-entry (cb/query (= :id entry-id) :cupboard cb-db)]
    (CompressionTools/decompressString (:body log-entry))))
