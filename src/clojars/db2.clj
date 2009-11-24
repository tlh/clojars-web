(ns clojars.db2
  (:use [clojars :only [config]]
        [clojars.utils :only [sha1 gen-salt conj-when]]
        [clojure.contrib.str-utils2 :only [join]]
        [clojure.contrib.json.write :only [print-json]]
        [clojure.contrib.duck-streams :only [writer]]
        [com.ashafa.clutch :only [create-document create-view with-db
                                  with-clj-view-server ad-hoc-view
                                  delete-document get-document get-view
                                  update-document]]
        [com.reasonr.scriptjure :only (js)])
  (:import java.security.MessageDigest
           java.util.Date
           java.io.File
           java.text.SimpleDateFormat))

(def ssh-options (str "no-agent-forwarding,no-port-forwarding,"
                      "no-pty,no-X11-forwarding"))

(def *reserved-names*
     #{"clojure" "clojars" "clojar" "register" "login"
       "pages" "logout" "password" "username" "user"
       "repo" "repos" "jar" "jars" "about" "help" "doc"
       "docs" "pages" "images" "js" "css" "maven" "api"
       "download" "create" "new" "upload" "contact" "terms"
       "group" "groups" "browse" "status" "blog" "search"
       "email" "welcome" "devel" "development" "test" "testing"
       "prod" "production" "admin" "administrator" "root"
       "webmaster" "profile" "dashboard" "settings" "options"
       "index" "files" "uber" "uberjar" "standalone" "slim"
       "slimjar"})

(def *date-format* (SimpleDateFormat. "yyyy/MM/dd HH:mm:ss Z"))

(defmethod print-json Date [date]
  (print (str\" (.format *date-format* date) \")))

(defmacro create-clj-view
  "Shorthand for creating a clojure view."
  [design-doc view map-fn & reduce-fn]
  `(create-view
    ~design-doc ~view
    (with-clj-view-server ~map-fn ~@reduce-fn)))

;;
;; Users
;;

(declare write-key-file)

(defn find-user [username]
  (:value (first (:rows (get-view "users" :by-username
                                  {:key username :limit 1})))))

(defn all-users []
  (:rows (get-view "users" :all)))

(defn auth-user [username password]
  (when-let [user (find-user username)]
    (= (:password user) (sha1 (:salt user) password))))

(defn add-user [email username password ssh-key]
  (assert (not (find-user username)))
  (let [salt (gen-salt)]
    (create-document
     {:type :user
      :username username
      :email email
      :password (sha1 salt password)
      :salt salt
      :ssh-keys [ssh-key]
      :created (Date.)
      :groups [(str "org.clojars." username)]})))

(defn update-user [account email password ssh-key]
  (let [doc (find-user account)
        salt (gen-salt)]
    (assert doc)
    (update-document
     doc
     (-> {}
         (conj-when email {:email email})
         (conj-when password {:password (sha1 salt password), :salt salt})
         (conj-when ssh-key {:ssh-keys [ssh-key]}))))
  (write-key-file (:key-file config)))

(defn init-users-view []
  (when-let [doc (get-document "_design/users")]
    (delete-document doc))

  (create-clj-view
   "users" "all"
   (fn [doc]
     (when (= (doc :type) "user")
       [[nil doc]])))

  (create-clj-view
   "users" "by-username"
   (fn [doc]
     (when (= (doc :type) "user")
       [[(:username doc) doc]])))

  (create-clj-view
   "users" "by-group"
   (fn [doc]
     (when (= (doc :type) "user")
       (for [group (doc :groups)]
         [group doc])))))

(defn write-key-file [path]
  (locking (:key-file config)
    (let [new-file (File. (str path ".new"))]
      (with-open [f (writer new-file)]
        (doseq [doc (all-users)
                key (:ssh-keys doc)
                :when key]
          (.println f (str "command=\"ng --nailgun-port 8700 clojars.scp "
                           (:username doc)
                           "\"," ssh-options " "
                           (.replaceAll (.trim key) "[\n\r\0]" "")))))
      (.renameTo new-file (File. path)))))

;;
;; Groups
;;

(defn group-members [group]
  (map :value (:rows (get-view "users" :by-group {:key group}))))

(defn add-member [group username]
  (let [user (find-user username)]
    (assert user)
    (when-not (some #{group} (:groups user))
      (update-document user {:groups (conj (:groups user) group)}))))

(defn check-and-add-group [account group jar]
  (when-not (re-matches #"^[a-z0-9_.-]+$" group)
    (throw (Exception. (str "Group names must consist of lowercase "
                            "letters, numbers, hyphens, underscores "
                            "and full-stops."))))
  (let [members (group-members group)]
    (if (empty? members)
      (if (or (find-user group) (*reserved-names* group))
        (throw (Exception. (str "The group name " group " is already taken.")))
        (add-member group account))
      (when-not (some #{account} members)
        (throw (Exception. (str "You don't have access to the "
                                group " group.")))))))

;;
;; Jars
;;

(defn add-jar [account jarmap & [check-only]]
  (when-not (re-matches #"^[a-z0-9_.-]+$" (:name jarmap))
    (throw (Exception. (str "Jar names must consist of lowercase "
                            "letters, numbers, hyphens and underscores."))))

  (when-not (re-matches #"^[a-zA-Z0-9_.-]+$" (:version jarmap))
    (throw (Exception.
            (str "Versions must consist only of "
                 "letters, numbers, hyphens, underscores and dots."))))

  (check-and-add-group account (:group jarmap) (:name jarmap))

  (create-document
   (assoc jarmap
     :user account
     :created (Date.))))

(defn init-jars-view []
  (when-let [doc (get-document "_design/jars")]
    (delete-document doc))

  (create-clj-view
   "jars" "all"
   (fn [doc]
     (when (= (doc :type) "jar")
       [nil doc])))

  (create-clj-view
   "jars" "by-username"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:username doc) doc]])))

  (create-clj-view
   "jars" "by-group"
   (fn [doc]
     (when (= (doc :type) "jar")
       (for [group (doc :groups)]
         [group doc]))))

  (create-clj-view
   "jars" "by-created"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[created doc]]))))

(defn init-db []
  (init-users-view)
  (init-jars-view))

(comment

  (def db {:name "clojars-test"
           :language "clojure"})
  (with-db db
    (init-db)

    (find-user "atox")
    )
  (= [1 2 3] '(1 2 3))

  (update-document)

  (with-db db
    (create-document
     {:type "user"
      :username "atox"}))

  (ad-hoc-view
   (with-clj-view-server
     (fn [doc] [nil doc]))))
