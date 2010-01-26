(ns clojars.web.search
  (:use [clojars :only [config]]
        clojars.web.common
        (clojars.db jars)
        [clojars.search :only [search-jars]]
        compojure))

(defn search [account query]
  (html-doc
   account (str (h query) " - search")
   [:h1 "Search for '" (h query) "'"]
   (try
    (let [results (search-jars (config :search-index) query)
          jars (remove nil? (map #(jar-by-id (:_id %)) results))]
      (jar-list jars))
    (catch org.apache.lucene.queryParser.ParseException e
      [:div {:class :error}
       (h (.getMessage e))]))))
