(ns clojars.web.login
  (:import org.openid4java.consumer.ConsumerManager)
  (:require [clojure.contrib.str-utils2 :as str])
  (:use compojure
        net.cgrand.enlive-html
        clojars.web.common
        [clojars.utils :only [servlet-url repr]]
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
  [(attr= :name "redir")] (when redir (set-attr :value redir)))

(defn login-form [request & [error-message redir]]
  (render request "Login" login-template error-message redir))

(def openid-manager (ConsumerManager.))

(defn openid-discover [openid]
  (let [openid (if (str/contains? openid "://") openid (str "http://" openid))
        discoveries (.discover openid-manager openid)]
    (.associate openid-manager discoveries)))

(defn openid-authenticate [request openid redir]
  (let [qstring (when-not (blank? redir) (url-encode {"redir" redir}))]
   (if-let [discovered (openid-discover openid)]
     [(session-assoc :openid-discovered discovered)
      (-> (.authenticate openid-manager discovered
                         (servlet-url request (str "/login?" qstring)))
          (.getDestinationUrl true)
          (redirect-to))]
     (login-form request
                 (str "No OpenID endpoint found. "
                      "Ensure you typed your OpenID correctly.") redir))))

(defn full-request-url [servlet-request]
  (let [url (.getRequestURL servlet-request)
        query-string (.getQueryString servlet-request)]
    (if (blank? query-string)
      url
      (str url "?" query-string))))

(defn openid-verify [request redir]
  (let [discovered (-> request :session :openid-discovered)
        sreq (:servlet-request request)
        resp (org.openid4java.message.ParameterList. (.getParameterMap sreq))
        verification (.verify openid-manager (full-request-url sreq) resp
                              discovered)]
    (if-let [id (.getVerifiedId verification)]
      (if-let [user (user-by-openid (.getIdentifier id))]
        [(session-assoc :account (:username user))
         (redirect-to (or redir "/"))]
        (login-form request "No account matching this OpenID." redir))
      (login-form request "OpenID verification failed." redir))))

(defn login [request]
  (let [{:keys [username password redir]} (:params request)
        openid (-> request :params :openid_identifier)
        redir (or redir (-> request :query-params :redir))
        openid-mode (-> request :query-params :openid.mode)]
    (cond
     openid (openid-authenticate request openid redir)
     (= openid-mode "id_res")
     (openid-verify request redir)
     username
     (if-let [user (auth-user username password)]
       [(session-assoc :account (:username user))
        (redirect-to (or redir "/"))]
       (login-form request "Incorrect username or password." redir))
     :else
     (login-form request nil redir))))

(defn logout [request]
  (let [redir (:redir (:params request))]
    [(session-dissoc :account)
     (redirect-to (or redir "/"))]))
