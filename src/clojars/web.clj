(ns clojars.web
  (:require [clojure.contrib.sql :as sql])
  (:use [clojars :only [config]]
        [clojars.db :only [with-db]]
        (clojars.db users groups utils jars)
        [clojars.web dashboard group jar login search user browse common]
        [compojure])
  (:import java.io.File))

(defn not-found-doc []
  (html [:h1 "Page not found"]
        [:p "Thundering typhoons!  I think we lost it.  Sorry!"]))

(defmacro with-account [body]
  `(if-let [~'account (~'session :account)]
     (do ~body)
     (redirect-to "/login")))

(defmacro param [kw]
  `((:route-params ~'request) ~kw))

(defmacro try-account [body]
  `(let [~'account (~'session :account)]
     (do ~body)))

(defn db-middleware [handler]
  (fn [request]
    (with-db (handler request))))

(defn find-public-dir
  "Searches up the directory hierachy looking for the 'public' directory
  containing static files.  This allows us to run from within a subdirectory of
  the project, simplifying testing with SLIME and deployment."
  []
  (loop [path (File. (System/getProperty "user.dir"))]
    (if path
     (let [public (File. path "public")]
       (if (.isDirectory public)
         (str public)
         (recur (.getParentFile path))))
     "public")))

(def public-dir (find-public-dir))

;; TODO: move all the logic from these into the handler functions.
;;       we don't need to do it here as the handlers can return :next
(defroutes clojars-app
  (GET "/search"
    (try-account
     (search account (params :q))))
  (GET "/browse"
    (try-account
     (browse account)))
  (GET "/browse/:tag"
    (try-account
     (browse account (params :tag))))
  (GET "/profile"
    (with-account
     (profile-form account)))
  (POST "/profile"
    (with-account
      (update-profile account params)))
  (GET "/login"
    (login-form))
  (POST "/login"
    (login params))
  (POST "/register"
    (register params))
  (GET "/register"
    (register-form))
  (GET "/logout"
    [(session-assoc :account nil)
     (redirect-to "/")])
  (GET "/"
    (try-account
     (if account
       (dashboard account)
       (index-page account))))
  (GET #"/groups/([^/]+)"
    (let [group ((:route-params request) 0)]
     (if-let [members (with-db (group-members group))]
       (try-account
        (show-group account group members))
       :next)))
  (POST #"/groups/([^/]+)"
    (let [group ((:route-params request) 0)]
     (if-let [members (with-db (group-members group))]
       (try-account
        (cond
          (some #{(params :user)} members)
          (show-group account group members "They're already a member!")
          (and (some #{account} members)
               (find-user (params :user)))
          (do (add-member group (params :user))
              (show-group account group
                          (conj members (params :user))))
          :else
          (show-group account group members (str "No such user: "
                                                 (h (params :user))))))
       :next)))
  (GET "/users/:username"
    (if-let [user (with-db (find-user (param :username)))]
      (try-account
       (show-user account user))
      :next))
  (GET "/:jarname"
    (if-let [jar (with-db (find-canon-jar (param :jarname)))]
      (try-account
       (show-jar account jar))
      :next))
  (GET #"/([^/]+)/([^/]+)"
    (if-let [jar (with-db (find-jar (param 0) (param 1)))]
      (try-account
       (show-jar account jar))
      :next))
  (GET "/:user"
    (if-let [user (with-db (find-user (param :user)))]
      (try-account
       (show-user account (:user user)))
      :next))
  (ANY "/*"
       (if-let [f (serve-file public-dir (params :*))]
         [{:headers {"Cache-Control" "max-age=3600"}} f]
         :next))
  (ANY "*"
    [404 (html-doc (session :account) "Page not found" (not-found-doc))]))

(decorate clojars-app
          (with-session)
          (db-middleware))

(comment
  (def server (run-server {:port 8000} "/*" (servlet clojars-app)))
  (.stop server)
  (with-db (find-jar "hello" "world"))
  )