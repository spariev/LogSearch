(ns com.spariev.models
  ;(:require )
  ;(:use )
  ;(:import )
  )

(defstruct rails-req
  :start-time :controller :method :remote-addr :format :http-method ; data from header (Processing ...)
  :duration :view-duration :db-duration :http-code :url) ; data from last request line (Completed ...)


