(ns clojars.db.utils
  (:use [clojure.contrib.json.write :only [print-json]]
        clojars.utils
        com.ashafa.clutch)
  (:import java.util.Date
           java.text.SimpleDateFormat))

;;; TODO: merge some of these into Clutch

(def ssh-options (str "no-agent-forwarding,no-port-forwarding,"
                      "no-pty,no-X11-forwarding"))

(def *reserved-names*
     #{"clojure" "clojars" "clojar" "register" "login"
       "pages" "logout" "password" "username" "user"
       "repo" "repos" "jar" "jars" "about" "help" "doc"
       "docs" "pages" "images" "js" "css" "maven" "api"
       "download" "create" "new" "upload" "contact" "terms"
       "group" "groups" "browse" "status" "blog" "search"
       "email" "welcome" "devel" "development" "test" "testing"
       "prod" "production" "admin" "administrator" "root"
       "webmaster" "profile" "dashboard" "settings" "options"
       "index" "files" "uber" "uberjar" "standalone" "slim"
       "slimjar"})

(def *date-format* (SimpleDateFormat. "yyyy/MM/dd HH:mm:ss Z"))

(defmethod print-json Date [date]0
  (print (str\" (.format *date-format* date) \")))

(def *chunk-size* 32)

(defn view-seq
  "Returns a lazy sequence of [key value] pairs from a view in the
  database. Queries will be with chunks of size *chunk-size* records."
  ([design-doc view-key] (view-seq design-doc view-key {}))
  ([design-doc view-key options-map]
     (let [limit (options-map :limit)
           options* (conj-when options-map limit
                               {:limit (min limit *chunk-size*)})
           results (get-view design-doc view-key options*)
           offset (:offset results)
           total-rows (:total_rows results)
           rows (for [row (:rows results)]
                  (with-meta [(:key row) (:value row)]
                    {:id (:id row)}))]
       (lazy-seq
        (concat
         rows
         (when (< (+ offset (count rows)) total-rows)
           (view-seq design-doc view-key
                     (-> options-map
                         (conj-when limit {:limit (- limit (count rows))})
                         (assoc :skip (+ offset (count rows)))))))))))

(defmacro create-clj-view
  "Shorthand for creating a clojure view."
  [design-doc view map-fn & reduce-fn]
  `(create-view
    ~design-doc ~view
    (with-clj-view-server ~map-fn ~@reduce-fn)))
