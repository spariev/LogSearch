(ns spariev.logsearch.parser)

(def processing-test-string "Processing EpisodesController#first_unapproved to xml (for 192.168.212.244 at 2009-12-10 04:59:43) [GET]")
(def completed-test-string
  "18112: 2009-12-10 04:59:43.108943 | Completed in 405ms (View: 1, DB: 15) | 422 Unprocessable Entity [http://admin.telemarker.home/episodes/first_unapproved.xml?lang=ru] ")

(def processing-rule-regexp
     #"Processing ((?:\w+::)*\w+)#(\w+)(?: to (\w+))? \(for (\d+\.\d+\.\d+\.\d+) at (\d+-\d+-\d+ \d+:\d+:\d+)\) \[(.*)\]")

(defn parse-line-regexp
  [re line]
  (let [matches (first (re-seq re line))
	r-keys [:controller-name :method-name :result-format :ip-addr :req-time :http-method]]
    (if (= 7 (count matches))
      (zipmap r-keys (rest matches))
      (do
	(println (str "Invalid line " line))
	{}))))
