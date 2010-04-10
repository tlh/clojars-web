(ns clojars.repo
  (:import java.io.File
           java.text.SimpleDateFormat
           java.util.Date)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.contrib.zip-filter.xml :as zf]
            [clojure.contrib.str-utils2 :as str])
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.duck-streams :only [slurp*]]
        [clojure.contrib.seq-utils :only [find-first group-by]]))

;;;; Version numbers

(defn without-nil-values
  "Prunes a map of pairs that have nil values."
  [m]
  (reduce (fn [m entry] (if (nil? (val entry)) m (conj m entry))) (empty m) m))

(defn parse-int [#^String s]
  (when s
    (Integer/parseInt s)))

(defn parse-version
  "Parse a Maven-style version number.

  The basic format is major[.minor[.increment]][-(buildNumber|qualifier)]

  The major, minor, increment and buildNumber are numeric with leading zeros
  disallowed (except plain 0).  If the value after the first - is non-numeric
  then it is assumed to be a qualifier.  If the format does not match then we
  just treat the whole thing as a qualifier."
  [s]
  ;; I'm so evil.  Maven does this in 100 lines of Java. ;-)
  (let [[match major minor incremental build-number qualifier]
        (re-matches #"(0|[1-9][0-9]*)(?:\.(0|[1-9][0-9]*)(?:\.(0|[1-9][0-9]*))?)?(?:-(0|[1-9][0-9]*)|-(.*))?" s)]
    (try
     (without-nil-values
      {:major        (parse-int major)
       :minor        (parse-int minor)
       :incremental  (parse-int incremental)
       :build-number (parse-int build-number)
       :qualifier    (if match qualifier s)})
     (catch NumberFormatException e
       {:qualifier s}))))

(defmacro numeric-or
  "Evaluates exprs one at a time.  Returns the first that returns non-zero."
  ([x] x)
  ([x & exprs]
     `(let [value# ~x]
        (if (zero? value#)
          (numeric-or ~@exprs)
          value#))))

(defn starts-with? [#^String s #^String prefix]
  (.startsWith s prefix))

(defn compare-versions
  "Compare two maven versions.  Accepts either the string or parsed
  representation."
  [x y]
  (let [x (if (string? x) (parse-version x) x)
        y (if (string? y) (parse-version y) y)]
   (numeric-or
    (compare (:major x 0) (:major y 0))
    (compare (:minor x 0) (:minor y 0))
    (compare (:incremental x 0) (:incremental y 0))
    (let [qx (:qualifier x), qy (:qualifier y)]
      (if qx
        (if qy
          (if (and (> (count qx) (count qy)) (starts-with? qx qy))
            -1                          ; x is longer, it's older
            (if (and (< (count qx) (count qy)) (starts-with? qx qy))
              1                     ; y is longer, it's older
              (compare qx qy)))     ; same length, so string compare
          -1)                       ; y has no qualifier, it's younger
        (if qy
          1                         ; x has no qualifier, it's younger
          0)))                      ; no qualifiers
    (compare (:build-number x 0) (:build-number y 0)))))

(defn snapshot-version? [s]
  (.endsWith (str/upper-case s) "SNAPSHOT"))

;;;; Maven POM

(defn pom-file? [#^File f]
  (re-matches #"[^.].*\.pom" (.getName f)))

(defn uncamel
  "Converts camelCase to something-more-lispy."
  [s]
  (if-let [[match lower cap more] (re-matches #"([^A-Z]+)([A-Z]+)(.*)" s)]
    (str lower "-" (str/lower-case cap) (uncamel more))
    s))

(defn plural-of? [singular plural]
  (or (= (str singular "s") plural)
      (= (str/replace singular #"y$" "ies") plural)))

(defn parse-pom-xml
  "Parses the small JSON-like subset of XML that maven uses (no attributes)
  into a nice nested map and vector structure.  We ignore the build section
  if its present."
  [element]
  (let [contents (:content element)
        tag (name (:tag element :invalid))]
    (cond
     (string? element) element
     (empty? contents) nil                          ; no value
     (every? string? contents) (apply str contents) ; string value
     (every? #(plural-of? (name (:tag % ::invalid)) tag) contents)
     (vec (map parse-pom-xml contents))     ; list
     :else
     (into {}
           (for [c contents]
             [(keyword (uncamel (name (:tag c :string-value))))
              (parse-pom-xml c)])))))

(defn parse-pom [#^File f]
  (let [name (.getName f)
        pom (-> (parse-pom-xml (xml/parse f))
                (assoc :pom-file name))]
    (if (snapshot-version? (:version pom) )
      (let [prefix-length (+ (count (:artifact-id pom))
                             (count (:group-id pom))
                             3)
            ss-tag (subs name prefix-length (- (count name) (count ".pom")))
            [timestamp build] (str/split ss-tag #"-")]
        (assoc pom :snapshot {:timestamp timestamp
                              :build-number (Integer/parseInt build)})))))

(defn #^File group-path [repodir group-id]
  (file repodir (str/replace group-id "." File/separator)))

(defn #^File artifact-path [repodir group-id artifact-id]
  (file (group-path repodir group-id) artifact-id))

(defn directory? [#^File f]
  (.isDirectory f))

(defn subdirs [#^File f]
  (filter directory? (.listFiles f)))

(defn poms-for-artifact [repodir group-id artifact-id]
  (let [path (artifact-path repodir group-id artifact-id)]
    (when (directory? path)
      (->> (subdirs path)
           (mapcat #(.listFiles #^File %))
           (filter pom-file?)
           (map parse-pom)))))

;;;; Maven Metadata

(defn metadata-file? [#^File f]
  (= (.getName f) "maven-metadata.xml"))

(defn metadata-file-seq [dir]
  (filter metadata-file? (file-seq dir)))

(defn parse-metadata [f]
  (let [z (zip/xml-zip (xml/parse f))]
   {:group-id     (zf/xml1-> z :groupId zf/text)
    :artifact-id  (zf/xml1-> z :artifactId zf/text)
    :version      (zf/xml1-> z :version zf/text)
    :snapshot     (first (for [ss (zf/xml-> z :versioning :snapshot)]
                           {:timestamp    (zf/xml1-> ss :timestamp zf/text)
                            :build-number (zf/xml1-> ss :buildNumber zf/text)}))
    :versions     (zf/xml-> z :versioning :versions :version zf/text)
    :last-updated (zf/xml1-> z :versioning :lastUpdated zf/text)}))

(defn print-metadata [m]
  (binding [*prxml-indent* 2]
    (prxml
     :decl!
     [:metadata
      [:groupId    (:group-id m)]
      [:artifactId (:artifact-id m)]
      [:version    (:version m)]
      [:versioning
       (when (seq (:versions m))
         `[:versions
           ~@(for [v (:versions m)]
               [:version v])])
       (when (:snapshot m)
         [:snapshot
          [:timestamp (-> m :snapshot :timestamp)]
          [:buildNumber (-> m :snapshot :build-number)]])
       [:lastUpdated (:last-updated m)]]])
    (println)))

(defn make-metadata
  "Given a seq of poms with the same groupId and artifactId build a metadata
   map."
  [poms]
  (let [poms (sort-by :version compare-versions poms)]
    (assoc (select-keys (first poms) [:group-id :artifact-id :version])
      :versions     (distinct (map :version poms))
      :last-updated (.format (SimpleDateFormat. "yyyyMMddHHmmss") (Date.)))))

(defn make-snapshot-metadata
  "Given a seq of poms with the same groupId, artifactId and snapshot version
  build a snapshot metadata map."
  [poms]
  (let [latest-pom (apply max-key #(-> % :snapshot :build-number) poms)]
    (assoc (select-keys latest-pom [:group-id :artifact-id :version :snapshot])
      :last-updated (.format (SimpleDateFormat. "yyyyMMddHHmmss") (Date.)))))

;;;; Repository fsck

(defn jar-file? [#^File f]
  (re-matches #"[^.].*\.jar" (.getName f)))

(defn #^File sibling [#^File f]
  (let [n (cond
           (jar-file? f) (str/replace (.getName f) #"\.jar$" ".pom")
           (pom-file? f) (let [_ (println f)
                               pkg (:packaging (parse-pom f) "jar")]
                           (str/replace (.getName f) #"\.pom$" (str "." pkg)))
           :else (throw (Exception. (str "Not a jar or a pom: " f))))]
    (file (or (.getParent f) "") n)))

(defn find-orphans
  "Returns a list of poms with jars and jars without poms."
  [dir]
  (doall (filter #(and (or (println %)
                     (jar-file? %) (pom-file? %))
                 (not (.exists (sibling %)))) (file-seq dir))))

;(find-orphans (file "/home/ato/tmp/clojars"))