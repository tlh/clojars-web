(ns clojars.web.user
  (:use (clojars.db users groups jars)
        clojars.web.common
        clojars.utils
        [clojure.contrib.str-utils2 :only [join]]
        compojure))

(defn register-form [ & [errors email user ssh-key]]
  (html-doc nil "Register"
            [:h1 "Register"]
            (error-list errors)
            (form-to [:post "/register"]
                     (label :email "Email:")
                     [:input {:type :email :name :email :id
                              :email :value email}]
                     (label :user "Username:")
                     (text-field :user user)
                     (label :password "Password:")
                     (password-field :password)
                     (label :confirm "Confirm password:")
                     (password-field :confirm)
                     (label :ssh-key "SSH public key:")
                     " (" (link-to
                           "http://wiki.github.com/ato/clojars-web/ssh-keys"
                           "what's this?") ")"
                           (text-area :ssh-key ssh-key)
                           (submit-button "Register"))))

(defn valid-ssh-key? [key]
  (re-matches #"(ssh-\w+ \S+|\d+ \d+ \D+).*\s*" key))

(defn validate-profile
  "Validates a profile, returning nil if it's okay, otherwise a list
  of errors."
  [account email user password confirm ssh-keys]
  (-> nil
      (conj-when (and (nil? account) (blank? email)) "Email can't be blank")
      (conj-when (neg? (.indexOf email "@")) "Email is invalid")
      (conj-when (blank? user) "Username can't be blank")
      (conj-when (and (nil? account) (blank? password))
                 "Password can't be blank")
      (conj-when (and (seq password) (not= password confirm))
                 "Password and confirm password must match")
      (conj-when (or (*reserved-names* user) ; "I told them we already
                     (and (not= account user) ; got one!"
                          (find-user user))
                     (seq (group-members user)))
                 "Username is already taken")
      (conj-when (not (re-matches #"[a-z0-9_-]+" user))
                 (str "Usernames must consist only of lowercase "
                      "letters, numbers, hyphens and underscores."))
      (conj-when (some #(not (valid-ssh-key? %)) ssh-keys)
                 "Invalid SSH public key")))

(defn register [{email :email, user :user, password :password
                 confirm :confirm, ssh-key :ssh-key}]
  (let [ssh-keys (remove empty? (map #(.trim %) (.split ssh-key "\n")))]
   (if-let [errors (validate-profile nil email user password confirm ssh-keys)]
     (register-form errors email user ssh-key)
     (do (add-user email user password ssh-keys)
         [(session-assoc :account user)
          (redirect-to "/")]))))

(defn profile-form [account & [errors]]
  (let [user (find-user account)]
    (html-doc account "Profile"
              [:h1 "Profile"]
              (error-list errors)
              (form-to [:post "/profile"]
                       (label :email "Email:")
                       [:input {:type :email :name :email :id
                                :email :value (user :email)}]
                       (label :password "Password:")
                       (password-field :password)
                       (label :confirm "Confirm password:")
                       (password-field :confirm)
                       (label :ssh-key "SSH public key(s):")
                       " ("
                       (link-to
                        "http://wiki.github.com/ato/clojars-web/ssh-keys"
                        "what's this?") ")"
                       [:textarea {:name :ssh-key, :wrap :off}
                        (h (join "\n" (user :ssh-keys)))]
                       (submit-button "Update")))))

(defn update-profile [account {email :email, password :password
                               confirm :confirm, ssh-key :ssh-key}]
  (let [ssh-keys (remove empty? (map #(.trim %) (.split ssh-key "\n")))]
   (if-let [errors (validate-profile account email
                                     account password confirm ssh-keys)]
     (profile-form account errors)
     (do (update-user account email password ssh-keys)
         [(redirect-to "/profile")]))))

(defn show-user [account user]
  (html-doc account (h (user :username))
    [:h1 (h (user :username))]
    [:h2 "Jars"]
    (unordered-list (map jar-link (jars-by-user (user :username))))
    [:h2 "Groups"]
    (unordered-list (map group-link (user :groups)))))
