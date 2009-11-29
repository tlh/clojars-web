(ns clojars.search
  (:use [clojure.contrib.java-utils :only [file]])
  (:import (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.document Field Field$Store Field$Index Document)
           (org.apache.lucene.index IndexReader IndexWriter
                                    IndexWriter$MaxFieldLength)
           (org.apache.lucene.queryParser QueryParser)
           (org.apache.lucene.search IndexSearcher)
           (org.apache.lucene.store NIOFSDirectory RAMDirectory Directory)
           (org.apache.lucene.util Version)))

(def *lucene-version* Version/LUCENE_30)

(defn parse-query [query-str]
  (-> (QueryParser. *lucene-version* "description"
                    (StandardAnalyzer. *lucene-version*))
      (.parse query-str)))

(defn search [searcher query max-results]
  (let [docs (.search searcher (parse-query query) max-results)]
    (with-meta
      (for [score-doc (.scoreDocs docs)]
        (let [doc (.doc searcher (.doc score-doc))]
          (into {} (map #(vector (keyword (.name %)) (.stringValue %))
                        (.getFields doc)))))
      {:total-hits (.totalHits docs)})))

(defn add-field
  "Nil-safe adding of fields to a document."
  [doc field value store index]
  (when value
    (.add doc (Field. field value
                      (condp = store
                        :stored Field$Store/YES
                        :not-stored Field$Store/NO)
                      (condp = index
                        :analyzed Field$Index/ANALYZED
                        :not-analyzed Field$Index/NOT_ANALYZED
                        :not-indexed Field$Index/NO)))))

(defn make-directory [path]
  (cond (instance? Directory path) path
        path (NIOFSDirectory. (file path))
        :else (RAMDirectory.)))

(defn search-jars [index-dir query]
  (with-open [searcher (IndexSearcher. (make-directory index-dir) false)]
    (search searcher query 20)))

(defn index-jar [index-writer jar]
  (.deleteDocuments index-writer (parse-query (str "name:" (:name jar)
                                                   " group:" (:group jar))))
  (.addDocument
   index-writer
   (doto (Document.)
     (add-field "_id" (:_id jar) :stored :not-indexed)
     (add-field "name" (:name jar) :stored :not-analyzed)
     (add-field "group" (:group jar) :stored :not-analyzed)
     (add-field "version" (:version jar) :stored :not-analyzed)
     (add-field "description" (str (:group jar) " " (:name jar) " "
                                   (:version jar) " " (:user jar) " "
                                   (:description jar)) :not-stored :analyzed)
     (add-field "user" (:user jar) :not-stored :not-analyzed))))

(defn index-jars [index-dir jars]
  (assert index-dir)
  (println "Indexing..." index-dir jars)
  (with-open [writer (IndexWriter. (make-directory index-dir)
                                   (StandardAnalyzer. *lucene-version*)
                                   true IndexWriter$MaxFieldLength/UNLIMITED)]
    (doseq [jar jars]
      (index-jar writer jar))))