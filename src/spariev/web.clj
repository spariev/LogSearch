(ns spariev.web
  (:use spariev.config
	spariev.logsearch.lucene
	spariev.logsearch.fileparser
	spariev.logsearch.db
	spariev.logsearch.util
	[spariev.chrono :as chrono]
	compojure
	somnium.congomongo)
  (:import (java.net URLEncoder))
  (:require [cupboard.core :as cb])
  (:gen-class))

#_(set! *warn-on-reflection* true)

(defn with-logging
 [handler]
  (fn [request]
    (do
      (println)
      (println (str "Processing " (request :uri)
		    " (for " (request :remote-addr) " at "
		    (chrono/format-date (chrono/now) :db-date-time)
		    ") [" (request :request-method) "]"))
      (println (str "  Params: " (request :params)))
      (println)
      (time (handler request)))))


(defn format-search-hit
  [config-id query search-result]
  (let [{:keys [id header]}  search-result]
    [:div
     [:p header
      [:a {:class "show-more-link" :href "#"
	   :rel (str "/event/" config-id "/" id "?query="
		     (URLEncoder/encode query "UTF-8") )} "Show ..."]]
     [:p {:class "detailed-container hidden"}]]))

(defn search-hit
  "return html for highlighted body of specified request"
  [request]
  (let [{:keys [config-id query obj-id]} (request :params)
	body-content (cb/with-open-cupboard
		       [cb-db (db-path-for-config config-id)]
		       (load-log-entry cb-db obj-id))
	highlighted-body (highlight body-content query)]
    (html [:pre highlighted-body])))


(defn do-search
  [config-id query]
  (let [search-results (bench "Lucene search "
			      (search (idx-path-for-config config-id) query))]
    (println "results" search-results)
    (bench
     "Rendering "
     (html
      [:div.results-count [:p (str (count search-results) " entries found")]]
      (doall
       (map (partial format-search-hit config-id query)
	    search-results))))))

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
     [:script {:type "text/javascript" :src "/jquery-1.4.1.min.js"}]
     [:script {:type "text/javascript" :src "/search.js"}]
     [:body "\n"
      [:div.header
       [:h2 page-title]] "\n"
      [:div.body
       body]]]))

(defn search-form
  [request]
  (let [config-id (-> request :params :config-id)
	query (-> request :params :query)]
    (with-std-template "LogSearch"
      [:div [:div.search
	     (form-to
	      [:post "/search"]
	      [:p "config-id "
	       (text-field {:size 10 :value (or config-id "tmadmin")}
			   :config-id)]
	       [:p "query " (text-field {:size 50  :value (or query "") }
					:query)]
	       (submit-button "Query"))]
     "\n" [:p query ] "\n"
    (when query
      [:div.results
       (do-search config-id query) ])])))


(defn index-logs
  [request]
  (let [{:keys [config-id date]} (request :params)
	target-date (chrono/parse-date date :compact-date)
	idx-agent (agent config-id)]
    (do
      (send-off idx-agent index-app-logs date)
      (with-std-template "LogSearch"
	[:h1 (str "Indexing for configuration " config-id " and date "
		  (chrono/format-date target-date :short-date)  " started")]))))


#_(index-file "c:\\sample_logs\\log\\production.20100120.app10042.log" "db10042" "tmp\\10042")

(decorate search-handler (with-logging))
(decorate search-form (with-logging))
(decorate index-logs (with-logging))
(decorate search-hit (with-logging))

(defroutes search-routes
  (ANY "/search" search-form)
  (ANY "/idx/:config-id/:date" index-logs)
  (ANY "/event/:config-id/:obj-id" search-hit)
  (GET "/search/:config-id/:query" search-handler)
  (GET "/*" (or (serve-file (params :*)) :next))
  (ANY "/*" (do (println (str "Requested " (request :route-params))) 404)))

(defn -main [& args]
  (run-server {:port 6060}
	      "/*" (servlet search-routes)))