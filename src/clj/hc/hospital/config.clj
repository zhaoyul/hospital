(ns hc.hospital.config
  "读取系统配置"
  (:require
    [kit.config :as config]))

(def ^:const system-filename "system.edn")

(defn system-config
  "根据给定选项读取 system.edn 配置"
  [options]
  (config/read-config system-filename options))
