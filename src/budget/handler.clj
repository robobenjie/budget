(ns budget.handler
  (:use compojure.core
        budget.manage-redis
        budget.renderer)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [] (render-main "benjie"))
  (POST "/update" {params :params}
	(process-transaction "benjie" params)
	(render-main "benjie"))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
