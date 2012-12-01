(ns budget.manage-redis
  (:require [taoensso.carmine :as car])
  (use [clj-time format local]
       budget.constants))

(def dateformat (formatter "yyyyMM"))

(def pool         (car/make-conn-pool))
(def spec-server1 (car/make-conn-spec))

(defn now-time [] (local-now))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))

(defn transaction-key [account-name month]
  (str ":transactions:" month ":" account-name))

(defn first-of-the-month [time-obj]
  (parse (formatter "YYYYMM") (unparse (formatter "YYYYMM") time-obj)))

(defn process-transaction 
  ([account-name data] (process-transaction account-name data (local-now)))
  ([account-name data time-obj]
   (try (. Integer parseInt (:amount data)) 
    (wcar
     (car/decrby (str ":total:" account-name) (:amount data))
     (car/lpush  (transaction-key account-name (unparse dateformat (local-now))) 
                 (assoc data :time (unparse (formatters :date-hour-minute-second) time-obj))))
     nil
    (catch Exception e "I didn't understand that number format. <a href=\"/\"> Go Back </a>"))))
(defn account-fetch 
  ([account-name] 
   (let [[transactions amount] (account-fetch account-name (unparse dateformat (local-now)))]
     (if (< 0 (count transactions))
	       [transactions amount]
       (do 
         (println "filling month")
         (process-transaction 
           account-name 
           {:amount (str "-" monthly-budget), :name "Monthly refresh" :account account-name}
           (first-of-the-month (local-now)))
          (account-fetch account-name)))))
  ([account-name month]
    (wcar
      (car/lrange (transaction-key account-name month) 0 5000)
      (car/get (str ":total:" account-name)))))
