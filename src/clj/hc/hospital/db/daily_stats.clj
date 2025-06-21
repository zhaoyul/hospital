(ns hc.hospital.db.daily-stats
  (:require [cheshire.core :as json]
            [malli.core :as m]
            [hc.hospital.specs.daily-stats-spec :as ds-spec])
  (:import [java.time LocalDate]
           [java.time.temporal IsoFields]))

(def default-stats
  "默认的统计字段初始值由 Malli 规范构造"
  (into {}
        (for [[k _ _] (m/children ds-spec/DailyStatsDataSpec)]
          [k 0])))

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
        new-data (merge-with + existing increments)]
    (upsert-daily-stats! query-fn date new-data)
    new-data))

(defn increment-stat-key!
  "将指定统计字段的值加一。"
  [query-fn date k]
  (increment-daily-stats! query-fn date {k 1}))

(def period-of
  "根据 Malli 规范生成统计字段对应的周期类型。"
  (into {}
        (for [[k {:keys [period]} _] (m/children ds-spec/DailyStatsDataSpec)]
          [k period])))

(defn- reset-period-keys
  "重置指定周期字段的值为 0。"
  [m period]
  (reduce-kv (fn [acc k v]
               (if (= (period-of k) period)
                 (assoc acc k 0)
                 (assoc acc k v)))
             {}
             m))

(defn- reset-daily-keys
  "将日周期字段归零，其他周期保持不变。"
  [m]
  (reduce-kv (fn [acc k v]
               (if (= (period-of k) :天)
                 (assoc acc k 0)
                 (assoc acc k v)))
             {}
             (merge default-stats m)))

(defn inherit-or-init-stats!
  "保证指定日期有统计记录。若不存在，则基于前一天的记录创建，并在跨年/跨月/跨周时重置相应前缀字段。"
  [query-fn date]
  (when-not (get-stats-by-date query-fn date)
    (let [curr (LocalDate/parse date)
          prev (.minusDays curr 1)
          prev-str (.toString prev)
          prev-data (:data (get-stats-by-date query-fn prev-str))
          base (or prev-data default-stats)
          base (if (and prev-data (not= (.getYear curr) (.getYear prev)))
                 (reset-period-keys base :年)
                 base)
          base (if (and prev-data (not= (.getMonthValue curr) (.getMonthValue prev)))
                 (reset-period-keys base :月)
                 base)
          base (if (and prev-data (not= (.get prev IsoFields/WEEK_OF_WEEK_BASED_YEAR)
                                         (.get curr IsoFields/WEEK_OF_WEEK_BASED_YEAR)))
                 (reset-period-keys base :周)
                 base)
          ;; 每日统计默认归零
          data (reset-daily-keys base)]
      (upsert-daily-stats! query-fn date data)
      data)))
