(ns com.spariev.lucene
  (:import (org.apache.lucene.index IndexReader IndexWriter
                                    IndexWriter$MaxFieldLength Term)
           (org.apache.lucene.search IndexSearcher BooleanQuery
                                     PhraseQuery BooleanClause$Occur TermQuery)
           (org.apache.lucene.document Document Field Field$Store
                                       Field$Index DateTools
                                       DateTools$Resolution)
           (org.apache.lucene.analysis SimpleAnalyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.store FSDirectory)
           (org.apache.lucene.queryParser QueryParser$Operator
                                          MultiFieldQueryParser)
           (java.net ServerSocket)
           (java.util Calendar Date SimpleTimeZone)
           (java.text SimpleDateFormat)
           (java.io File))
  (:use clojure.contrib.duck-streams
        clojure.contrib.str-utils
        clojure.contrib.seq-utils
        clojure.contrib.def))


;;; Utility functions
(def *index-filename* "/Users/user/tmp/rails-log-idx")

(defn get-field [#^Document doc field]
  "Return the first value for a given field from a Lucene document."
  (first (.getValues doc field)))

(defn stored-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/YES Field$Index/NOT_ANALYZED))

(defn tokenized-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/NO Field$Index/ANALYZED))

(defn load-body [#^Document doc [line & lines :as body]]
  "Add each line from body into our Lucene document."
  (if (seq body)
    (do (.add doc (tokenized-field "body" line))
        (recur doc lines))
    doc))

(defn parse-log-chunk [req-id log-chunk]
  "Produce a Lucene document from single request log chunk"
      (let [#^Document doc (Document.)]
        (.add doc (stored-field "req-id" req-id))
        (.add doc (tokenized-field "header" (first log-chunk)))
        (load-body    doc (rest log-chunk))))

(defn prepare-lucene-index [idx-dir]
  (doto (IndexWriter. idx-dir (StandardAnalyzer. org.apache.lucene.util.Version/LUCENE_30) IndexWriter$MaxFieldLength/UNLIMITED)
    (.setRAMBufferSizeMB 20)
    (.setUseCompoundFile false)))

(defn index-dir [idx]
  (FSDirectory/open (new java.io.File idx)))

(defn build-query [searchstr]
  (let [all-fields (.parse (doto (MultiFieldQueryParser. org.apache.lucene.util.Version/LUCENE_30
                                      (into-array String ["header" "body"])
                                      (StandardAnalyzer. org.apache.lucene.util.Version/LUCENE_30))
                             (.setDefaultOperator QueryParser$Operator/OR))
                           searchstr)]
    (.setBoost all-fields 20)
    all-fields
  ))

(defn result-seq [hits searcher]
  "Returns a lazy seq of a Lucene Hits object."
  (map #(let [doc (.doc searcher (.doc (aget hits %)))]
          [(get-field doc "req-id")
           (int (* (.score (aget hits %)) 100000))])
       (range (alength hits))))

(defn search [index-file query]
  (let [reader (IndexReader/open (index-dir index-file))
        searcher (IndexSearcher. reader)]
    (result-seq (.scoreDocs (.search searcher (build-query query) 100)) searcher)))


