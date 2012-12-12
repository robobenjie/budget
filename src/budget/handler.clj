(ns budget.handler
  (:use compojure.core
        budget.manage-redis
        budget.renderer)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (render-main "benjie"))
  (POST "/update" {params :params}
    (let [error (process-transaction "benjie" params)]
      (if error (render-message error 5)
	    (render-message (random-affirmation) 1))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))