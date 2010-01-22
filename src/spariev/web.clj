(ns spariev.logsearch.web  
  (:use spariev.config
	spariev.logsearch.lucene
	compojure
	somnium.congomongo))

(defn with-logging
 [handler]
  (fn [request]
    (do
      (println)
      (println (str "Processing " (request :uri)
		    " (for " (request :remote-addr) " at "
		    ") [" (request :request-method) "]"))
      (println (str "  Params: " (request :params)))
      (println)
      (time (handler request)))))

(defn format-search-hit
  [search-result]
  (do
    (println search-result)
    (let
	[request (fetch-one :railslogs :where {:_id (first search-res) })]
      [:div
       [:p (request :hdr)]
       [:p (request :parsed-hdr)]
       [:p (request :body)]])))

(defn search-handler
  [request]
  (do
    (somnium.congomongo/mongo! :db *db-name*)
    (let [search-results (search *index-filename* ((request :params) :query))]
      (html
       (doall (map format-search-result search-results))))))

(decorate search-handler (with-logging))

(defroutes search-routes
  (GET "/search/:query" search-handler)
  (ANY "/*" (do (println (str "Requested " (request :route-params))) 404)))

(defn -main [& args]
  (run-server {:port 6060}
	      "/*" (servlet search-routes)))


  