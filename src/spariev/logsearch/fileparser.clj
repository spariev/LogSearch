(ns spariev.logsearch.fileparser
 (:use [clojure.contrib.duck-streams :only [reader]]
       [clojure.contrib.str-utils2 :as strutils :only [join]]
       [spariev.chrono :as chrono]
       [spariev.logsearch.db :as db]
       spariev.config
       spariev.logsearch.lucene
       spariev.logsearch.parser
       somnium.congomongo
       [clojure.contrib.seq-utils :only [partition-by]])
 (:import (org.apache.lucene.document CompressionTools)))

(defn parse-and-save-req
  [req-lines]
  (let [gridfs-file (db/pack-into-file :logsfs (rest req-lines))]
    (insert! :railslogs
	     {:hdr (first req-lines)
	      :parsed-hdr (parse-line-regexp processing-rule-regexp (first req-lines))
	      :file_id (str (gridfs-file :_id))})))

(defn process-request-log [index-writer log-chunk]
  (do
    #_(println (first log-chunk))
    (let [mongo-log (parse-and-save-req log-chunk)
	  lucene-doc (parse-log-chunk (.toString (mongo-log :_id)) log-chunk)]
      (.addDocument index-writer lucene-doc)
      nil)))

(defn index-file-with-writer [index-writer fname]
  (let [req-counter (ref 0)]
    (do
      (println (str "Parsing " fname))
      (doall (map (partial process-request-log index-writer)
		(partition-by
		 #(if (. % startsWith "Processing")
		    (dosync (alter req-counter inc))
		    (deref req-counter))
		 (line-seq (reader fname)))))
		'())))

(defn index-file [filename db-name idx-name]
  (let [idx-dir (index-dir idx-name) ]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db db-name)
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(do
	  (parse-file-with-writer writer filename)
	  (println "Indexing is over"))))))


(defn index-app-logs [date config-id]
  (let [path (get-config-val config-id :path)
	dirfile (java.io.File. path)
	formatted-date (chrono/format-date date :compact-date)
	fnames (map #(str path path-sep % ) (filter #(> (.indexOf % formatted-date) 0)
		       (if dirfile (.list dirfile) [])))
	idx-dir-fname (idx-path-for-config config-id)
	_ (println (str "=== indexing to " idx-dir-fname))
	idx-dir (index-dir idx-dir-fname) ]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db *db-name*)
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(doall (pmap (partial parse-file-with-writer writer) fnames))
	(println "Indexing is over")))))


