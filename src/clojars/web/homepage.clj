(ns clojars.web.homepage
  (:use net.cgrand.enlive-html
        clojars.web.common))

(defsnippet recent-jar-model "clojars/web/homepage.html" [:#recent-jars :li]
  [jar]
  [:a] (set-attr :href (str "/" jar))
  [:a] (content jar))

(defsnippet homepage-template "clojars/web/homepage.html" [:article]
  [recent-jars]
  [:#recent-jars] (content (map recent-jar-model recent-jars)))

(defn homepage [request]
  (render request nil homepage-template ["hello" "world"]))
