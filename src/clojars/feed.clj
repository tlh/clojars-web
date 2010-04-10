(ns clojars.feed
  (:import java.util.zip.GZIPOutputStream
           java.io.FileOutputStream
           java.io.PrintWriter)
  (:use [clojars.repo :only [pom-seq compare-versions]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.seq-utils :only [group-by]])
  (:gen-class))

(defn full-feed [repo]
  (let [grouped-jars (->> (pom-seq repo)
                          (map #(select-keys % [:group-id :artifact-id :version
                                                :description :scm :url]))
                          (group-by (juxt :group-id :artifact-id))
                          (vals))]
   (for [jars grouped-jars]
     (let [jars (sort-by :version #(compare-versions %2 %1) jars)]
       (-> (first jars)
           (dissoc :version)
           (assoc :versions (vec (distinct  (map :version jars)))))))))

(defn write-feed! [feed f]
  (with-open [w (-> (FileOutputStream. f)
                    (GZIPOutputStream.)
                    (PrintWriter.))]
    (binding [*out* w]
      (doseq [form feed]
        (prn form)))))

(defn -main [repo dest]
 (write-feed! (full-feed (file repo)) dest))

