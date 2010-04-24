(ns clojars.web
  (:use compojure
        (clojars.web common login homepage user)))

(defn dashboard [x]
  (h (str x)))

(decorate-with require-login dashboard)

(defroutes clojars-routes
  (GET "/login" login)
  (POST "/login" login)
  (GET "/logout" logout)
  (GET "/register" register-form)
  (POST "/register" register)
  (GET "/profile" profile-form)
  (POST "/profile" update-profile)
  (GET "/dashboard" dashboard)
  (GET "/" homepage)
  (ANY "*" [404 "not found"]))

(decorate clojars-routes
  (with-session :memory))

;;---------------------------------------------------------
;; Auto-start server
;;---------------------------------------------------------

(declare server)

(when (.hasRoot #'server)
  (stop server))

(def server
     (run-server
      {:port 8000}
      "/*" (servlet clojars-routes)))


