(ns spariev.logsearch.parser
  (:use name.choi.joshua.fnparse
	clojure.contrib.error-kit
        [clojure.contrib.seq-utils :only [flatten]]))

(defn to-str [a-seq]
  (reduce str "" (flatten a-seq)))

(def processing-test-string "Processing EpisodesController#first_unapproved to xml (for 192.168.212.244 at 2009-12-10 04:59:43) [GET]")
(def completed-test-string
  "18112: 2009-12-10 04:59:43.108943 | Completed in 405ms (View: 1, DB: 15) | 422 Unprocessable Entity [http://admin.telemarker.home/episodes/first_unapproved.xml?lang=ru] ")

(def minus-lit (lit \-))
(def colon-lit (lit \:))
(defn word-lit [wrd]
  (lit-conc-seq (seq wrd)))

(def space-lit (lit \space))
(def tab-lit (lit \tab))
(def ignored-words-lit (alt (word-lit "to") (word-lit "for") (word-lit "at")))

(def ws (constant-semantics (rep* (alt ignored-words-lit (lit \|) (lit \() (lit \)) space-lit tab-lit )) :ws))

(def dec-point (lit \.))
(def decimal-digit (lit-alt-seq "0123456789"))

(def digits
  (rep+ decimal-digit))
  
(def ip-addr-lit
  (conc digits dec-point digits dec-point digits dec-point digits))

(def datetime-lit
  (conc
    digits minus-lit digits minus-lit digits
    space-lit
    digits colon-lit digits colon-lit digits))

(def processing-rule-complex
  (complex [ _               (word-lit "Processing")
             _               ws
             controller-name (rep+ (except anything (lit \#)))
             _               (lit \#)
             method-name     (rep+ (except anything space-lit))
             _               ws
             result-format   (alt (word-lit "xml") (word-lit "json") (word-lit "yml") (word-lit "html") (word-lit "xls") ws)
             _               ws
             ip-addr         ip-addr-lit
             _               ws
             req-time        datetime-lit
             _               ws
             _               (lit \[)
             http-method     (alt (word-lit "GET") (word-lit "POST") (word-lit "PUT") (word-lit "DELETE"))
             _               (lit \])]
    {:start-time (to-str req-time) :controller (to-str controller-name) :method (to-str method-name)
     :remote-addr (to-str ip-addr) :format (to-str result-format) :http-method (to-str http-method)})); TODO come up with more clever way to do it

(def completed-rule-complex
  (complex [ process-id digits
             _           (lit \:)
             _           ws
             datetime    datetime-lit
             _           dec-point
             _           digits
             _           ws
             _           (word-lit "Completed in")
             _           ws
             duration    digits
             _           (word-lit "ms")
             _           ws
             _           (word-lit "View: ")
             view-dur    digits
             _           (lit \,)
             _           ws
             _           (word-lit "DB: ")
             db-dur      digits
             _           (lit \))
             _           ws
             http-code   digits
             _           ws
             http-code-desc (rep+ (except anything (lit \[)))
             _           (lit \[)
             full-url    (rep+ (except anything (lit \])))
             _           (lit \])
             _           ws
             ]
    { :process-id (to-str process-id) :end-time (to-str datetime) :duration (to-str duration)
      :view-duration (to-str view-dur) :db-duration (to-str db-dur)
      :http-code (to-str http-code) :url (to-str full-url) }))

(defstruct state-s :remainder)
(def remainder-a
  (accessor state-s :remainder))


(defn inv-doc-fn [state]
  (println "invalid document \"%s\"" (to-str (remainder-a state))))

(defn data-left-fn [data-left state]
  (println "leftover data after a valid node \"%s\"" (to-str (remainder-a state))))

(defn parse-line [line-rule line-str]
    (rule-match line-rule inv-doc-fn data-left-fn (struct state-s (seq line-str))))

;(defn parse-processed-line
;  (partial parse-line processed-rule-complex))

;(defn parse-completed-line
;  (partial parse-line completed-rule-complex))