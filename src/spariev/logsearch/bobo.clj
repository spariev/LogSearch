(ns spariev.logsearch.bobo
  (:use spariev.logsearch.lucene
	[spariev.logsearch.parser :as parser])
  (:import (com.browseengine.bobo.facets.impl SimpleFacetHandler)
	   (com.browseengine.bobo.facets FacetHandler)
	   (com.browseengine.bobo.api BrowseRequest BrowseSelection
				      BrowseException BrowseResult
				      BrowseFacet BoboIndexReader
				      Browsable BoboBrowser BrowseHit)))

(defn facet-handlers
     [attr-values]
     (map #(SimpleFacetHandler. %) (keys attr-values)))

(defn- browse-result-seq [hits index-reader]
  "Returns a lazy seq of a BrowseHits object."
  (map #(let [doc (.document
		   index-reader
		   (.doc (aget hits %)))]
	  {:header (get-field doc "header")
	   :id (get-field doc "id")
	   :score (int (* (.score (aget hits %)) 100000))})
       (range (alength hits))))

(defn browse
  [index-reader query attrs-values options]
  (let [facets (facet-handlers attrs-values)
	bobo-reader (BoboIndexReader/getInstance index-reader
						 (java.util.ArrayList. facets))
	lucene-query          (build-query query)
	browse-selections (map #(doto (BrowseSelection. %)
				  (.addValue (attrs-values %)))
			       (keys attrs-values))
	browse-request (doto (BrowseRequest.)
			 (.setCount  (or (options :count) 100))
			 (.setOffset (or (options :offset) 0))
			 (.setQuery lucene-query))
	_              (doseq [sel browse-selections]
			   (.addSelection browse-request sel))
	browser (BoboBrowser. bobo-reader)
	browse-result (.browse browser browse-request)
	total-count (.getNumHits browse-result)
	hits (.getHits browse-result)
	]
    {:total-count total-count :results hits}))
