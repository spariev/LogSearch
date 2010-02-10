(defproject LogSearch "0.1.0"
	  :description "LogSearch"
	  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
			 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
			 [org.apache.lucene/lucene-core "3.0.0"]
			 [compojure "0.3.2"]
			 [joda-time/joda-time "1.6"]
			 [org.apache.lucene/lucene-highlighter "3.0.0"]
			 [org.clojars.sanityinc/congomongo "0.1.1-SNAPSHOT"]
			 #_[org.clojars.somnium/congomongo "0.1.1-SNAPSHOT"]
			 [spariev/chrono "1.0.0-SNAPSHOT"]
			 ]
	  :dev-dependencies [[leiningen/lein-swank "1.1.0"]]
	  :main spariev.web)