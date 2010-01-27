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
  (let
      [request-rec (fetch-one :railslogs :where { :_id (com.mongodb.ObjectId. (first search-result)) })]
    (do
      #_(println search-result)
      #_(println request-rec)
      [:div
       [:p (request-rec :hdr)]
       #_[:p (request-rec :parsed-hdr)]
       [:p [:code (request-rec :body)]]])))

(defn search-handler
  [request]
  (do
    (somnium.congomongo/mongo! :db *db-name*)
    (let [search-results (search *index-filename* ((request :params) :query))]
      (html
       (doall (map format-search-hit search-results))))))

(decorate search-handler (with-logging))

(defroutes search-routes
  (GET "/search/:query" search-handler)
  (ANY "/*" (do (println (str "Requested " (request :route-params))) 404)))

(defn -main [& args]
  (run-server {:port 6060}
	      "/*" (servlet search-routes)))


  