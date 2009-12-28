(ns com.spariev.models
  ;(:require )
  (:use com.spariev.parse.rails)
  ;(:import )
  )

(defstruct rails-req
  :start-time :controller :method :remote-addr :format :http-method ; data from header (Processing ...)
  :duration :view-duration :db-duration :http-code :url
  :body) ; data from last request line (Completed ...)

(defn create-rails-req [log-chunk]
  (let [proc-line (first log-chunk)
        proc-line-data (parse-processed-line proc-line)
        comp-line (first (filter #( >= (.indexOf % "Completed on") 0) (rest log-chunk)))
        comp-line-data (if comp-line (parse-completed-line comp-line) {})]
    (merge proc-line-data comp-line-data)))


