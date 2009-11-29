(ns clojars.web.jar
  (:use clojars.web.common
        compojure))

(defn show-jar [account jar]
  (html-doc account (:name jar)
            [:h1 (jar-link jar)]
            (:description jar)

            [:div {:class "useit"}
             [:div {:class "lein"}
              [:h3 "leiningen"]
              [:pre
               (tag "[")
               (jar-name jar)
               [:span {:class :string} " \""
                (h (:version jar)) "\""] (tag "]") ]]

             [:div {:class "maven"}
              [:h3 "maven"]
              [:pre
               (tag "<dependency>\n")
               (tag "  <groupId>") (:group jar) (tag "</groupId>\n")
               (tag "  <artifactId>") (:name jar) (tag "</artifactId>\n")
               (tag "  <version>") (h (:version jar)) (tag "</version>\n")
               (tag "</dependency>")]]
             [:p "Pushed by " (user-link (:user jar)) " at " (:created jar)]]))
