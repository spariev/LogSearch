(ns spariev.logsearch.query-parser-test
  (:use clojure.contrib.test-is)
  (:use :reload-all spariev.logsearch.query-parser))

(deftest test-empty-string
  (is (empty? (parse-conditions ""))))

(deftest test-blank-string
  (is (empty? (parse-conditions "   "))))

(deftest test-simple-key-val
  (is (= {:field "ip-addr" :cond "=" :value "127.0.0.1"} (first (parse-conditions "ip-addr=127.0.0.1"))))
  (is (= {:field "ip-addr" :cond "!=" :value "127.0.0.1"} (first (parse-conditions "ip-addr!=127.0.0.1")))))

(deftest test-key-val-with-delimiters
  (is (= {:field "ip-addr" :cond "=" :value "127.0.0.1 333"} (first (parse-conditions "ip-addr='127.0.0.1 333'"))))
  (is (= {:field "ip-addr" :cond "=" :value "127.0.0.1 333"} (first (parse-conditions "ip-addr=\"127.0.0.1 333\""))))
  (is (= {:field "ip-addr" :cond "=" :value "127'0.0'1 333'"} (first (parse-conditions "ip-addr=\"127'0.0'1 333'\"")))))

(deftest test-invalid-cond
  (is (thrown? RuntimeException (parse-conditions "ip-addr<>127.0.0.1")))) ;; #"Parse error at column 7:(.*)" 


#_(run-tests)