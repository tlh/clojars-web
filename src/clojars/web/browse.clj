(ns clojars.web.browse
  (:use clojars.web.common
        (clojars.db jars users)
        compojure))

(defn browse
  ([account]
     (browse account "A"))
  ([account letter]
     (html-doc account "Browse"
      [:h1 "Browse jars"]
      [:div
       (for [letter "ABCDEFGHIJKLMNOPQRSTUVWXYZ"]
         (html (link-to (str "/browse/" letter) letter) " "))]
      [:p "Jars starting with the letter " [:em (h letter)] ":"]
      (jar-list (jars-by-letter letter)))))