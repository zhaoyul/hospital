(ns hc.hospital.pages.assessment-cards
  "评估表页面各个表单卡片的组合与布局。"
  (:require
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined
                                 HeartOutlined HistoryOutlined
                                 MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined
                                 WarningOutlined WomanOutlined]] ; Removed UserOutlined
   ["antd" :refer [Form Row Col]]
   ["react" :as React]
   [clojure.string :as str]
   [taoensso.timbre :as timbre] ; Removed :refer [spy]
   [malli.core :as m]
   ;; Removed malli.util :as mu as it's likely unused here after refactoring
   [hc.hospital.components.assessment-form-components :as afc]
   [hc.hospital.pages.assessment-form-generators :as afg]
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [hc.hospital.utils :as utils]
   [hc.hospital.ui-helpers :as ui-helpers]
   [hc.hospital.components.common-assessment-card :as cac]
   [hc.hospital.natural-language-generators :as nlg]
   [hc.hospital.form-utils :as form-utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

(defn circulatory-system-card "循环系统" [props]
  [cac/common-assessment-card
   {:title "循环系统"
    :icon HeartOutlined
    :color "#e6f7ff"
    :spec assessment-specs/循环系统Spec
    :spec-key :循环系统
    :data-sub ::subs/circulatory-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn respiratory-system-card "呼吸系统" [props]
  [cac/common-assessment-card
   {:title "呼吸系统"
    :icon CloudOutlined
    :color "#e6fffb"
    :spec assessment-specs/呼吸系统Spec
    :spec-key :呼吸系统
    :data-sub ::subs/respiratory-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" [props]
  [cac/common-assessment-card
   {:title "精神及神经肌肉系统"
    :icon NodeIndexOutlined
    :color "#f6ffed"
    :spec assessment-specs/精神及神经肌肉系统Spec
    :spec-key :精神及神经肌肉系统
    :data-sub ::subs/mental-neuromuscular-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn endocrine-system-card "内分泌系统" [props]
  [cac/common-assessment-card
   {:title "内分泌系统"
    :icon ExperimentOutlined
    :color "#f9f0ff"
    :spec assessment-specs/内分泌系统Spec
    :spec-key :内分泌系统
    :data-sub ::subs/endocrine-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn liver-kidney-system-card "肝肾病史" [props]
  [cac/common-assessment-card
   {:title "肝肾病史"
    :icon ProjectOutlined
    :color "#fff7e6"
    :spec assessment-specs/肝肾病史Spec
    :spec-key :肝肾病史
    :data-sub ::subs/liver-kidney-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn digestive-system-card  "消化系统" [props]
  [cac/common-assessment-card
   {:title "消化系统"
    :icon CoffeeOutlined
    :color "#eff8ff"
    :spec assessment-specs/消化系统Spec
    :spec-key :消化系统
    :data-sub ::subs/digestive-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn hematologic-system-card "血液系统" [props]
  [cac/common-assessment-card
   {:title "血液系统"
    :icon ExperimentOutlined
    :color "#fff0f6"
    :spec assessment-specs/血液系统Spec
    :spec-key :血液系统
    :data-sub ::subs/hematologic-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn immune-system-card        "免疫系统" [props]
  [cac/common-assessment-card
   {:title "免疫系统"
    :icon SecurityScanOutlined
    :color "#f6ffed"
    :spec assessment-specs/免疫系统Spec
    :spec-key :免疫系统
    :data-sub ::subs/immune-system-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn special-medication-history-card "特殊用药史" [props]
  [cac/common-assessment-card
   {:title "特殊用药史"
    :icon MedicineBoxOutlined
    :color "#fffbe6"
    :spec assessment-specs/特殊用药史Spec
    :spec-key :特殊用药史
    :data-sub ::subs/special-medication-history-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn special-disease-history-card "特殊疾病病史" [props]
  [cac/common-assessment-card
   {:title "特殊疾病病史"
    :icon WarningOutlined
    :color "#fff1f0"
    :spec assessment-specs/特殊疾病病史Spec
    :spec-key :特殊疾病病史
    :data-sub ::subs/special-disease-history-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(def nutritional-static-content
  [:<>
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
    [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
    [:p {:style {:fontSize "12px" :color "gray"}}
     "0 分：健康；" [:br]
     "1-2 分：衰弱前期；" [:br]
     "≥3 分：衰弱。"]]]])

(defn nutritional-assessment-card "营养评估" [props]
  [cac/common-assessment-card
   {:title "营养评估"
    :icon AppleOutlined
    :color "#f0fff0"
    :spec assessment-specs/营养评估Spec
    :spec-key :营养评估
    :data-sub ::subs/nutritional-assessment-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :additional-detailed-content nutritional-static-content
    :card-props props}])

(defn pregnancy-assessment-card "妊娠" [props]
  [cac/common-assessment-card
   {:title "妊娠"
    :icon WomanOutlined
    :color "#fff0f6"
    :spec assessment-specs/妊娠Spec
    :spec-key :妊娠
    :data-sub ::subs/pregnancy-assessment-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(defn surgical-anesthesia-history-card "手术麻醉史" [props]
  [cac/common-assessment-card
   {:title "手术麻醉史"
    :icon HistoryOutlined
    :color "#e6f7ff"
    :spec assessment-specs/手术麻醉史Spec
    :spec-key :手术麻醉史
    :data-sub ::subs/surgical-anesthesia-history-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])

(def airway-static-content
  [:<>
   [:h4 {:style {:fontStyle "italic"}} "甲颏距离图示与说明"]
   [:p "甲颏距离 (TMD): 指下颌角到颏结节的距离。"]
   [:table {:style {:width "100%" :borderCollapse "collapse" :marginBottom "10px"}}
    [:thead
     [:tr [:th {:style {:border "1px solid #ddd" :padding "4px"}} "分级"]
      [:th {:style {:border "1px solid #ddd" :padding "4px"}} "距离"]
      [:th {:style {:border "1px solid #ddd" :padding "4px"}} "临床意义"]]]
    [:tbody
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅰ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} ">6.5 cm"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "插管通常无困难"]]
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅱ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "6.0-6.5 cm"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "可能存在一定困难"]]
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅲ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "<6.0 cm"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "提示插管困难"]]]]

   [:h4 {:style {:fontStyle "italic" :marginTop "10px"}} "改良Mallampati分级图示与说明表"]
   [:p "改良Mallampati分级 (Modified Mallampati Score): 患者取坐位，头保持中立位，张口伸舌，观察咽部结构。"]
   [:table {:style {:width "100%" :borderCollapse "collapse" :marginBottom "15px"}}
    [:thead
     [:tr [:th {:style {:border "1px solid #ddd" :padding "4px"}} "分级"] [:th {:style {:border "1px solid #ddd" :padding "4px"}} "可见结构"]]]
    [:tbody
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅰ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "软腭、腭垂、腭弓、扁桃体均可见"]]
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅱ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "软腭、腭垂、腭弓可见，扁桃体被舌根部分遮盖"]]
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅲ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "软腭、腭垂根部可见"]]
     [:tr [:td {:style {:border "1px solid #ddd" :padding "4px"}} "Ⅳ级"] [:td {:style {:border "1px solid #ddd" :padding "4px"}} "仅可见硬腭"]]]]
   [:h4 {:style {:marginTop "16px" :borderTop "1px solid #eee" :paddingTop "10px"}} "详细评估项"]])

(defn airway-assessment-card "气道评估" [props]
  [cac/common-assessment-card
   {:title "气道评估"
    :icon NodeIndexOutlined
    :color "#fff7e6"
    :spec assessment-specs/气道评估Spec
    :spec-key :气道评估
    :data-sub ::subs/airway-assessment-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :additional-detailed-content airway-static-content
    :card-props props}])

(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" [props]
  [cac/common-assessment-card
   {:title "椎管内麻醉相关评估"
    :icon GatewayOutlined
    :color "#f0f5ff"
    :spec assessment-specs/椎管内麻醉相关评估Spec
    :spec-key :椎管内麻醉相关评估
    :data-sub ::subs/spinal-anesthesia-assessment-data
    :patient-id-sub ::subs/canonical-patient-outpatient-number
    :update-event ::events/update-canonical-assessment-section
    :report-form-instance-fn (:report-form-instance-fn props)
    :card-props props}])
