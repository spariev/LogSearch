(ns spariev.logsearch.parser
  (:refer-clojure :exclude [contains?])
  (:use [clojure.contrib.str-utils2 :as strutils :only [contains?]]))

#_(set! *warn-on-reflection* true)

(def *processing-attrs* [:controller-name :method-name :result-format :ip-addr :req-time :http-method])

(def *completed-attrs* [:duration :view :db :status :url])

(def *req-attrs* (into *processing-attrs* *completed-attrs*))



(def processing-test-string "Processing EpisodesController#first_unapproved to xml (for 192.168.212.244 at 2009-12-10 04:59:43) [GET]")
(def completed-test-string
  "18112: 2009-12-10 04:59:43.108943 | Completed in 405ms (View: 1, DB: 15) | 422 Unprocessable Entity [http://admin.telemarker.home/episodes/first_unapproved.xml?lang=ru] ")

(def processing-line-check
     "Processing ")
(def processing-line-regexp
     #"Processing ((?:\w+::)*\w+)#(\w+)(?: to (\w+))? \(for (\d+\.\d+\.\d+\.\d+) at (\d+-\d+-\d+ \d+:\d+:\d+)\) \[(.*)\]")

(def completed-line-check
     "Completed in ")
(def completed-line-regexp
     #"Completed in (\d+)ms \((?:View: (\d+), )?DB: (\d+)\) \| (\d\d\d).+\[(http.+)\]")

(def ordinary-line-regexp
     #"(\d+): (\d+-\d+-\d+ \d+:\d+:\d+)\.\d\d\d\d\d\d \|(.*)")

(def ordinary-line-check
     #"(\d+): (\d+-\d+-\d+ \d+:\d+:\d+)\.\d\d\d\d\d\d \|(.*)")

(defn processing-line?
  [line]
  (strutils/contains? line processing-line-check))

(defn completed-line?
  [line]
  (strutils/contains? line completed-line-check))

;; TODO check this later
;; (defn ordinary-line?
;;  [line thread-num]
;;  (.startsWith line thread-num))

(defn ordinary-line?
  [line]
  (re-matches ordinary-line-check line))

(defn parse-line-regexp
  [line re r-keys]
  (let [matches (first (re-seq re line))]
    (do
      #_(println (rest matches))
      #_(println (count (rest matches)))
      (if (= (count r-keys) (count (rest matches)))
	(zipmap r-keys (rest matches))
	(do
	  (println (str "Invalid line " line))
	  {})))))

(defn parse-processing-line
  [line]
  (parse-line-regexp line processing-line-regexp *processing-attrs*))

(defn parse-completed-line
  [line]
  (parse-line-regexp line completed-line-regexp *completed-attrs*))

(defn parse-ordinary-line
  [line]
  (parse-line-regexp line ordinary-line-regexp [:thread :datetime :content]))

