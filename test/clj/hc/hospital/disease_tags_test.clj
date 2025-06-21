(ns hc.hospital.disease-tags-test
  (:require [clojure.test :refer [deftest is]]
            [hc.hospital.disease-tags :as tags]))

(deftest assessment->disease-tags-test
  (let [assessment {:medical_history {:allergy {:has_history true}}
                    :内分泌系统 {:糖尿病病史 {:有无 :有}}
                    :循环系统 {:血管疾病史 {:详情 {:高血压 {:病史时长 :小于1年}}}
                               :心脏疾病史 {:有无 :有}}}
        expected #{{:label "过敏史" :color "magenta"}
                   {:label "糖尿病" :color "purple"}
                   {:label "高血压" :color "volcano"}
                   {:label "心脏病" :color "red"}}]
    (is (= expected (set (tags/assessment->disease-tags assessment))))))

