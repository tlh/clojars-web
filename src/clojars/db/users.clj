(ns clojars.db.users
  (:use [clojars :only [config]]
        clojars.utils
        clojars.db.utils
        [com.ashafa.clutch :exclude [config]]
        [clojure.contrib.duck-streams :only [writer]])
  (:import java.util.Date
           java.io.File))

(declare write-key-file)

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

(defn find-user [username]
  (:value (first (:rows (get-view "users" :by-username
                                  {:key username :limit 1})))))

(defn users-by-email [email]
  (map second (view-seq "users" :by-email {:key email})))

(defn all-users []
  (map :value (:rows (get-view "users" :all))))

(defn auth-user [username password]
  (if-let [user (find-user username)]
    (when (= (:password user) (sha1 (:salt user) password))
      user)
    (first (filter #(= (:password %) (sha1 (:salt %) password))
                   (users-by-email username)))))

(defn add-user [email username password ssh-key]
  (assert (not (find-user username)))
  (let [salt (gen-salt)]
    (create-document
     {:type :user
      :username username
      :email email
      :password (sha1 salt password)
      :salt salt
      :ssh-keys ssh-key
      :created (Date.)
      :groups [(str "org.clojars." username)]})))

(defn update-user [account email password ssh-keys]
  (let [doc (find-user account)
        salt (gen-salt)]
    (assert doc)
    (update-document
     doc
     (-> {}
         (conj-when (seq email) {:email email})
         (conj-when (seq password) {:password (sha1 salt password), :salt salt})
         (conj-when ssh-keys {:ssh-keys ssh-keys}))))
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
   "users" "by-email"
   (fn [doc]
     (when (= (doc :type) "user")
       [[(:email doc) doc]])))

  (create-clj-view
   "users" "by-group"
   (fn [doc]
     (when (= (doc :type) "user")
       (for [group (doc :groups)]
         [group (:username doc)])))))

(defn write-key-file [path]
  (assert path)
  (locking path
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