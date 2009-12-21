(ns com.spariev.parse.file
 (:use [clojure.contrib.duck-streams :only [reader]]
   com.spariev.db
   com.spariev.lucene
   [clojure.contrib.seq-utils :only [partition-by]]))

(defn process-request-log [index-writer req-id log-chunk]
  (let [mongo-log (save-req (deref req-id) log-chunk)
        lucene-doc (parse-log-chunk (.toString (mongo-log :_id)) log-chunk)]
    (.addDocument index-writer lucene-doc)
    nil))

(defn parse-file [fname]
  (let [req-counter (ref 0)
        idx-dir (index-dir *index-filename*)]
    (org.apache.lucene.index.IndexWriter/unlock idx-dir)
    (somnium.congomongo/mongo! :db "rails-logs-test3")
     (with-open [#^org.apache.lucene.index.IndexWriter writer (prepare-lucene-index idx-dir)]
       (doall (map (partial process-request-log writer req-counter)
       (partition-by
         #(if (. % startsWith "Processing")
             (dosync (alter req-counter inc))
             (deref req-counter))
         (line-seq (reader fname))))))
   ))
