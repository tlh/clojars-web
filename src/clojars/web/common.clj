(ns clojars.web.common
  (:use [net.cgrand.enlive-html :only [deftemplate prepend
                                       html-content remove-class
                                       substitute]]))

(deftemplate layout-template
  "clojars/web/layout.html"
  [title authenticated article]
  [:title] (prepend (if title (str title " | ") ""))
  [:article] (substitute article)
  [:.authenticated] (when authenticated (remove-class "authenticated"))
  [:.anonymous] (when-not authenticated (remove-class "anonymous")))

(defn authenticated? [request]
  (boolean (:account (:session request))))

(defn render [request title template & args]
  (apply str
         (layout-template title
                          (authenticated? request)
                          (apply template args))))