(ns budget.manage-redis
  (:require [taoensso.carmine :as car])
  (:use [clj-time format local]
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

(defn try-signup [params]
  (try 
    (let [budget (. Integer parseInt (:budget params))
          username (:username params)]
      (if
        (= (wcar (car/sismember "::usernames" (:username params))) 1)
        ["That username is taken" false]
        (do
          (wcar
             (car/set (str ":monthly-budget:" username) budget)
             (car/set (str ":password:" username) (:password params))
             (car/sadd "::usernames" username))
           [(str "Welcome " username) true])))
    (catch Exception e ["Bad number in budget field" false])))


(defn try-login [params]
  (let [[has-user fetched-password]
        (wcar
          (car/sismember "::usernames" (:username params))
          (car/get (str ":password:" (:username params))))]
    (if (= has-user 0)
        [(str "No such user: " (:username params)) nil]
        (if (= (:password params) fetched-password)
           ["success" (:username params)]
           ["incorrect password" nil]))))

(defn process-transaction 
  ([data] (process-transaction data (local-now)))
  ([data time-obj]
   (let [account-name (:account data)]
     (try (. Integer parseInt (:amount data)) 
       (wcar
         (car/decrby (str ":total:" account-name) (:amount data))
         (car/lpush  (transaction-key account-name (unparse dateformat (local-now))) 
                 (assoc data :time (unparse (formatters :date-hour-minute-second) time-obj))))
       nil
       (catch Exception e "I didn't understand that number format. <a href=\"/\"> Go Back </a>")))))

(defn account-fetch 
  ([account-name] 
   (let [[transactions amount & rest :as result] (account-fetch account-name (unparse dateformat (local-now)))]
     (if (< 0 (count transactions))
	       result
       (do 
         (println "filling month")
         (let [monthly-budget (wcar (car/get (str ":monthly-budget:" account-name)))]
           (process-transaction          
             {:amount (str "-" monthly-budget), :name "Monthly refresh" :account account-name}
             (first-of-the-month (local-now)))
            (account-fetch account-name))))))
  ([account-name month]
    (let [[transactions total account-string monthly-budget]
           (wcar
             (car/lrange (transaction-key account-name month) 0 6000)
             (car/get (str ":total:" account-name))
             (car/get (str ":account-string:" account-name))
             (car/get (str ":monthly-budget:" account-name)))]
      (vector transactions total account-string (. Integer parseInt monthly-budget)))))
