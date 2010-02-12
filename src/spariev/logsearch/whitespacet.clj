(ns spariev.logsearch.whitespacet
  (:gen-class
   :extends org.apache.lucene.analysis.CharTokenizer))

(def *stop-chars-set*
     (hash-set \( \[ \] \) \, \` \' \= \< \> \" ))

(defn -isTokenChar [this c]
  (and (not (Character/isWhitespace (char c))) (not (*stop-chars-set* (char c)))))
