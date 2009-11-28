(ns clojars.search
  (:use [clojars.db.jars :only [all-jars]])
  (:import (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.document Field Field$Store Field$Index Document)
           (org.apache.lucene.index IndexReader IndexWriter
                                    IndexWriter$MaxFieldLength)
           (org.apache.lucene.queryParser QueryParser)
           (org.apache.lucene.search IndexSearcher)
           (org.apache.lucene.store NIOFSDirectory RAMDirectory)
           (org.apache.lucene.util Version)))

(def *lucene-version* Version/LUCENE_30)
(def *searcher*)
(def *writer*)

(defn parse-query [query-str]
  (-> (QueryParser. *lucene-version* "title"
                    (StandardAnalyzer. *lucene-version*))
      (.parse query-str)))

(defn search [searcher query max-results]
  (let [docs (.search searcher (parse-query query) max-results)]
    (with-meta
      (map #(.doc searcher (.doc %)) (.scoreDocs docs))
      {:total-hits (.totalHits docs)})))

(defmacro with-index [path & body]
  `(let [path# ~path
         dir# (if path# (NIOFSDirectory. path#) (RAMDirectory.))]
     (with-open [writer# (IndexWriter.
                          dir# (StandardAnalyzer. *lucene-version*)
                          true IndexWriter$MaxFieldLength/UNLIMITED)
                 searcher# (IndexSearcher. (.getReader writer#))]
      (binding [*searcher* searcher#
                *writer* writer#]
        ~@body))))

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

(defn search-jars [query])

(defn index-jar [jar]
  (.deleteDocuments *writer* (parse-query (str "name:" (:name jar)
                                               " group:" (:group jar))))
  (.addDocument
   *writer*
   (doto (Document.)
     (add-field "name" (:name jar) :stored :not-analyzed)
     (add-field "group" (:group jar) :stored :not-analyzed)
     (add-field "version" (:group jar) :stored :not-analyzed)
     (add-field "description" (:description jar) :not-stored :analyzed)
     (add-field "user" (:user jar) :not-stored :not-analyzed))))
