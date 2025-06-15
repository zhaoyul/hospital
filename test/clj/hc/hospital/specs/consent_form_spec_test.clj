(ns hc.hospital.specs.consent-form-spec-test
  (:require [clojure.test :refer :all]
            [malli.core :as m]
            [hc.hospital.specs.consent-form-spec :as cf-spec]))

(deftest consent-form-spec-validation
  (is (m/validate cf-spec/ConsentFormSpec
                  {:assessment_id 1 :sedation_form "<html>"}))
  (is (not (m/validate cf-spec/ConsentFormSpec
                       {:assessment_id 1 :sedation_form ""})))
  (is (m/validate cf-spec/ConsentFormSpec
                  {:assessment_id 2 :pre_anesthesia_form "<html>"
                   :signed_by "Dr" :signed_at "2024-01-01"})))
