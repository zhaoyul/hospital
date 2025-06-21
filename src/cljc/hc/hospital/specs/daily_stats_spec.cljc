(ns hc.hospital.specs.daily-stats-spec
  "每日统计数据的 Malli Schema，支持按天、周、月、年累积。"
  (:require [malli.core :as m]))

(def 周期Enum
  "统计周期枚举。"
  (m/schema [:enum :天 :周 :月 :年]))

(def DailyStatsDataSpec
  (m/schema
   [:map {:closed true}
    [:总就诊人数 {:optional true :period :年} [:int {:min 0}]]
    [:今日患者人数 {:optional true :period :天} [:int {:min 0}]]
    [:已签字人数 {:optional true :period :天} [:int {:min 0}]]
    [:住院患者人数 {:optional true :period :天} [:int {:min 0}]]
    [:门诊患者人数 {:optional true :period :天} [:int {:min 0}]]
    [:评估人数 {:optional true :period :天} [:int {:min 0}]]
    [:年累计就诊人数 {:optional true :period :年} [:int {:min 0}]]
    [:月累计就诊人数 {:optional true :period :月} [:int {:min 0}]]
    [:周累计就诊人数 {:optional true :period :周} [:int {:min 0}]]]))
