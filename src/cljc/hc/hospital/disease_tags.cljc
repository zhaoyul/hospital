(ns hc.hospital.disease-tags
  "从评估数据提取疾病标签的工具"
  (:require [malli.core :as m]
            [hc.hospital.specs.assessment-complete-cn-spec :as specs]))

(def disease-tag-path-specs
  {[:medical_history :allergy :has_history] specs/过敏史Spec
   [:内分泌系统 :糖尿病病史 :有无] specs/糖尿病病史详情Spec
   [:循环系统 :血管疾病史 :详情 :高血压] specs/高血压详情Spec
   [:循环系统 :心脏疾病史 :有无] specs/心脏疾病通用详情Spec})

(defn assessment->disease-tags
  "根据评估数据生成疾病标签列表"
  [assessment]
  (->> disease-tag-path-specs
       (keep (fn [[path spec]]
               (let [tag (-> spec m/properties :disease/tag)
                     pred (-> spec m/properties :disease/predicate)
                     val (get-in assessment path)]
                 (when (and tag
                            (if pred (pred val) val))
                   tag))))
       vec))
