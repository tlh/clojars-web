(ns clojars.web.jar
  (:use net.cgrand.enlive-html
        clojars.web.common
        [clojars.utils :only [pretty-date unique-by]]
        [clojars.repo :only [poms-for-artifact compare-versions
                             snapshot-version?]])
  (:require [clojure.contrib.str-utils2 :as str]))

(defn format-lein-id [{:keys [group-id artifact-id]}]
  (if (= group-id artifact-id)
    group-id
    (str group-id "/" artifact-id)))

(defn scm-tag [pom]
  (when-let [tag (-> pom :scm :tag)]
    (if (> (count tag) 10)
      (str (subs tag 0 7) "...")
      tag)))

(defn scm-url [pom]
  (let [base (-> pom :scm :url)
        tag (-> pom :scm :tag)]
    (when (and base tag
               (and (.startsWith base "http://github.com/")))
      (str base "/tree/" tag))))

(defn homepage [pom]
  (or (:url pom) (-> pom :scm :url)))

(defn pom-url [pom]
  (str "/repo/" (:group-id pom) "/" (:artifact-id pom) "/" (:version pom) "/"
       (:pom-file pom)))

(defn jar-url [pom]
  (str/replace (pom-url pom) #"pom$" (:packaging pom "jar")))

(defsnippet jar-version-model "clojars/web/jar.html" [:.versions :li]
  [pom]
  [:.version] (content (:version pom))
  [:.date]    (content (pretty-date (:last-modified pom)))
  [:.scmtag]  (content (scm-tag pom))
  [:.scmtag]  (set-attr :href (scm-url pom)))

(defsnippet show-jar-template "clojars/web/jar.html" [:article]
  [pom other-poms]
  #{[:.useit :.lein-id] [:h1]}  (content (format-lein-id pom))
  [:.description]         (content (:description pom))
  [:.useit :.group-id]    (content (:group-id pom))
  [:.useit :.artifact-id] (content (:artifact-id pom))
  [:.useit :.version]     (content (:version pom))
  [:.versions]            (content (map jar-version-model other-poms))
  [:.links :.homepage]    #(if (homepage pom) % nil)
  [:.links :.homepage :a] (set-attr :href (homepage pom))
  [:.links :.pom :a]      (set-attr :href (pom-url pom))
  [:.links :.jar :a]      (set-attr :href (jar-url pom)))

(defn show-jar [request]
  (let [[group-id artifact-id] (:route-params request)
        artifact-id (or artifact-id group-id)
        poms (->> (poms-for-artifact "/home/ato/tmp/clojars"
                                     group-id artifact-id)
                  (sort-by :last-modified #(compare %2 %1))
                  (sort-by :version #(compare-versions %2 %1))
                  (unique-by :version))
        selected-pom (or (first (remove #(snapshot-version? (:version %)) poms))
                         (first poms))]
    (if selected-pom
      (render request nil show-jar-template selected-pom poms)
      :next)))