(ns com.spariev.parse.file
 (:use [clojure.contrib.duck-streams :only [reader]]
       [clojure.contrib.seq-utils :only [partition-all]]))

(defn parse-file [fname]
  (let [req-counter (ref 0)]
  (partition-by #( ) (line-seq (reader fname)))