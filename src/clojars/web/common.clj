(ns clojars.web.common
  (:use compojure))

(defn when-ie [& contents]
  (str
   "<!--[if IE]>"
   (apply html contents)
   "<![endif]-->"))

(defn html-doc [account title & body]
  (html
   "<!DOCTYPE html>"
   [:html {:lang :en}
    [:head
     [:meta {:charset "utf-8"}]
     [:title
      (when title
        (str title " | "))
      "Clojars"]
     (map #(include-css (str "/stylesheets/" %))
          ["reset.css" "grid.css" "screen.css"])
     (when-ie (include-js "/js/html5.js"))]

    [:body
     [:div {:class "container_12 header"}
      [:header
       [:hgroup {:class :grid_4}
        [:h1 (link-to "/" "Clojars")]
        [:h2 "Simple Clojure jar repository"]]
       [:nav
        (if account
          (unordered-list
           [(link-to "/browse" "browse")
            (link-to "/" "dashboard")
            (link-to "/profile" "profile")
            (link-to "/logout" "logout")])
          (unordered-list
           [(link-to "/browse" "browse")
            (link-to "/login" "login")
            (link-to "/register" "register")]))
        (form-to [:get "/search"]
          [:input {:name "q" :id "search" :class :search :value
                   "Search jars..."
                   :onclick (str "if (!this.cleared==1) {this.value=''; "
                                 "this.cleared=1;}")}])]]
      [:div {:class :clear}]]]
    [:div {:class "container_12 article"}
     [:article
      body]]
    [:footer
     (link-to "mailto:contact@clojars.org" "contact")
     (link-to "http://github.com/ato/clojars-web" "code")
     (link-to "http://wiki.github.com/ato/clojars-web" "help")]]))

(defn error-list [errors]
  (when errors
    [:div {:class :error}
     [:strong "Blistering barnacles!"]
     "  Something's not shipshape:"
     (unordered-list errors)]))

(defn tag [s]
  (html [:span {:class "tag"} (h s)]))

(defn jar-link [jar]
  (link-to
   (if (= (:group jar) (:name jar))
     (str "/" (:name jar))
     (str "/" (:group jar) "/" (:name jar)))
   (if (= (:group jar) (:name jar))
     (html [:span {:class "jar-name"} (:name jar)])
     (html [:span {:class "group-name"} (:group jar)] "/"
           [:span {:class "jar-name"}] (:name jar)))))

(defn user-link [user]
  (link-to (str "/users/" user)
           user))

(defn group-link [group]
  (link-to (str "/groups/" group) group))

(defn jar-name [jar]
  (if (= (:group jar) (:name jar))
    (h (:name jar))
    (h (str (:group jar) "/" (:name jar)))))

(defn- org-clojars-group?
  [jar]
  (-> jar :group (.startsWith "org.clojars.")))

(defn jar-list [jars]
  (html
   (if (empty? jars)
     [:p {:class "search-null"} "Nothing found."]
     [:ul {:class "browse-results"}
      (for [jar (concat (remove org-clojars-group? jars)
                        (filter org-clojars-group? jars))]
        [:li
         (jar-link jar) " "
         [:span {:class "version"} "(" (h (:version jar)) ")"]
         [:div {:class "desc"}
          (h (:description jar))]])])))
