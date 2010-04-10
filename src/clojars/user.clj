(ns clojars.user
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.duck-streams :only [reader writer]]
        clojars.utils))

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

(def date-format
     (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")
       (.setTimeZone (java.util.TimeZone/getTimeZone "UTC"))))

(def users-dir (file "/home/clojars/data/users"))

(defn- valid-username? [#^String username]
  (re-matches #"[a-z0-9_-]+" username))

(defn- validate-username [username]
  (when (not (valid-username? username))
    (throw (Exception. "Invalid username."))))

(defn read-file
  "Safely reads a form from a file."
  [f]
  (with-open [r (reader (file f))]
    (binding [*read-eval* false]
      (read (java.io.PushbackReader. r)))))

(defn prn-file
  "Safely and atomically prints form to file f."
  [f form]
  (let [new-file (file (str f ".new"))]
    (try
     (with-open [w (writer new-file)]
       (binding [*out* w]
         (prn form))
       (.flush w))
     (.renameTo new-file (file f))
     (finally
      (.delete new-file)))))

(defn find-user [username]
  (validate-username username)
  (try
   (read-file (file users-dir username))
   (catch java.io.FileNotFoundException e
     nil)))

(defn add-user [profile]
  (let [username (:username profile)
        salt (gen-salt)
        profile (assoc profile
                  :password (sha1 salt (:password profile))
                  :salt salt
                  :created (.format date-format (java.util.Date.))
                  :groups [(str "org.clojars." username)])
        profile (dissoc profile :confirm :redir)]
    (validate-username username)
    (prn-file (file users-dir username) profile)))

(defn all-users []
  (->> (.listFiles users-dir)
       (map #(.getName %))
       (filter valid-username?)
       (map find-user)))

(defn users-by-email [email]
  (filter #(= email (:email %)) (all-users)))

(defn auth-user [username password]
  (if-let [user (find-user username)]
    (when (= (:password user) (sha1 (:salt user) password))
      user)
    (first (filter #(= (:password %) (sha1 (:salt %) password))
                   (users-by-email username)))))

(defn update-user [profile]
  (assert (:username profile))
  (add-user profile))