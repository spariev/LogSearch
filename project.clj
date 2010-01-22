(defproject LogSearch "0.1.0"
	  :description "LogSearch"
	  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
			 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
			 [org.apache.lucene/lucene-core "3.0.0"]
			 [compojure "0.3.2"]
			 [org.apache.lucene/lucene-highlighter "3.0.0"]
			 [org.clojars.somnium/congomongo "0.1.1-SNAPSHOT"]
			 ]
	  :dev-dependencies [[leiningen/lein-swank "1.1.0"]]
	  :main clj_renderer)