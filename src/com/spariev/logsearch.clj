(comment
Sample clojure source file
)
(ns com.spariev.logsearch
    (:gen-class))

(defn -main
    ([greetee]
  (println (str "Hello " greetee "!")))
  ([] (-main "world")))
