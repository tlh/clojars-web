(ns clojars.db.groups
  (:use (clojars.db utils users)
        com.ashafa.clutch))

(defn group-members [group]
  (map :value (:rows (get-view "users" :by-group {:key group}))))

(defn add-member [group username]
  (let [user (find-user username)]
    (assert user)
    (when-not (some #{group} (:groups user))
      (update-document user {:groups (conj (:groups user) group)}))))

(defn check-and-add-group [account group jar]
  (when-not (re-matches #"^[a-z0-9_.-]+$" group)
    (throw (Exception. (str "Group names must consist of lowercase "
                            "letters, numbers, hyphens, underscores "
                            "and full-stops."))))
  (let [members (group-members group)]
    (if (empty? members)
      (if (or (find-user group) (*reserved-names* group))
        (throw (Exception. (str "The group name " group " is already taken.")))
        (add-member group account))
      (when-not (some #{account} members)
        (throw (Exception. (str "You don't have access to the "
                                group " group. Only " (vec members))))))))