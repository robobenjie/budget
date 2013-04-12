(ns budget.handler
  (:use compojure.core
        budget.manage-redis
        budget.renderer
	(sandbar stateful-session))
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" [month] 
    (if (session-get :username)
       (render-main (session-get :username) month)
       (render-login)))
  (POST "/signup" {params :params}
    (let [[message success] (try-signup params)]
      (render-message message 2)))
  (POST "/login" {params :params}
    (let [[message username]
          (try-login params)]
       (if username
         (session-put! :username username))
         (render-message message 2)))
  (GET "/logout" []
     (session-delete-key! :username)
     (render-message "logged out" 3))
  (POST "/update" {params :params}
    (let [error (process-transaction params)]
      (if error (render-message error 5)
	    (render-message (random-affirmation) 1))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
     wrap-stateful-session))