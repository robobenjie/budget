(ns budget.renderer
  (:use [hiccup core page]
        budget.manage-redis
   		[clj-time local core format]))

(defn comma-seperate [l]
  (if 
    (coll? l)
    (str 
     "["
     (reduce 
      #(str % ", " (comma-seperate %2))
      (comma-seperate (first l))
      (rest l))
     "]")
    l))

(defn target-data [time-obj total]
	[[1 5000] [30 0]])

(defn days-since-first [time-obj]
  (float (inc (/ (in-secs (interval 
          (first-of-the-month time-obj)
          time-obj))
     (* 60 60 24)))))

(defn plot-data [transactions total]
    (loop [trans transactions
       curr (. Integer parseInt total)
       retval [[(days-since-first (local-now)) total]]]
       (if (empty? trans)
         (conj retval [1 curr])
         (let [t (first trans)]
           (recur (rest trans) 
                  (+ curr (. Integer parseInt (:amount t)))
                  (conj retval 
                        [(days-since-first 
                          (parse 
                           (formatters :date-hour-minute-second)
                           (:time t)))
                         curr]))))))

(defn render-transaction-list [transactions]
  [:table.table
   (map 
    #(let [amount (. Integer parseInt (% :amount))
          display-time (unparse (formatters :date) (parse (formatters :date-hour-minute-second) (% :time)))]
      (vector :tr {:class (if (> 0 amount) "success" "")}
        [:td display-time] [:td (% :item)] [:td (. Math abs amount)]))
   transactions)])

(defn render-main [account-name]
  (let [[transactions total] (account-fetch account-name)]
    (println (plot-data transactions total))
    (html5
      [:head
        [:title (str "Savings for " account-name)]
        (include-css "/css/bootstrap.css")
        (include-css "/css/budget.css")
        (include-css "/css/bootstrap-responsive.css")
        [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0"}]]
      [:body
        [:div.container-fluid
          [:div.row-fluid
            [:div.span3
         	  [:div.well
                [:h1.center (str "$" total)]]
              [:div#graph]
              [:form {:method "post" :action "/update"}
               	[:input {:type "hidden" :value account-name :name "account", :id "account"}]
                [:fieldset
                 [:label "Item"]
				 [:input {:type "text", :placeholder "What are you buying", :name "item", :id "item"}]
                 [:label "Cost"]
                 [:div.input-prepend
                  [:span.add-on "$"]
                  [:input {:type "number" :placeholder "Round up to the nearest dollar", :name "amount", :id "amount"}]]
				 [:div.form-actions
                  [:input {:class "btn btn-primary" :type "submit"}]]]]
              (render-transaction-list transactions)
               ]]]
       [:script {:type "text/javascript"} (str "d1=" (comma-seperate (plot-data transactions total))";"
                                               "d2=" (comma-seperate (target-data :TODO total)) ";")]
       (include-js "/scripts/flotr2.min.js")
       (include-js "/scripts/make_plot.js")])))