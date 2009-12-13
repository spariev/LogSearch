
(ns com.spariev.parse.rails
;  (:require  )
  (:use com.spariev.parse.common name.choi.joshua.fnparse clojure.contrib.error-kit
        [clojure.contrib.seq-utils :only [flatten]])
  ;(:import )
  )

(def test-string "Processing EpisodesController#first_unapproved to xml (for 192.168.212.244 at 2009-12-10 04:59:43) [GET]")

(def minus-lit (lit \-))
(def colon-lit (lit \:))

(def controller-method-delimiter (lit \#))

(def controller-and-method
  (complex [controller-name (rep+ (except anything controller-method-delimiter))
            _     controller-method-delimiter
            method-name (rep+ (except anything (lit \space)))]
    (vec (map apply-str [controller-name method-name]))))
  
(def ip-addr (conc 
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit) dec-point
               (rep+ decimal-digit)))

(def datetime-lit (conc
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) minus-lit
                (rep+ decimal-digit) space-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit) colon-lit
                (rep+ decimal-digit)))


(http-method {:remainder (seq "[GET]")})
(result-format {:remainder (seq "xml")})
(ip-addr {:remainder (seq "192.168.212.244")})
(datetime-lit {:remainder (seq "2009-12-10 04:59:43")})

(defstruct state-s :remainder :column :line)

(def remainder-a
  (accessor state-s :remainder))

(def line-rule
  (conc processing ws controller-and-method ws result-format ip-addr ws datetime-lit ws http-method))

(defn parse-line [line-str]
    (rule-match line-rule
      #(println "invalid document \"%s\""   (apply-str (remainder-a %)))
      #(println "leftover data after a valid node \"%s\"" (apply-str (remainder-a %2)))
      (struct state-s (seq line-str) 0 0)))