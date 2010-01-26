(defproject clojars-web "0.5.0-SNAPSHOT"
  :repositories [["clojure-releases" "http://build.clojure.org/releases/"]]
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]

                 [org.apache.maven/maven-ant-tasks "2.0.10"]
                 [org.apache.maven/maven-artifact-manager "2.2.1"]
                 [org.apache.maven/maven-model "2.2.1"]
                 [org.apache.maven/maven-project "2.2.1"]
                 [org.apache.maven.wagon/wagon-file "1.0-beta-6"]

                 [org.apache.lucene/lucene-core "3.0.0"]

                 [compojure "0.3.2"]
                 [org.clojars.ato/nailgun "0.7.1"]

                 [org.clojars.ato/clutch "0.1.0-SNAPSHOT"]
                 [org.clojars.ato/scriptjure "0.1.0-SNAPSHOT"]])
