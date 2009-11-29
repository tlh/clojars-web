(ns clojars.core
  (:use [clojars.web :only [clojars-app]]
        [clojars.db :only [init-db with-db]]
        [clojars :only [config]]
        compojure)
  (:import com.martiansoftware.nailgun.NGServer
           java.net.InetAddress))

(defn main []
  (with-db (init-db))
  (when (config :http-port)
    (def server (run-server {:port (config :http-port)}
                            "/*" (servlet clojars-app))))
  (when (config :nailgun-port)
    (println "Starting nailgun on port" (config :nailgun-port))
    (.run (NGServer. (InetAddress/getByName "127.0.0.1")
                     (config :nailgun-port)))))

; (main)