(comment
Sample clojure source file
)
(ns spariev.logsearch
    (:gen-class))

(defn -main
    ([greetee]
  (println (str "Hello " greetee "!")))
  ([] (-main "world")))
