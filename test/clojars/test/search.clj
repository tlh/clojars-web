(ns clojars.test.search
  (:use [clojure.test]
        (clojars search db)
        [clojars.test.db :only [setup-db]]
        [com.ashafa.clutch :only [with-db]]))

(defn setup-index [f]
  (with-index nil
    (f)))

(use-fixtures :each setup-db setup-index)

(deftest test-indexer
  (add-user "user@example.org" "user1" "pass" nil)
  (add-jar
   "user1"
   {:name "clojars-web"
    :group "org.clojars.user1"
    :version "0.5.0-SNAPSHOT"
    :description "A jar for testing the search indexing."
    :dependencies [{:name "clojure"
                    :group "org.clojure"
                    :version "1.0"}]})
  (doall (map index-jar (all-jars)))
  (is (= (count (search-jars "testing")) 1)))