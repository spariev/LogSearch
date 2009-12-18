(ns com.spariev.parse.file
 (:use [clojure.contrib.duck-streams :only [reader]]
   com.spariev.db
   [clojure.contrib.seq-utils :only [partition-by]]))

(defn process-request-log [req-id log-chunk]
  (save-req (deref req-id) log-chunk)
  [])

(defn parse-file [fname]
  (let [req-counter (ref 0)]
    (doall (map (partial process-request-log req-counter)
       (partition-by
         #(if (. % startsWith "Processing")
             (dosync (alter req-counter inc))
             (deref req-counter))
         (line-seq (reader fname)))))))
