(ns hc.hospital.logging
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io])
  (:import [java.time Instant ZoneId]
           [java.time.format DateTimeFormatter]))

(def ^:private date-format (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn- log-file-path
  [dir prefix ^Instant inst]
  (let [ld (.format (.atZone inst (ZoneId/systemDefault)) date-format)]
    (str (or dir "log") "/" (or prefix "hospital") "-" ld ".log")))

(defn daily-file-appender
  "创建按日滚动写入的文件 appender。支持异步写入。"
  [& [{:keys [dir prefix async?]
       :or {dir "log" prefix "hospital" async? true}}]]
  (let [lock (Object.)]
    {:enabled? true
     :async? async?
     ;; :fn 是 appender 的核心回调，Timbre 会在每条日志事件产生时调用
     ;; 该函数。这里解析时间戳与输出内容，生成对应的文件路径并写入
     ;; 日志文件，写入过程通过锁保证线程安全。
     :fn (fn [{:keys [timestamp_ output_]}]
           (let [^Instant inst (force timestamp_)
                 out (force output_)
                 path (log-file-path dir prefix inst)]
             (io/make-parents path)
             (locking lock
               (spit path out :append true))))}))

(defn configure-logging!
  "配置日志系统，输出到控制台和每日滚动文件，默认异步写入。"
  ([] (configure-logging! {}))
  ([opts]
   (timbre/merge-config!
    {:appenders {:println (assoc (timbre/println-appender) :async? true)
                 :daily-file (apply daily-file-appender [opts])}})))
