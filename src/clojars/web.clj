(ns clojars.web
  (:use compojure
        [clojars.utils :only [servlet-url]]
        (clojars.web common login homepage user jar)))

(defn dashboard [x]
  (h (str x)))

(decorate-with require-login dashboard)

(defn serve-resource [root path]
  (when (safe-path? root path)
    (when-let [stream (.getResourceAsStream (clojure.lang.RT/baseLoader)
                                            (str root "/" path))]
      [{:headers {"Cache-Control" "max-age=3600"}} stream])))

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
  (GET #"/([a-zA-Z0-9._-]+)/([a-zA-Z0-9._-]+)" show-jar)
  (GET #"/([a-zA-Z0-9._-]+)" show-jar)
  (ANY "/*" (or (serve-resource "clojars/public" (params :*)) :next))
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

