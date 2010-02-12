(ns spariev.web
  (:use spariev.config
	spariev.logsearch.lucene
	spariev.logsearch.fileparser
	spariev.logsearch.db
	spariev.logsearch.util
	[spariev.chrono :as chrono]
	compojure
	somnium.congomongo)
  (:gen-class))

#_(set! *warn-on-reflection* true)

(defn with-logging
 [handler]
  (fn [request]
    (do
      (println)
      (println (str "Processing " (request :uri)
		    " (for " (request :remote-addr) " at " (chrono/format-date (chrono/now) :db-date-time)
		    ") [" (request :request-method) "]"))
      (println (str "  Params: " (request :params)))
      (println)
      (time (handler request)))))


(defn format-search-hit
  [config-id query search-result]
  (let
      [obj-id  (first search-result)
 ;      _ (println (str query " -> " obj-id " -> " (second search-result)))
       request-rec (fetch-one (db-name-for-config config-id) :where { :_id (com.mongodb.ObjectId. obj-id) })
;       #_ _ (println (str (request-rec :file_id)))
       body-content (unpack-from-file (fs-name-for-config config-id) (request-rec :file_id))
       highlighted-body (highlight body-content query)]
    (do
      #_(println search-result)
      #_(println request-rec)
      [:div
       [:p (request-rec :hdr)]
       #_[:p (request-rec :parsed-hdr)]
       [:p [:pre highlighted-body]]])))


(defn do-search
  [config-id query]
  (do
    (somnium.congomongo/mongo! :db (db-name-for-config config-id))
    (let [search-results (bench "Lucene search " (search (idx-path-for-config config-id) query))]
      (bench "Rendering " (html
			   [:div.results-count [:p (str (count search-results) " entries found")]]
			   (doall (map (partial format-search-hit config-id query) search-results)))))))

(defn search-handler
  [request]
  (let [{:keys [config-id query]} (request :params)]
  (do-search config-id query)))


(defn with-std-template
  [page-title body]
  (html
    (doctype :html4)
    [:html "\n"
     [:head "\n"
      [:title page-title] "\n"
      [:link {:href "/main.css" :rel "stylesheet" :type "text/css"} ]] "\n"
     [:body "\n"
      [:div.header
       [:h2 page-title]] "\n"
      [:div.body
       body]]]))

(defn search-form
  [request]
  (with-std-template "LogSearch"
    [:div [:div.search
	   (form-to
	    [:post "/search"]
	    [:p "config-id " (text-field {:size 10} :config-id)]
	    [:p "query "     (text-field {:size 50} :query)]
	    (submit-button "Query"))]
     "\n" [:p (str (request :query)) ] "\n"
    (when (-> request :params :query)
      [:div.results
       (do-search (-> request :params :config-id) (-> request :params :query)) ])]))


(defn index-logs
  [request]
  (let [{:keys [config-id date]} (request :params)
	target-date (chrono/parse-date date :compact-date)
	idx-agent (agent config-id)]
    (do
      (send-off idx-agent index-app-logs target-date)
      (with-std-template "LogSearch"
	[:h1 (str "Indexing for configuration " config-id " and date "
		  (chrono/format-date target-date :short-date)  " started")]))))


#_(index-file "c:\\sample_logs\\log\\production.20100120.app10042.log" "db10042" "tmp\\10042")

(decorate search-handler (with-logging))
(decorate search-form (with-logging))
(decorate index-logs (with-logging))

(defroutes search-routes
  (ANY "/search" search-form)
  (ANY "/idx/:config-id/:date" index-logs)
  (GET "/search/:config-id/:query" search-handler)
  (GET "/*" (or (serve-file (params :*)) :next))
  (ANY "/*" (do (println (str "Requested " (request :route-params))) 404)))

(defn -main [& args]
  (run-server {:port 6060}
	      "/*" (servlet search-routes)))


  