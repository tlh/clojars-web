(ns clojars.web.user
  (:use compojure
        net.cgrand.enlive-html
        clojars.web.common
        clojars.utils
        clojars.user
        clojars.group)
  (:require [clojure.contrib.str-utils2 :as str]))

(defsnippet register-template
  "clojars/web/register.html" [:article]
  [error-list]
  [:.error] (when (seq error-list) identity)
  [:.error :li] #(for [error error-list] (at % [:li] (content error))))

(defsnippet profile-template
  "clojars/web/profile.html" [:article]
  [error-list profile]
  [:.error] (when (seq error-list) identity)
  [:.error :li] #(for [error error-list] (at % [:li] (content error)))
  [:.profile (attr= :name "email")] (set-attr :value (:email profile))
  [:.profile (attr= :name "ssh-keys")] (content (str/join
                                                 "\n" (:ssh-keys profile))))

(defn register-form [request & [error-message]]
  (render request "Register" register-template error-message))

(defn profile-form [request & [error-message]]
  (let [username (:account (:session request))
        profile (find-user username)]
    (render request "Profile" profile-template error-message profile)))

(defn valid-ssh-key? [key]
  (re-matches #"(ssh-\w+ \S+|\d+ \d+ \D+).*\s*" key))

(defn validate-profile
  "Validates a profile, returning nil if it's okay, otherwise a list
  of errors."
  [profile]
  (let [{:keys [account email username password confirm ssh-keys]} profile]
    (-> nil
        (conj-when (and (nil? account) (blank? email)) "Email can't be blank")
        (conj-when (neg? (.indexOf email "@")) "Email is invalid")
        (conj-when (blank? username) "Username can't be blank")
        (conj-when (and (nil? account) (blank? password))
                   "Password can't be blank")
        (conj-when (and (seq password) (not= password confirm))
                   "Password and confirm password must match")
        (conj-when (or (*reserved-names* username)
                       (and (not= account username)
                            (find-user username))
                       (find-group username))
                   "Username is already taken")
        (conj-when (not (re-matches #"[a-z0-9_-]+" username))
                   (str "Usernames must consist only of lowercase "
                        "letters, numbers, hyphens and underscores."))
        (conj-when (some #(not (valid-ssh-key? %)) ssh-keys)
                   "Invalid SSH public key"))))

(defn split-keys [s]
  (vec (remove empty? (map #(.trim %) (.split s "\n")))))

(defn register [request]
  (let [profile (select-keys (:params request) [:username :email :password
                                                :confirm :ssh-keys])
        profile (assoc profile :ssh-keys (split-keys (:ssh-keys profile)))
        errors (validate-profile profile)]
    (if errors
      (register-form request errors)
      (do
        (add-user (select-keys profile [:username :email :password :ssh-keys]))
        [(session-assoc :account (:username profile))
         (redirect-to "/dashboard")]))))

(defn update-profile [request]
  (let [user (find-user (:account (:session request)))
        profile (select-keys (:params request) [:email :password
                                                :confirm :ssh-keys])
        profile (assoc profile
                  :account (:username user)
                  :username (:username user)
                  :ssh-keys (split-keys (:ssh-keys profile)))
        errors (validate-profile profile)]
    (if errors
      (profile-form request errors)
      (do
        (update-user (merge user (select-keys profile [:email :password
                                                       :ssh-keys])))
        (redirect-to "/profile")))))