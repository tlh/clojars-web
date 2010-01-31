(ns clojars.db.jars
  (:use (clojars.db utils groups)
        [clojars.utils :only [unique-by]]
        [clojars.search :only [index-jars]]
        [clojars :only [config]]
        [com.ashafa.clutch :exclude [config]])
  (:import java.util.Date))

(defn find-jar [group name]
  (first (map second (view-seq "jars" :by-full-name
                               {:key (str group "/" name)}))))

(defn find-canon-jar [name]
  (find-jar name name))

(defn all-jars []
  (map second (view-seq "jars" :all)))

(defn reindex-jars []
  (index-jars (config :search-index) (sort-by :created (all-jars))))

(defn recent-jars []
  (unique-by (juxt :group :name)
             (map second (view-seq "jars" :by-created
                                   {:descending true}))))

(defn jars-by-user [username]
  (sort-by (juxt :group :name)
   (unique-by (juxt :group :name)
              (map second (view-seq "jars" :by-user {:key username})))))

(defn jars-by-group [group]
  (sort-by (juxt :group :name)
   (unique-by (juxt :group :name)
              (map second (view-seq "jars" :by-group {:key group})))))

(defn jars-by-letter [letter]
  (sort-by (juxt :name :group)
   (unique-by (juxt :group :name)
              (map second (view-seq "jars" :by-letter {:key letter})))))

(defn jar-by-id [id]
  (get-document id))

(defn add-jar [account jarmap & [check-only]]
  (when-not (re-matches #"^[a-z0-9_.-]+$" (:name jarmap))
    (throw (Exception. (str "Jar names must consist of lowercase "
                            "letters, numbers, hyphens and underscores."))))

  (when-not (re-matches #"^[a-zA-Z0-9_.-]+$" (:version jarmap))
    (throw (Exception.
            (str "Versions must consist only of "
                 "letters, numbers, hyphens, underscores and dots."))))

  (check-and-add-group account (:group jarmap) (:name jarmap))

  (when-not check-only
   (let [{id :id} (create-document
                   (assoc jarmap
                     :type "jar"
                     :user account
                     :created (Date.)))]
     (index-jars (config :search-index) [(get-document id)]))))

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
   "jars" "by-name"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:name doc) doc]])))

  (create-clj-view
   "jars" "by-letter"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(.toUpperCase (subs (:name doc) 0 1)) doc]])))

  (create-clj-view
   "jars" "by-full-name"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(str (:group doc) "/" (:name doc)) doc]])))

  (create-clj-view
   "jars" "by-created"
   (fn [doc]
     (when (= (doc :type) "jar")
       [[(:created doc) doc]]))))
