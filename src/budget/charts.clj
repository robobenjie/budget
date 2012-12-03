(ns budget.charts
  (:use budget.constants
        budget.manage-redis
        [clj-time core format local]))

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



(defn plot-data [transactions total f]
    (loop [trans transactions
       curr (. Integer parseInt total)
       retval [[(days-since-first (local-now)) total]]]
       (if (empty? trans)
        retval
         (let [t (first trans)
               curr-total (+ curr (. Integer parseInt (:amount t)))]
           (recur (rest trans) 
				  curr-total
                  (let [x (days-since-first 
                            (parse 
                             (formatters :date-hour-minute-second)
                             (:time t)))]
                    (conj retval (vector x (f x curr-total)))))))))

;(render-chart "test" trans "8000" (fn[x y] y))

(def chart-options 
  "{
    xaxis: {tickDecimals: 0, autoscaleMargin: 20},
    yaxis: {tickDecimals: 0, min: 0}
   }") 

(defn render-chart [chart-name transactions total & functions]
  [:div
    [:h6.center chart-name]
    [:div.chart {:id chart-name}]
    [:script {:type "text/javascript"} 
    	(str "Flotr.draw(document.getElementById('" chart-name "'),"
		  (comma-seperate
            (map #(plot-data transactions total %) functions))
          ","
	      chart-options
          ");")]])
