(ns spariev.logsearch.fileparser
 (:use [clojure.contrib.duck-streams :only [reader]]
       [clojure.contrib.str-utils2 :as strutils :only [join]]
       [spariev.chrono :as chrono]
       spariev.config
       spariev.logsearch.lucene
       spariev.logsearch.parser
       somnium.congomongo
       [clojure.contrib.seq-utils :only [partition-by]])
 (:import (org.apache.lucene.document CompressionTools)))

(defn pack-into-file [fs lines]
  (insert-file! fs (CompressionTools/compressString (strutils/join "\n" lines))))

(defn unpack-from-file [fs file-id]
  (let [a-file (fetch-one-file fs :where { :_id (com.mongodb.ObjectId. file-id) })
	byte-stream (java.io.ByteArrayOutputStream. (:length a-file))]
    (do
      (write-file-to fs a-file byte-stream)
      (CompressionTools/decompressString (.toByteArray byte-stream)))))

(defn parse-and-save-req
  [req-lines]
  (let [gridfs-file (pack-into-file :logsfs (rest req-lines))]    
    (insert! :railslogs
	     {:hdr (first req-lines)
	      :parsed-hdr (parse-line-regexp processing-rule-regexp (first req-lines))
	      :file_id (str(gridfs-file :_id))})))

(defn process-request-log [index-writer log-chunk]
  (do
    #_(println (first log-chunk))
    (let [mongo-log (parse-and-save-req log-chunk)
	  lucene-doc (parse-log-chunk (.toString (mongo-log :_id)) log-chunk)]
      (.addDocument index-writer lucene-doc)
      nil)))


(defn parse-file-reader [file-reader]
  (let [req-counter (ref 0)
        idx-dir (index-dir *index-filename*)]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db *db-name*)
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(do
	  (doall (map (partial process-request-log writer)
		      (partition-by
		       #(if (. % startsWith "Processing")
			  (dosync (alter req-counter inc))
			  (deref req-counter))
		       (line-seq file-reader))))
	  '())))))

(defn parse-file-with-writer [index-writer fname]
  (let [req-counter (ref 0)]
    (do
      (println (str "parsing " fname))
      (doall (map (partial process-request-log index-writer)
		(partition-by
		 #(if (. % startsWith "Processing")
		    (dosync (alter req-counter inc))
		    (deref req-counter))
		 (line-seq (reader fname)))
		'())))))

;(defn parse-file [fname]
;  (do
;    (println (str "Parsing " fname))
;    (parse-file-reader (reader fname))))

(defn parse-app-logs [ date server-id app-id {:keys [path format]}]
  (let [dirfile (java.io.File. path)
	formatted-date (chrono/format-date date :compact-date)
	fnames (map #(str path "\\" % ) (filter #(> (.indexOf % formatted-date) 0)
		       (if dirfile (.list dirfile) [])))
	idx-dir-fname (str "tmp\\" (name server-id) "\\" (name app-id) "\\idx" )
	_ (println (str "=== indexing to " idx-dir-fname))
	idx-dir (index-dir idx-dir-fname) ]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db *db-name*)
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(doall (pmap (partial parse-file-with-writer writer) fnames))
	(println "Indexing is over"))))) 


