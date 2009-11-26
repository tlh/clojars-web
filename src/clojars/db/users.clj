(ns clojars.db.users
  (:use clojars.utils
        clojars.db.utils
        com.ashafa.clutch
        [clojure.contrib.duck-streams :only [writer]])
  (:import java.util.Date
           java.io.File))

(declare write-key-file)

(defn find-user [username]
  (:value (first (:rows (get-view "users" :by-username
                                  {:key username :limit 1})))))

(defn all-users []
  (map :value (:rows (get-view "users" :all))))

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
         [group (:username doc)])))))

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