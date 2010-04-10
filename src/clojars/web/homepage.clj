(ns clojars.web.homepage
  (:use net.cgrand.enlive-html
        clojars.web.common))

(defsnippet homepage-template "clojars/web/homepage.html" [:article]
  [recent-jars]
  [:#recent-jars :li] #(for [jar recent-jars]
                         (at %
                             [:a] (set-attr :href (str "/" jar))
                             [:a] (content jar))))

(defn homepage [request]
  (render request nil homepage-template ["hello" "world"]))
