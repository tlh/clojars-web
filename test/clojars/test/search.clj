(ns clojars.test.search
  (:use [clojure.test]
        (clojars search db)
        (clojars.db users jars groups)
        [clojars.test.db :only [setup-db]])
  (:import java.io.File))

(def temp-dir)

(defn delete-dir
  "Recursively delete a directory."
  [file]
  (when (.isDirectory file)
    (doseq [child (.list file)]
      (delete-dir (File. file child))))
  (.delete file))

(defn setup-temp-dir [f]
  (binding [temp-dir (File/createTempFile "clojars-test" ".tmp")]
    (.delete temp-dir)
    (.mkdir temp-dir)
    (try
     (f)
     (finally
      (delete-dir temp-dir)))))

(use-fixtures :each setup-db setup-temp-dir)

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
  (println (all-jars))
  (index-jars temp-dir (all-jars))
  (is (= (count (search-jars temp-dir "testing")) 1))
  (is (= (count (search-jars temp-dir "bogus")) 0))
  (is (= (:name (first (search-jars temp-dir "testing"))) "clojars-web")))