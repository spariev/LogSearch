(ns spariev.logsearch.fileparser
  (:refer-clojure :exclude [contains?])
  (:use [clojure.contrib.duck-streams :only [reader]]
       [clojure.contrib.str-utils2 :as strutils :only [join blank?]]
       [spariev.chrono :as chrono]
       [spariev.logsearch.db :as db]
       spariev.config
       spariev.logsearch.lucene
       spariev.logsearch.parser
       [clojure.contrib.seq-utils :only [partition-by]])
  (:require [cupboard.core :as cb])
  (:import (org.apache.lucene.document CompressionTools)))

#_(set! *warn-on-reflection* true)

(defn parse-log-chunk
  [chunk]
  (let [hdr (first chunk)
	parsed-hdr (parse-processing-line (first chunk))
	cntr (ref 0)
	content
	(->> (partition-by #(if (ordinary-line? %)
			      (dosync (alter cntr inc))
			      (deref cntr))
			   (rest chunk))
	     (map #(parse-ordinary-line (strutils/join " " %)))
	     (map #(% :content))
	     (filter #(not (strutils/blank? %))))
	parsed-ftr
	(let [complete-lines (map parse-completed-line
				  (filter #(completed-line? %) content))]
	  (if (> (count complete-lines) 0)
	    (first complete-lines)
	    ;; there could be only one completed line anyway
	    {}))]
    {:hdr hdr
     :attrs (into parsed-hdr parsed-ftr)
     :content content }))

(defn save-req
  [req-lines parsed-chunk cb-db]
  (db/save-log-entry cb-db (rest req-lines)))

(defn- save-lucene-doc
  [index-writer lucene-doc]
  (.addDocument #^org.apache.lucene.index.IndexWriter index-writer lucene-doc))

(defn process-request-log
  [cb-db index-writer log-chunk]
  (println "process-request-log" cb-db "|")
  (if (and (> (count log-chunk) 1) (processing-line? (first log-chunk)))
    (let [parsed-chunk (parse-log-chunk log-chunk)
	  log-entry-id (save-req log-chunk parsed-chunk cb-db)
	  lucene-doc (create-doc-from-chunk log-entry-id parsed-chunk)]
      (save-lucene-doc index-writer lucene-doc)
      nil)))

(defn index-file-with-writer
  [cb-db index-writer fname]
  (let [req-counter (ref 0)]
    (println (str "Parsing " fname))
    (doall (map (partial process-request-log cb-db index-writer)
		(partition-by
		 #(if (. #^String % startsWith "Processing")
		    (dosync (alter req-counter inc))
		    (deref req-counter))
		 (line-seq (reader fname)))))
    '()))

(defn index-file [filename config-id]
  (let [idx-dir (index-dir (idx-path-for-config config-id))
	cb-db (cb/open-cupboard(db-path-for-config config-id))]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (with-open [#^org.apache.lucene.index.IndexWriter writer
		  (prepare-lucene-index idx-dir)]
	(do
	  (index-file-with-writer cb-db writer filename)
	  (cb/close-cupboard cb-db)
	  (println "Indexing is over"))))))

(defn index-app-logs [config-id formatted-date]
  (let [path (get-config-val config-id :path)
	dirfile (java.io.File. #^String path)
	fnames (sort (map #(str path path-sep % )
		    (filter #(> (.indexOf #^String % #^String formatted-date) 0)
		       (if dirfile (.list dirfile) []))))
	idx-dir-fname (idx-path-for-config config-id)
	_ (println (str "=== indexing to " idx-dir-fname))
;;	_ (println fnames)
	idx-dir (index-dir idx-dir-fname)
	cb-db (cb/open-cupboard (db-path-for-config config-id))]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (with-open [#^org.apache.lucene.index.IndexWriter writer
		  (prepare-lucene-index idx-dir)]
	(doall (map (partial index-file-with-writer cb-db writer) fnames))
	(cb/close-cupboard cb-db)
	(println "Indexing is over")))))
