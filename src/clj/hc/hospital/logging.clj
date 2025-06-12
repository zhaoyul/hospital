(ns hc.hospital.logging
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io])
  (:import [java.time Instant ZoneId LocalDate]
           [java.time.format DateTimeFormatter]))

(def ^:private date-format (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn- log-file-path
  [dir prefix ^Instant inst]
  (let [ld (.format (.atZone inst (ZoneId/systemDefault)) date-format)]
    (str (or dir "log") "/" (or prefix "hospital") "-" ld ".log")))

(defn daily-file-appender
  "Returns a Timbre appender that writes logs to a daily rolling file."
  [& [{:keys [dir prefix] :or {dir "log" prefix "hospital"}}]]
  (let [lock (Object.)]
    {:enabled? true
     :fn (fn [{:keys [timestamp_ output_]}]
           (let [^Instant inst (force timestamp_)
                 out (force output_)
                 path (log-file-path dir prefix inst)]
             (io/make-parents path)
             (locking lock
               (spit path out :append true))))}))

(defn configure-logging!
  "Configure Timbre to log to console and daily rolling file."
  ([] (configure-logging! {}))
  ([opts]
   (timbre/merge-config!
    {:appenders {:println (timbre/println-appender)
                 :daily-file (apply daily-file-appender [opts])}})))
