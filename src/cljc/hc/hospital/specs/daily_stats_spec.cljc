(ns hc.hospital.specs.daily-stats-spec
  (:require [malli.core :as m]))

(def DailyStatsDataSpec
  (m/schema
   [:map {:closed true}
    [:total_visits {:optional true} [:int {:min 0}]]
    [:patient_count {:optional true} [:int {:min 0}]]
    [:signed_count {:optional true} [:int {:min 0}]]
    [:inpatient_count {:optional true} [:int {:min 0}]]
    [:outpatient_count {:optional true} [:int {:min 0}]]
    [:assessment_count {:optional true} [:int {:min 0}]]]))
