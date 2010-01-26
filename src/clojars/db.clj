;;; TODO: split into seperate files

(ns clojars.db
  "CouchDB database backend"
  (:use [clojure.contrib.ns-utils :only [immigrate]]
        [clojars :only [config]])
  (:require (clojars.db utils users jars groups)
            [com.ashafa.clutch :as clutch]))

(defmacro with-db [& body]
  `(clutch/with-db (config :db)
     ~@body))

(defn init-db []
  (clojars.db.users/init-users-view)
  (clojars.db.jars/init-jars-view))

