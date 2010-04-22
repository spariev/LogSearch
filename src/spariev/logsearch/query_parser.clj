(ns spariev.logsearch.query-parser
  (:use name.choi.joshua.fnparse clojure.contrib.error-kit))

(defstruct state-s :remainder :column)

(def remainder-a
     (accessor state-s :remainder))

(deferror parse-error [] [state message message-args]
  {:msg (str (format "Parse error at column %s: " (:column state))
             (apply format message message-args))
   :unhandled (throw-msg Exception)})

(defn- field-error-fn [expectation]
  (fn [remainder state]
    ))

(defn inc-rule [subrule]
     (invisi-conc subrule (update-info :column inc)))

(def inc-lit
     (comp inc-rule lit))

(def ws
     (complex [_ (alt (inc-lit \tab) (inc-lit \space))]
	      :space))

(defn word-lit [wrd]
  (lit-conc-seq (seq wrd) inc-lit))

(def field-lit
     (alt (word-lit "ip-addr") (word-lit "time") (word-lit "status") (word-lit "duration")
	  (word-lit "http-method") (word-lit "url") (word-lit "controller-name") (word-lit "method-name")
	  (word-lit "result-format") (word-lit "view") (word-lit "db")))


(def str-indicator
     (alt (inc-lit \") (inc-lit \')))

(def string-lit
     (complex [open-indicator (opt str-indicator)
	       content (rep* (except anything
				     (if open-indicator  (inc-lit open-indicator) ws)))
	       _ (if open-indicator (inc-lit open-indicator) (opt str-indicator))]
	      (apply str content)))

(def field-cond
     (complex [field field-lit
	       fcond (failpoint (alt (word-lit "=") (word-lit "!="))
				(fn [rem state]
				  (raise parse-error state "condition = or != expected at \"%s\"" [ rem ])))
	       val   string-lit]
	      (zipmap [:field :cond :value] (map #(apply str %) [field fcond val]))))

(def attr-root-rule
    (rep* (alt ws field-cond))) ;;(alt field-cond ws)

(def query-root-rule
    (rep* (alt ws string-lit))) ;;(alt field-cond ws)


(defn inv-doc-fn [state]
  (println "invalid document " (str (remainder-a state))))

(defn data-left-fn [data-left state]
  (println "leftover data after a valid term " (str (remainder-a state))))

(defn parse-conditions
  "Returns a seq of maps with conditions."
  [string-value]
  
  (remove #(= :space %) (rule-match attr-root-rule inv-doc-fn data-left-fn (struct state-s (seq string-value) 0))))

(defn parse-lucene-query
  "Returns seq of search terms."
  [string-value]
  (remove #(= :space %) (rule-match query-root-rule inv-doc-fn data-left-fn (struct state-s (seq string-value) 0))))