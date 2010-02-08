(ns spariev.logsearch.util)

(defmacro bench
  [msg expr]
  `(let [message# ~msg
	 start# (. System (nanoTime))
	 ret# ~expr]
     (println (str message# (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))
