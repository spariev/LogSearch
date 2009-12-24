
(ns com.spariev.parse.rails
;  (:require  )
  (:use com.spariev.parse.common name.choi.joshua.fnparse clojure.contrib.error-kit
        [clojure.contrib.seq-utils :only [flatten]])
  ;(:import )
  )

(defn to-str [a-seq]
  (reduce str(flatten a-seq)))

(def test-string "Processing EpisodesController#first_unapproved to xml (for 192.168.212.244 at 2009-12-10 04:59:43) [GET]")


(def minus-lit (lit \-))
(def colon-lit (lit \:))

(def controller-method-delimiter (lit \#))

(def controller-and-method
  (complex [controller-name (rep+ (except anything controller-method-delimiter))
            _     controller-method-delimiter
            method-name (rep+ (except anything (lit \space)))]
    (vec (map apply-str [controller-name method-name]))))
  
(def ip-addr
  (complex [ip (conc
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit))]
    (reduce str (flatten ip))))

(def datetime
  (complex [datetime-parsed (conc
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) space-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit))]
                (to-str datetime-parsed)))

(def line-datetime
  (complex [datetime-parsed (conc
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) space-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit))
            _  dec-point
            _  (rep+ decimal-digit)]
                (to-str datetime-parsed)))

(def completed-string
  "18112: 2009-12-10 04:59:43.108943 | Completed in 405ms (View: 1, DB: 15) | 422 Unprocessable Entity [http://admin.telemarker.home/episodes/first_unapproved.xml?lang=ru] ")

(def process-id
  (semantics (rep+ (except decimal-digit colon-lit)) to-str))
(def duration
  (semantics (rep+ (except decimal-digit (word-lit "ms"))) to-str))

(def view-duration
  (complex [ _        (word-lit "View: ")
             view-dur (rep+ decimal-digit)
             _        (lit \,)]
    (to-str view-dur)))

(def db-duration
  (complex [ _      (word-lit "DB: ")
             db-dur (rep+ decimal-digit)
             _      (lit \))]
    (to-str db-dur)))

(def http-code
  (semantics (rep+ decimal-digit) to-str))

(def http-code-desc
  (semantics (rep+ (except anything (lit \[))) to-str))

(def full-url
  (complex [ _        (lit \[)
             url (rep+ (except anything (lit \])))
             _        (lit \])]
    (to-str url)))


;(def completed-line-rule
;  (conc process-id line-datetime ws completed ws duration ws view-duration
;    db-duration ws http-code ws result-format ws ip-addr ws datetime ws http-method))

(defstruct state-s :remainder)

(def remainder-a
  (accessor state-s :remainder))


(defn inv-doc-fn [state]
  (println "invalid document \"%s\""   (apply-str (remainder-a state))))

(defn data-left-fn [data-left state]
  (println "leftover data after a valid node \"%s\"" (apply-str (remainder-a state))))

(def processing-line-rule
  (conc processing ws controller-and-method ws result-format ws ip-addr ws datetime ws http-method))

(def completed-line-rule
  (conc process-id colon-lit ws line-datetime ws completed ws duration (word-lit "ms") ws
    view-duration ws db-duration ws http-code ws http-code-desc full-url ws))

(defn parse-line [line-str line-rule]
    (rule-match line-rule inv-doc-fn data-left-fn (struct state-s (seq line-str))))