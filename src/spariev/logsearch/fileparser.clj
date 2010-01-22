(ns spariev.logsearch.fileparser
 (:use [clojure.contrib.duck-streams :only [reader]]
       spariev.config
       spariev.logsearch.lucene
       spariev.logsearch.parser
       somnium.congomongo
       [clojure.contrib.seq-utils :only [partition-by]]))

(defn parse-and-save-req
  [req-lines]
  (insert! :railslogs
    {:hdr (first req-lines)
     :parsed-hdr (parse-line processing-rule-complex (first req-lines))
     :body (rest req-lines)}))

(defn process-request-log [index-writer log-chunk]
  (do
    #_(println (first log-chunk))
    (let [mongo-log (parse-and-save-req log-chunk)
	  lucene-doc (parse-log-chunk (.toString (mongo-log :_id)) log-chunk)]
      (.addDocument index-writer lucene-doc)
      nil)))

(defn parse-file [fname]
  (let [req-counter (ref 0)
        idx-dir (index-dir *index-filename*)]
    (do
      (org.apache.lucene.index.IndexWriter/unlock idx-dir)
      (somnium.congomongo/mongo! :db *db-name*)
      (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
	(doall (map (partial process-request-log writer)
		     (partition-by
		      #(if (. % startsWith "Processing")
			 (dosync (alter req-counter inc))
			 (deref req-counter))
		      (line-seq (reader fname))))))
      (println (str fname " processed")))))
