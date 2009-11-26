(ns clojars.test.db
  (:use [clojure.test]
        [clojure.contrib.repl-utils]
        [clojars.db]
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
  (is (not (auth-user "user" "bad-pass")))
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
  (is (= (group-members "test-group") ["user1"]))
  (add-member "test-group" "user1")
  (is (= (count (group-members "test-group")) 1))
    (add-member "test-group" "user2")
  (is (= (count (group-members "test-group")) 2)))

(deftest test-jars
  (add-user "user1@example.org" "user1" "pass" nil)
  (add-jar
   "user1"
   {:name "clojars-web"
    :group "org.clojars.user1"
    :version "0.5.0-SNAPSHOT"
    :dependencies [{:name "clojure"
                    :group "org.clojure"
                    :version "1.0"}]})
  (add-jar
   "user1"
   {:name "clojars-web2"
    :group "org.clojars.user1"
    :version "0.5.0-SNAPSHOT"
    :dependencies [{:name "clojure"
                    :group "org.clojure"
                    :version "1.0"}]})
  (is (= (count (recent-jars)) 2))
  (is (= (:name (second (recent-jars)) "clojars-web2")))

  (is (= (count (jars-by-user "user1")) 2))
  (is (= (count (jars-by-user "user2")) 0))
  (is (= (:group (first (jars-by-user "user1"))) "org.clojars.user1")))


(comment
  (with-db test-db
    (find-user "user"))
  (run-tests)
  )