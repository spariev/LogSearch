(ns spariev.logsearch.fileparser
  (:refer-clojure :exclude [contains?])
  (:use [clojure.contrib.duck-streams :only [reader]]
       [clojure.contrib.str-utils2 :as strutils :only [join blank?]]
       [spariev.chrono :as chrono]
       [spariev.logsearch.db :as db]
       spariev.config
       spariev.logsearch.lucene
       spariev.logsearch.parser
       somnium.congomongo
       [clojure.contrib.seq-utils :only [partition-by]])
 (:import (org.apache.lucene.document CompressionTools)))

#_(set! *warn-on-reflection* true)

(defn parse-log-chunk
  [chunk]
  (do
    #_(println "=== ")
    (let [hdr (first chunk)
	parsed-hdr (parse-processing-line (first chunk))
	cntr (ref 0)
	content (->> (partition-by #(if (ordinary-line? %)
				      (dosync (alter cntr inc))
				      (deref cntr))
				   (rest chunk))
		     (map #(parse-ordinary-line (strutils/join " " %)))
		     (map #(% :content))
		     (filter #(not (strutils/blank? %))))
        parsed-ftr (let [complete-lines (map parse-completed-line (filter #(completed-line? %) content))]
		     (if (> (count complete-lines) 0)
		       (first complete-lines) ; there could be only one completed line anyway
		       {}))]
    {:hdr hdr
     :attrs (into parsed-hdr parsed-ftr)
     :content content })))

(defn save-req
  [req-lines parsed-chunk config-id]
  (let [gridfs-file (db/pack-into-file (fs-name-for-config config-id) (rest req-lines))
	{:keys [hdr attrs]} parsed-chunk]
    (insert! (db-name-for-config config-id)
	     {:hdr hdr
	      :attr attrs
	      :file_id (str (gridfs-file :_id))})))

(defn process-request-log [config-id index-writer log-chunk]
  (if (and (> (count log-chunk) 1) (processing-line? (first log-chunk)))
    (let [parsed-chunk (parse-log-chunk log-chunk)
	  mongo-log (save-req log-chunk parsed-chunk config-id)
	  lucene-doc (create-doc-from-chunk (.toString #^com.mongodb.ObjectId (mongo-log :_id)) parsed-chunk)]
      (.addDocument #^org.apache.lucene.index.IndexWriter index-writer lucene-doc)
      nil)))

(defn index-file-with-writer [config-id index-writer fname]
  (let [req-counter (ref 0)]
    (do
      (println (str "Parsing " fname))
      (doall (map (partial process-request-log config-id index-writer)
		(partition-by
		 #(if (. #^String % startsWith "Processing")
		    (dosync (alter req-counter inc))
		    (deref req-counter))
		 (line-seq (reader fname)))))
		'())))

(defn index-file [filename config-id]
  (let [idx-dir (index-dir (idx-path-for-config config-id))]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db (db-name-for-config config-id))
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(do
	  (index-file-with-writer config-id writer filename)
	  (println "Indexing is over"))))))


(defn index-app-logs [config-id date]
  (let [path (get-config-val config-id :path)
	dirfile (java.io.File. #^String path)
	formatted-date (chrono/format-date date :compact-date)
	fnames (sort (map #(str path path-sep % )
		    (filter #(> (.indexOf #^String % #^String formatted-date) 0)
		       (if dirfile (.list dirfile) []))))
	idx-dir-fname (idx-path-for-config config-id)
	_ (println (str "=== indexing to " idx-dir-fname))
;;	_ (println fnames)
	idx-dir (index-dir idx-dir-fname) ]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db (db-name-for-config config-id))
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(doall (pmap (partial index-file-with-writer config-id writer) fnames))
	(println "Indexing is over")))))


