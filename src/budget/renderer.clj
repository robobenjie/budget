(ns budget.renderer
  (:use [hiccup core page]
        budget.manage-redis
        budget.constants
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
	[[1 monthly-budget] [30 0]])

(defn days-since-first [time-obj]
  (float (inc (/ (in-secs (interval 
          (first-of-the-month time-obj)
          time-obj))
     (* 60 60 24)))))

(defn plot-data [transactions total function]
    (loop [trans transactions
       curr (. Integer parseInt total)
       retval [[(days-since-first (local-now)) total]]]
       (if (empty? trans)
         (conj retval [1 curr])
         (let [t (first trans)]
           (recur (rest trans) 
                  (+ curr (. Integer parseInt (:amount t)))
                  (let [x (days-since-first 
                            (parse 
                             (formatters :date-hour-minute-second)
                             (:time t)))]
                    (conj retval [x (function x curr)])))))))

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
    (html5
      [:head
        [:title (str "Savings for Benjie and Stephanie")]
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
       [:script {:type "text/javascript"} 
        (str "d1=" (comma-seperate (plot-data transactions total (fn[x y] y)))";"
             "d2=" (comma-seperate (target-data :TODO total)) ";"
             "savings" (comma-seperate (plot-data transactions total (fn[x y] (+ y -5000 (* x 5000 (/ 1 30)))))) ";"
             )]
       (include-js "/scripts/flotr2.min.js")
       (include-js "/scripts/make_plot.js")])))
(/ 1 30)
(defn render-message [message-markup wait-time]
  (html5
    [:head
     [:meta {:HTTP-EQUIV "REFRESH" :content (str wait-time "; url=/")}]
     [:title (str "Savings for Benjie and Stephanie")]
     (include-css "/css/bootstrap.css")
     (include-css "/css/budget.css")
     (include-css "/css/bootstrap-responsive.css")
     [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0"}]]
   [:body 
    [:div.container-fluid
          [:div.row-fluid
            [:div.span3
         	  [:div.well
                [:h1.center message-markup]]]]]]))
(defn random-affirmation []
  (rand-nth ["OK" "Great!" "Nice" "Good choice" "I like it!" "Sweet" "Awesome" "Rock on" "Thanks" ]))
