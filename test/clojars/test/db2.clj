(ns clojars.test.db2
  (:use [clojure.test]
        [clojure.contrib.repl-utils]
        [clojars.db2]
        [com.ashafa.clutch :only [create-database delete-database with-db]]))

(def test-db {:name "clojars-test"
              :language "clojure"})

(defn setup-db [f]
  (delete-database "clojars-test")
  (create-database "clojars-test")
  (with-db test-db
    (init-db)
    (f)))

(use-fixtures :each setup-db)

(deftest test-add-user
  (add-user "user@example.org" "user" "pass" nil)
  (let [user (find-user "user")]
    (is user)
    (is (= (:username user) "user"))
    (is (= (:email user) "user@example.org")))

  (is (auth-user "user" "pass"))
  (is (not (auth-user "user" "bad-pass")))

  (update-user "user" nil "new-pass" nil)

  (is (auth-user "user" "new-pass"))
  (is (not (auth-user "user" "pass"))))

(deftest test-groups
  (add-user "user1@example.org" "user1" "pass" nil)
  (add-user "user2@example.org" "user2" "pass" nil)
  (is (empty? (group-members "test-group")))
  (add-member "test-group" "user1")

  (is (= (:groups (find-user "user1")) ["org.clojars.user1" "test-group"]))
  (is (= (map :username (group-members "test-group")) ["user1"]))
  (add-member "test-group" "user1")
  (is (= (count (group-members "test-group")) 1))
    (add-member "test-group" "user2")
  (is (= (count (group-members "test-group")) 2)))


(comment
  (with-db test-db
    (find-user "user"))
  (run-tests)
  )