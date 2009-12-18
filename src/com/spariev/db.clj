(ns com.spariev.db
  (:use somnium.congomongo))

(defn save-req [req-id req-lines]
  (insert! :railslogs
    {:reqid req-id
     :hdr (first req-lines)
     :body (rest req-lines)}))

