(ns spariev.config
  (:use spariev.logsearch.db))

#_(set! *warn-on-reflection* true)

(def *log-src*
     {:localtest {:server-id :dev
		  :app-id :lucene-idx
		  :path "c:\\sample_logs\\log"
		  :remote {:addr "localhost"
			    :format "default"}}
      :applogs   {:server-id :test
		  :app-id :tm_admin_prod
		  :path "c:\\sample_logs\\log"
		  :remote {:addr "app.home"
			   :format "default"}}
      :tmlite   {:server-id :tmadmin
		  :app-id :tmadmin
		  :path "/home/spariev/tmp/tmlogs_lite"
		  :remote {:addr "tmadmin.home"
			   :format "default"}}
      :tmadmin   {:server-id :tmadmin
		  :app-id :tmadmin
		  :path "/home/spariev/tmp/tmlogs"
		  :remote {:addr "tmadmin.home"
			   :format "default"}}

      })

(defn get-config
  [config-id]
  (*log-src* (keyword config-id)))

(defn get-config-val
  [config-id key]
  ((get-config config-id) (keyword key)))

(defn idx-path-for-config
  [config-id]
  (let [{:keys [server-id app-id]} (get-config config-id)]
    (str "tmp" path-sep (name server-id) path-sep (name app-id) path-sep "idx" )))

(defn db-name-for-config*
  [config-id]
  (keyword ((get-config config-id) :server-id)))

(def db-name-for-config (memoize db-name-for-config*))

(defn fs-name-for-config*
  [config-id]
  (keyword (str (name (db-name-for-config config-id)) "fs")))

(def fs-name-for-config (memoize fs-name-for-config*))
