(ns com.spariev.parse.file
 (:use [clojure.contrib.duck-streams :only [reader]]
   [clojure.contrib.seq-utils :only [partition-by]]))

(defn process-request-log [log-chunk]
  (println "Processing another chunk ..."))

(defn parse-file [fname]
  (let [req-counter (ref 0)]
    (doall (map process-request-log
       (partition-by
         #((if (. % startsWith "Processing")
             (dosync (alter req-counter inc))
             (deref req-counter)))
         (line-seq (reader fname)))))))
