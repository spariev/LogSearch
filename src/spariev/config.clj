(ns spariev.config
  (:use spariev.logsearch.db))


(def *log-src*
     { :localtest {:server-id :dev
		   :app-id :lucene-idx
		   :path "c:\\sample_logs\\log"
		   :remote {:addr "localhost"
			      :format "default"}}
       :applogs    {:server-id :test
		    :app-id :tm_admin_prod
		    :path "c:\\sample_logs\\log"
		    :remote {:addr "app.home"
			     :format "default"}}})

(defn get-config
  [config-id]
  (*log-src* (keyword config-id)))

(defn get-config-val
  [config-id key]
  ((get-config config-id) (keyword key) ))

(defn idx-path-for-config
  [config-id]
  (let [{:keys [server-id app-id]} (get-config config-id)]
    (str "tmp" path-sep (name server-id) path-sep (name app-id) path-sep "idx" )))

(defn db-name-for-config
  [config-id]
  (name ((get-config config-id) :server-id)))
