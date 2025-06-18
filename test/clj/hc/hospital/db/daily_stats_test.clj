(ns hc.hospital.db.daily-stats-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :refer [system-fixture *sys*]]
            [hc.hospital.db.daily-stats :as stats]
            [integrant.core :as ig]))

(use-fixtures :once (system-fixture))

(defn query-fn []
  (let [sys @*sys*]
    (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))))

(deftest daily-stats-basic-test
  (let [qf (query-fn)
        date "2099-01-01"]
    (stats/upsert-daily-stats! qf date {:total_visits 1
                                        :patient_count 1})
    (stats/increment-daily-stats! qf date {:signed_count_inc 1
                                           :assessment_count_inc 1})
    (let [{:keys [data]} (stats/get-stats-by-date qf date)]
      (is (= 1 (:patient_count data)))
      (is (= 1 (:signed_count data)))
      (is (= 1 (:assessment_count data))))))
