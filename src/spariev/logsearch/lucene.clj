(ns spariev.logsearch.lucene
  (:import (org.apache.lucene.index IndexReader IndexWriter
				    IndexWriter$MaxFieldLength Term)
	   (org.apache.lucene.search Query IndexSearcher BooleanQuery
				     PhraseQuery BooleanClause$Occur TermQuery)
	   (org.apache.lucene.document Document Field Field$Store
				       Field$Index Field$TermVector DateTools
				       DateTools$Resolution)
	   (org.apache.lucene.analysis.tokenattributes TermAttribute)
	   (org.apache.lucene.analysis SimpleAnalyzer StopAnalyzer
				       WhitespaceAnalyzer LowerCaseFilter
				       LetterTokenizer CachingTokenFilter
				       StopFilter WhitespaceTokenizer
				       CharTokenizer)
	   (org.apache.lucene.analysis.standard StandardAnalyzer)
	   (org.apache.lucene.document CompressionTools)
	   (org.apache.lucene.store FSDirectory)
	   (org.apache.lucene.search.highlight QueryScorer NullFragmenter
					       Fragmenter SimpleHTMLFormatter
					       SimpleSpanFragmenter Highlighter)
	   (org.apache.lucene.queryParser QueryParser$Operator
					  MultiFieldQueryParser)
	   (java.net ServerSocket)
	   (java.util Calendar Date SimpleTimeZone)
	   (java.text SimpleDateFormat)
	   (java.io File))
  (:use [spariev.config :as config]
	[clojure.contrib.str-utils2 :as strutils :only [join blank?]]
	spariev.logsearch.whitespacet
	[spariev.logsearch.parser :as parser]
	clojure.contrib.duck-streams
	clojure.contrib.str-utils
	clojure.contrib.seq-utils
	clojure.contrib.def))

#_(set! *warn-on-reflection* true)


;;; Utility functions

(defn get-field [#^Document doc field]
  "Return the first value for a given field from a Lucene document."
  (first (.getValues doc field)))

(defn attr-field [name val]
  "Create an attribute field - stored, not tokenized."
  (Field. name val Field$Store/YES Field$Index/NOT_ANALYZED))

(defn tokenized-nonstored-field [name val]
  "Create a tokenized, non-stored field."
  (Field. name val Field$Store/NO Field$Index/ANALYZED))

(defn tokenized-stored-field [name val]
  "Create a tokenized, stored field."
  (Field. name val Field$Store/NO Field$Index/ANALYZED))

(defn tokenized-no-term-vector-field [name val]
  "Create a tokenized, non-stored field with no term-vector info."
  (doto (Field. #^String name #^String val
		Field$Store/NO Field$Index/ANALYZED Field$TermVector/NO)
    (.setOmitTermFreqAndPositions true)
    (.setOmitNorms true)))

(defn create-doc-from-chunk [req-id parsed-chunk]
  "Produce a Lucene document from single request log chunk"
      (let [#^Document doc (Document.)
	    body-content (if (:content parsed-chunk)
			   (strutils/join " " (:content parsed-chunk)) "")
;;           compressed-body (CompressionTools/compressString body-content)
;;           compressed-field (doto (Field. "compressed-body"
;;                                          compressed-body Field$Store/YES)
;;                              (.setOmitTermFreqAndPositions true)
;;                              (.setOmitNorms true))
	    ]
	(.add doc (attr-field "req-id" req-id))
	(doseq [attr-name parser/*req-attrs*]
	  (.add doc (attr-field (name attr-name)
				(or (-> parsed-chunk :attrs attr-name) ""))))
	(.add doc (tokenized-stored-field "header" (:hdr parsed-chunk)))
	(.add doc (tokenized-no-term-vector-field "body" body-content))
;;	(.add doc compressed-field)
	doc))

(def *cleanup-analyzer*
     (proxy [org.apache.lucene.analysis.Analyzer] []
       (tokenStream [#^String fieldName #^java.io.Reader reader]
		    (LowerCaseFilter.
		     (spariev.logsearch.whitespacet. reader)))))

(defn prepare-lucene-index [idx-dir]
  (doto (IndexWriter. idx-dir *cleanup-analyzer*
		      IndexWriter$MaxFieldLength/UNLIMITED)
    (.setRAMBufferSizeMB 128)
    (.setUseCompoundFile false)))

(defn index-dir [idx]
  (FSDirectory/open (java.io.File. #^String idx)))

(defn extract-terms
  [analyzer text]
  (let [stream (.tokenStream analyzer "contents" (java.io.StringReader. text))
	term (.addAttribute stream TermAttribute)]
    (loop [ term-seq [] strm stream]
      (if (not (.incrementToken strm))
	term-seq
	(recur (conj term-seq (.term term) ) strm )))))

(defn build-query [searchstr]
  (let [terms (extract-terms *cleanup-analyzer* searchstr)
	_     (println (str "search terms " terms))
	header-query (BooleanQuery.)
	_            (doall (map #(.add header-query
					(TermQuery. (Term. "header" %))
					BooleanClause$Occur/SHOULD) terms))
	body-query   (BooleanQuery.)
	_            (doall (map #(.add body-query
					(TermQuery. (Term. "body" %))
					BooleanClause$Occur/MUST) terms))
	all-fields-query (doto (BooleanQuery.)
			   (.add header-query BooleanClause$Occur/SHOULD)
			   (.add body-query BooleanClause$Occur/MUST))]
    all-fields-query))


(defn result-seq [hits searcher]
  "Returns a lazy seq of a Lucene Hits object."
  (map #(let [doc (.doc searcher (.doc (aget hits %)))]
	  [(get-field doc "req-id")
	   (int (* (.score (aget hits %)) 100000))])
       (range (alength hits))))

(defn search [index-file query]
  (let [reader (IndexReader/open (index-dir index-file))
	searcher (IndexSearcher. reader)]
    (result-seq (.scoreDocs (.search searcher (build-query query) 5000))
		searcher)))

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
  (doseq [analyzer [*cleanup-analyzer* (WhitespaceAnalyzer.) (SimpleAnalyzer.)
		    (StopAnalyzer. org.apache.lucene.util.Version/LUCENE_30)
		    (StandardAnalyzer.
		     org.apache.lucene.util.Version/LUCENE_30)]]
    (do (println (str (class analyzer)))
	(println)
	(display-tokens analyzer text)
	(println))))

(defn highlight
  [src-text query & options]
  (let [opts (apply hash-map options)
	frag-num (or (opts :frag-num) 100)
	[start-tag end-tag] (or (opts :hl-tags)
				["<span class=\"hlight\">" "</span>"])
	term-query (TermQuery. (Term. "body" query))
	token-stream (.tokenStream *cleanup-analyzer*
				   "body" (java.io.StringReader. src-text))
	scorer (QueryScorer. term-query "body")
	highlighter (doto (Highlighter.
			   (SimpleHTMLFormatter. start-tag end-tag) scorer)
		      (.setTextFragmenter (NullFragmenter.)))
	best-fragments (seq (.getBestFragments highlighter
					       token-stream src-text frag-num))]
    (if (> (count best-fragments) 0) (first best-fragments) src-text)))
