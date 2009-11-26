(ns clojars.db.jars
  (:use (clojars.db utils groups)
        com.ashafa.clutch)
  (:import java.util.Date))

(defn all-jars []
  (map second (view-seq "jars" :all)))

(defn recent-jars []
  (map second (view-seq "jars" :by-created
                        {:descending true})))

(defn jars-by-user [username]
  (map second (view-seq "jars" :by-user {:key username})))

(defn jars-by-group [group]
  (map second (view-seq "jars" :by-group {:key group})))

(defn add-jar [account jarmap & [check-only]]
  (when-not (re-matches #"^[a-z0-9_.-]+$" (:name jarmap))
    (throw (Exception. (str "Jar names must consist of lowercase "
                            "letters, numbers, hyphens and underscores."))))

  (when-not (re-matches #"^[a-zA-Z0-9_.-]+$" (:version jarmap))
    (throw (Exception.
            (str "Versions must consist only of "
                 "letters, numbers, hyphens, underscores and dots."))))

  (check-and-add-group account (:group jarmap) (:name jarmap))

  (create-document
   (assoc jarmap
     :type "jar"
     :user account
     :created (Date.))))

(defn init-jars-view []
  (when-let [doc (get-document "_design/jars")]
    (delete-document doc))

  (create-clj-view
   "jars" "all"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[nil doc]])))

  (create-clj-view
   "jars" "by-user"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:user doc) doc]])))

  (create-clj-view
   "jars" "by-group"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:group doc) doc]])))

  (create-clj-view
   "jars" "by-created"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:created doc) doc]]))))
