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
