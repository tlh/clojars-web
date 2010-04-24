(ns clojars.web.login
  (:use compojure
        net.cgrand.enlive-html
        clojars.web.common
        clojars.user))

(defn request-path [request]
  (let [sr (:servlet-request request)
        query (.getQueryString sr)
        request (.getRequestURI sr)]
    (if query
      (str request "?" query)
      request)))

(defn require-login [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (redirect-to (str "/login?redir=" (url-encode (request-path request)))))))

(defsnippet login-template
  "clojars/web/login.html" [:article]
  [error redir]
  [:.error] (when error (content error))
  [:#login-form (attr= :name "redir")] (when redir (set-attr :value redir)))

(defn login-form [request & [error-message redir]]
  (render request "Login" login-template error-message redir))

(defn login [request]
  (let [{:keys [username password redir]} (:params request)]
    (if username
     (if-let [user (auth-user username password)]
       [(session-assoc :account (:username user))
        (redirect-to (or redir "/"))]
       (login-form request "Incorrect username or password." redir))
     (login-form request (str request) redir))))

(defn logout [request]
  (let [redir (:redir (:params request))]
    [(session-dissoc :account)
     (redirect-to (or redir "/"))]))
