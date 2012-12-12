(ns budget.renderer
  (:use [hiccup core page]
        budget.manage-redis
        budget.constants
       	budget.charts
   		[clj-time local core format]))

(defn render-transaction-list [transactions]
  [:table.table
   (map 
    #(let [amount (. Integer parseInt (% :amount))
          display-time (unparse (formatters :date) (parse (formatters :date-hour-minute-second) (% :time)))]
      (vector :tr {:class (if (> 0 amount) "success" "")}
        [:td display-time] [:td (% :item)] [:td (. Math abs amount)]))
   transactions)])

(time (let [[transactions total] (account-fetch "benjie")]
        (render-chart "Extra savings" transactions total plot-budget-left
							(fn[x y] (- y (- monthly-budget (* x monthly-budget 0.033333)))))
        (render-chart "Spent" transactions total plot-total-spent
							(fn[x y] y))
        (render-chart "Money left" transactions total plot-budget-left
                            (fn[x y] y)
                            (fn[x y] (- monthly-budget (* x monthly-budget 0.033333))))
        "foo"

        ))

(defn render-main [account-name]
  (let [[transactions total] (account-fetch account-name)]
    (html5
      [:head
       	[:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
        [:title (str "Savings for Benjie and Stephanie")]
        (include-css "/css/swipe.css")
        (include-css "/css/bootstrap.css")
        (include-css "/css/budget.css")
        (include-css "/css/bootstrap-responsive.css")
        (include-js "/scripts/flotr2.min.js")
       (include-js "/scripts/swipe.min.js")
        [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0"}]]
      [:body
        [:div.container-fluid
          [:div.row-fluid
            [:div.span3
         	  [:div.well
                [:h1.center (str "$" total)]]
              [:div#chart-slider 
               [:ul
                [:li 
                   (render-chart "Extra savings" transactions total plot-budget-left
							(fn[x y] (- y (- monthly-budget (* x monthly-budget 0.033333)))))]
                [:li 
                   (render-chart "Spent" transactions total plot-total-spent
							(fn[x y] y))]
                [:li 
                   (render-chart "Money left" transactions total plot-budget-left
                            (fn[x y] y)
                            (fn[x y] (- monthly-budget (* x monthly-budget 0.033333))))]]]
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
              [:script {:type "text/javascript"} 
                "new Swipe(document.getElementById('chart-slider'));"]
               ]]]]
       )))

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
