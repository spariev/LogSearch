(ns spariev.config)

(def *db-name* "logsearch_dev")
(def *index-filename* "tmp/dev/lucene-idx")

;
(def *log-src*
     [
      {:server-id "local_test" :addr "localhost"
       :apps {:tm_admin_prod {:path "c:\\sample_logs\\log"
			      :format "default"}}}
;      {:server-id "tm_admin_prod" :addr "admin.telemarker.home"
;      :apps {:tm_admin_prod {:path "/rest/u/apps/tm-admin-production/current"
;			     :format "default"}}}
      ]
     )

