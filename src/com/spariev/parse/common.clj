
(ns com.spariev.parse.common
  ;(:require )
  (:use name.choi.joshua.fnparse clojure.contrib.error-kit
        [clojure.contrib.seq-utils :only [flatten]])
  ;(:import )
  )

(defn word-lit [wrd]
  (lit-conc-seq (seq wrd)))

(def get-mtd-lit (word-lit "[GET]"))
(def post-mtd-lit (word-lit "[POST]"))

(def apply-str
  (partial apply str))

(def space-lit (lit \space))
(def tab-lit (lit \tab))
(def ignored-words-lit (alt (word-lit "to") (word-lit "for") (word-lit "at")))

(def processing (constant-semantics (word-lit "Processing") :processing))
(def completed  (constant-semantics (word-lit "Completed in") :completed))

(def ws (constant-semantics (rep* (alt ignored-words-lit (lit \|) (lit \() (lit \)) space-lit tab-lit )) :ws))

(def dec-point (lit \.))
(def decimal-digit (lit-alt-seq "0123456789"))

(def result-format
  (alt
    (constant-semantics (word-lit "xml") :xml-format)
    (constant-semantics (word-lit "html") :html-format)))

(def get-mtd (constant-semantics get-mtd-lit :get-method))
(def post-mtd (constant-semantics post-mtd-lit :post-method))
(def http-method (alt get-mtd post-mtd))

