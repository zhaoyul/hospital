(ns hc.hospital.db.daily-stats
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(def default-stats
  {:total_visits 0
   :patient_count 0
   :signed_count 0
   :inpatient_count 0
   :outpatient_count 0
   :assessment_count 0})

(defn- encode-data [m]
  (json/generate-string m))

(defn- decode-data [s]
  (when s (json/parse-string s true)))

(defn upsert-daily-stats!
  [query-fn date data]
  (query-fn :upsert-daily-stats!
            {:date date
             :data (encode-data data)}))

(defn get-stats-by-date
  [query-fn date]
  (when-let [row (query-fn :get-daily-stats-by-date {:date date})]
    (update row :data decode-data)))

(defn increment-daily-stats!
  [query-fn date increments]
  (let [existing (or (:data (get-stats-by-date query-fn date)) default-stats)
        normalized (into {}
                          (map (fn [[k v]]
                                 [(-> k name (str/replace #"_inc$" "") keyword) v])
                               increments))
        new-data (merge-with + existing normalized)]
    (upsert-daily-stats! query-fn date new-data)
    new-data))
