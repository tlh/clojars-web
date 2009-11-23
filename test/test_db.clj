(ns test-db
  (:use [clojure.test]
        [clojure.contrib.repl-utils]
        [clojars.db2]
        [com.ashafa.clutch :only [create-database delete-database with-db]]))

(def test-db {:name "clojars-test"})

(defn setup-db [f]
  (delete-database "clojars-test")
  (create-database "clojars-test")
  (with-db test-db
    (init-db)
    (f)))

(use-fixtures :each setup-db)

(deftest test-add-user
  (add-user "user@example.org" "user" "pass" nil))

; (run-tests)
