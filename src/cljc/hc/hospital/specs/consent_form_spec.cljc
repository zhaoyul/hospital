(ns hc.hospital.specs.consent-form-spec
  (:require [malli.core :as m]))

(def ConsentFormSpec
  (m/schema
   [:map
    [:assessment_id pos-int?]
    [:sedation_form {:optional true} [:maybe [:string {:min 1}]]]
    [:pre_anesthesia_form {:optional true} [:maybe string?]]
    [:anesthesia_form {:optional true} [:maybe string?]]
    [:signed_by {:optional true} [:maybe string?]]
    [:signed_at {:optional true} [:maybe string?]]]))
