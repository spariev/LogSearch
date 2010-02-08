(ns spariev.logsearch.lucene
  (:import (org.apache.lucene.index IndexReader IndexWriter
                                    IndexWriter$MaxFieldLength Term)
           (org.apache.lucene.search IndexSearcher BooleanQuery
                                     PhraseQuery BooleanClause$Occur TermQuery)
           (org.apache.lucene.document Document Field Field$Store
                                       Field$Index Field$TermVector DateTools
                                       DateTools$Resolution)
	   (org.apache.lucene.analysis.tokenattributes TermAttribute)
           (org.apache.lucene.analysis SimpleAnalyzer StopAnalyzer WhitespaceAnalyzer CachingTokenFilter)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.store FSDirectory)
	   (org.apache.lucene.search.highlight QueryScorer NullFragmenter Fragmenter SimpleHTMLFormatter SimpleSpanFragmenter Highlighter)
           (org.apache.lucene.queryParser QueryParser$Operator
                                          MultiFieldQueryParser)
           (java.net ServerSocket)
           (java.util Calendar Date SimpleTimeZone)
           (java.text SimpleDateFormat)
           (java.io File))
  (:use [spariev.config :as config]
	clojure.contrib.duck-streams
        clojure.contrib.str-utils
        clojure.contrib.seq-utils
        clojure.contrib.def))


;;; Utility functions

(defn cleanup
  [text]
  (re-gsub #"[\(\),`'=]" "" text))

(defn get-field [#^Document doc field]
  "Return the first value for a given field from a Lucene document."
  (first (.getValues doc field)))

(defn stored-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/YES Field$Index/NOT_ANALYZED))

(defn tokenized-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/NO Field$Index/ANALYZED))

(defn tokenized-no-term-vector-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/NO Field$Index/ANALYZED Field$TermVector/NO))


(defn load-body [#^Document doc [line & lines :as body]]
  "Add each line from body into our Lucene document."
  (if (seq body)
    (do (.add doc (tokenized-field "body" (cleanup line)))
        (recur doc lines))
    doc))

(defn parse-log-chunk [req-id log-chunk]
  "Produce a Lucene document from single request log chunk"
      (let [#^Document doc (Document.)]
        (.add doc (stored-field "req-id" req-id))
        (.add doc (tokenized-field "header" (first log-chunk)))
        (load-body doc (rest log-chunk))))

(defn prepare-lucene-index [idx-dir]
  (doto (IndexWriter. idx-dir (WhitespaceAnalyzer.) IndexWriter$MaxFieldLength/UNLIMITED)
    (.setRAMBufferSizeMB 128)
    (.setUseCompoundFile false)))

(defn index-dir [idx]
  (FSDirectory/open (new java.io.File idx)))

(defn build-query [searchstr]
  (let [all-fields (.parse (doto (MultiFieldQueryParser. org.apache.lucene.util.Version/LUCENE_30
                                      (into-array String ["header" "body"])
                                      (WhitespaceAnalyzer.))
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
    (result-seq (.scoreDocs (.search searcher (build-query query) 50)) searcher)))

(defn display-tokens
  [analyzer text]
  (let [stream (.tokenStream analyzer "contents" (java.io.StringReader. text))
	term (.addAttribute stream TermAttribute)]
    (loop [ strm stream]
      (when (.incrementToken strm)
	(do
	  (print (str "[" (.term term) "] "))
	  (recur strm ))))))
  
(defn demo-analyzers
  [text]
  (doseq [analyzer [(WhitespaceAnalyzer.) (SimpleAnalyzer.) (StopAnalyzer. org.apache.lucene.util.Version/LUCENE_30)
		    (StandardAnalyzer. org.apache.lucene.util.Version/LUCENE_30)]]
    (do (println (str (class analyzer)))
	(println)
	(display-tokens analyzer (cleanup text))
	(println))))

(defn highlight
  [src-text query & options]
  (let [opts (apply hash-map options)
	frag-num (or (opts :frag-num) 100)
	[start-tag end-tag] (or (opts :hl-tags) ["<b>" "</b>"])
	term-query (.parse
		    (MultiFieldQueryParser. org.apache.lucene.util.Version/LUCENE_30
					    (into-array String ["body"])
					    (StandardAnalyzer. org.apache.lucene.util.Version/LUCENE_30))
		    query)
	token-stream (.tokenStream (SimpleAnalyzer.) "body" (java.io.StringReader. src-text))
	scorer (QueryScorer. term-query "body")
	highlighter (doto (Highlighter. (SimpleHTMLFormatter. start-tag end-tag) scorer)
		      (.setTextFragmenter (NullFragmenter.)))
	best-fragments (seq (.getBestFragments highlighter token-stream src-text frag-num))]
    (if (> (count best-fragments) 0) (first best-fragments) src-text)))	



