(ns clojars.migration
  "Script to migrate from the old SQLite database to CouchDB."
  (:use clojars.db
        clojure.contrib.sql
        [com.ashafa.clutch :only [create-document delete-database
                                  create-database]])
  (:import java.util.Date))

(def sql-db
     {:classname "org.sqlite.JDBC"
      :subprotocol "sqlite"
      :subname "/home/clojars/data/db"})

(defn migrate-users []
 (with-query-results rs ["select * from users"]
   (doseq [user rs]
     (create-document
     {:type :user
      :username (:user user)
      :email (:email user)
      :password (:password user)
      :salt (:salt user)
      :ssh-keys (vec (when-not (empty (:ssh_key user)) [(:ssh_key user)]))
      :created (Date. (:created user))
      :groups (with-query-results rs2
                ["select * from groups where user = ?" (:user user)]
                (vec (map :name rs2)))}))))

(defn migrate-jars []
  (with-query-results rs ["select * from jars"]
   (doseq [jar rs]
     (create-document
      {:type :jar
       :group (:group_name jar)
       :name (:jar_name jar)
       :version (:version jar)
       :description (:description jar)
       :user (:user jar)
       :created (Date. (:created jar))
       :homepage (:homepage jar)
       :scm (:scm jar)
       :authors (vec (filter #(not= "" %) (.split (:authors jar) ",")))}))))

(defn main []
  (delete-database "clojars")
  (create-database "clojars")
  (with-db
    (init-db)
    (with-connection sql-db
      (migrate-users)
      (migrate-jars))))

;(main)