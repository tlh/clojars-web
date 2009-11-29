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
    [:ul
     (for [result (search-jars (config :search-index) query)]
       (when-let [jar (jar-by-id (:_id result))]
         [:li {:class "search-results"}
          (jar-link jar) " " (h (:version jar))
          [:span {:class "desc"} " &mdash; "
           (h (:description jar))]]))]
    (catch org.apache.lucene.queryParser.ParseException e
      [:div {:class :error}
       (h (.getMessage e))]))))
