(ns spariev.logsearch.db
  (:use somnium.congomongo
	[clojure.contrib.str-utils2 :as strutils :only [join]])
  (:import (org.apache.lucene.document CompressionTools)))

(def path-sep
      (java.io.File/separator))

(defn pack-into-file [fs lines]
  (insert-file! fs (CompressionTools/compressString (strutils/join "\n" lines))))

(defn unpack-from-file [fs file-id]
  (let [a-file (fetch-one-file fs :where { :_id (com.mongodb.ObjectId. file-id) })
	byte-stream (java.io.ByteArrayOutputStream. (:length a-file))]
    (do
      (write-file-to fs a-file byte-stream)
      (CompressionTools/decompressString (.toByteArray byte-stream)))))

(defn fetch-by-id
  [coll obj-id]
  (fetch-one coll :where { :_id (com.mongodb.ObjectId. obj-id)}))