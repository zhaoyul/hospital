(ns hc.hospital.core
  "应用程序入口, 启动与停止整体系统"
  (:require
   [taoensso.timbre :as log]
   [integrant.core :as ig]
   [hc.hospital.web.routes.pages]
   [hc.hospital.config :as config]
   [hc.hospital.env :refer [defaults]]
   [clojure.java.io :as io]

   ;; Edges
   [kit.edge.server.undertow]
   [hc.hospital.web.handler]

   ;; Routes
   [hc.hospital.web.routes.api]
   [hc.hospital.web.routes.patient-pages]
   [hc.hospital.web.routes.report-pages]
   [hc.hospital.web.routes.role-api]
   [hc.hospital.web.routes.patient-api]
   [hc.hospital.web.routes.user-api]
   [hc.hospital.web.routes.overview-api]
   [hc.hospital.web.routes.consent-form-api]
   [kit.edge.db.sql.conman]
   [hc.hospital.db.oracle]
   [kit.edge.db.sql.migratus])
  (:import [java.util Properties])
  (:gen-class))

(comment
  "记录所有线程中的未捕获异常")
(Thread/setDefaultUncaughtExceptionHandler
 (fn [thread ex]
   (log/error {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))})))

(defonce system
  ^{:doc "保存 Integrant 系统状态的原子"}
  (atom nil))

(defn stop-app
  "停止系统并执行自定义 :stop 钩子"
  []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app
  "启动系统, 参数可传入自定义的 :start 钩子与配置选项"
  [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       ig/expand
       ig/init
       (reset! system)))

(defn- print-version-info
  "读取并输出构建信息" [path]
  (if-let [resource-url (io/resource path)]
    (with-open [is (io/input-stream resource-url)]
      (let [props (Properties.)]
        (.load props is)
        (println "Starting hc.hospital application...")
        (println (str "Build Timestamp: " (.getProperty props "build.timestamp" "UNKNOWN")))
        (println (str "Git Commit Hash: " (.getProperty props "git.commit.hash" "UNKNOWN")))))
    (do
      (println "Starting hc.hospital application...")
      (println (str "WARNING: Version properties file not found at " path)))))

(defn -main
  "程序入口"
  [& _]
  (let [properties-path "hc/hospital/version.properties"]
    (try
      (print-version-info properties-path)
      (catch Exception e
        (println "Starting hc.hospital application...")
        (println (str "ERROR: Could not load version information from " properties-path))
        (println (.getMessage e)))))
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
