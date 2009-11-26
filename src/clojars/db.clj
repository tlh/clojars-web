;;; TODO: split into seperate files

(ns clojars.db
  "CouchDB database backend"
  (:use [clojure.contrib.ns-utils :only [immigrate]])
  (:require (clojars.db utils users jars groups)))

(immigrate 'clojars.db.utils
           'clojars.db.users
           'clojars.db.jars
           'clojars.db.groups)


(defn init-db []
  (init-users-view)
  (init-jars-view))

(comment
  (conj {} {1 2})
  (with-db db (doall (view-seq "users" :all {})))




  (def db {:name "clojars-test"
           :language "clojure"})
  (with-db db
    (init-db)

    (find-user "atox")
    )

  (lazy-seq (concat [1 2] [3 4]))



  (with-db db
    (get-view "users" :all))

  (= [1 2 3] '(1 2 3))

  (update-document)

  (with-db db
    (create-document
     {:type "user"
      :username "atox"}))

  (ad-hoc-view
   (with-clj-view-server
     (fn [doc] [nil doc]))))
