(defproject budget "0.1.0-SNAPSHOT"
  :description "site to keep track of monthly budget"
  :url "http://thegoodspender.com"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.taoensso/carmine "1.0.0"]
                 [hiccup "1.0.1"]
                 [clj-time "0.4.4"]
                 [ring "1.1.1"]
                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
                 [compojure "1.1.1"]]
  :plugins [[lein-ring "0.7.3"]]
  :ring {:handler budget.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
