(ns hc.hospital.pages.assessment-cards
  "评估表页面各个表单卡片的组合与布局。"
  (:require
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined
                                 HeartOutlined HistoryOutlined
                                 MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined
                                 UserOutlined WarningOutlined WomanOutlined]]
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
   [hc.hospital.components.assessment-card :as ac]
   [hc.hospital.components.assessment-summary :as as]
   [hc.hospital.components.assessment-detailed-view :as adv]
   [hc.hospital.natural-language-generators :as nlg]
   [hc.hospital.form-utils :as form-utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

(defn- circulatory-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:circulatory-data props)
    :spec assessment-specs/循环系统Spec
    :section-key :循环系统}])

(defn- circulatory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id circulatory-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :循环系统
      :spec                  assessment-specs/循环系统Spec
      :data                  circulatory-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-circulatory-system-spec")}]))

(defn circulatory-system-card "循环系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> HeartOutlined {:style {:marginRight "8px"}}]
           :title        "循环系统"
           :bg-color     "#e6f7ff"
           :data-sub     [::subs/circulatory-system-data]
           :data-key     :circulatory-data
           :summary-view circulatory-system-summary-view
           :detailed-view circulatory-system-detailed-view}
          props)])

(defn- respiratory-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:respiratory-data props)
    :spec assessment-specs/呼吸系统Spec
    :section-key :呼吸系统}])

(defn respiratory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id respiratory-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :呼吸系统
      :spec                  assessment-specs/呼吸系统Spec
      :data                  respiratory-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-respiratory-system-spec")}]))

(defn respiratory-system-card "呼吸系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> CloudOutlined {:style {:marginRight "8px"}}]
           :title        "呼吸系统"
           :bg-color     "#e6fffb"
           :data-sub     [::subs/respiratory-system-data]
           :data-key     :respiratory-data
           :summary-view respiratory-system-summary-view
           :detailed-view respiratory-system-detailed-view}
          props)])

;; Mental Neuromuscular System Card
(defn- mental-neuromuscular-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:mn-data props)
    :spec assessment-specs/精神及神经肌肉系统Spec
    :section-key :精神及神经肌肉系统}])

(defn mental-neuromuscular-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id mn-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :精神及神经肌肉系统
      :spec                  assessment-specs/精神及神经肌肉系统Spec
      :data                  mn-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-mental-neuromuscular-system-spec")}]))

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
           :title        "精神及神经肌肉系统"
           :bg-color     "#f6ffed"
           :data-sub     [::subs/mental-neuromuscular-system-data]
           :data-key     :mn-data
           :summary-view mental-neuromuscular-system-summary-view
           :detailed-view mental-neuromuscular-system-detailed-view}
          props)])

;; Endocrine System Card
(defn- endocrine-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:endo-data props)
    :spec assessment-specs/内分泌系统Spec
    :section-key :内分泌系统}])

(defn endocrine-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id endo-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :内分泌系统
      :spec                  assessment-specs/内分泌系统Spec
      :data                  endo-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-endocrine-system-spec")}]))

(defn endocrine-system-card "内分泌系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> ExperimentOutlined {:style {:marginRight "8px"}}]
           :title        "内分泌系统"
           :bg-color     "#f9f0ff"
           :data-sub     [::subs/endocrine-system-data]
           :data-key     :endo-data
           :summary-view endocrine-system-summary-view
           :detailed-view endocrine-system-detailed-view}
          props)])

;; Liver Kidney System Card
(defn- liver-kidney-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:lk-data props)
    :spec assessment-specs/肝肾病史Spec
    :section-key :肝肾病史}])

(defn liver-kidney-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id lk-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :肝肾病史
      :spec                  assessment-specs/肝肾病史Spec
      :data                  lk-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-liver-kidney-system-spec")}]))

(defn liver-kidney-system-card "肝肾病史" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> ProjectOutlined {:style {:marginRight "8px"}}]
           :title        "肝肾病史"
           :bg-color     "#fff7e6"
           :data-sub     [::subs/liver-kidney-system-data]
           :data-key     :lk-data
           :summary-view liver-kidney-system-summary-view
           :detailed-view liver-kidney-system-detailed-view}
          props)])

;; Digestive System Card
(defn- digestive-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:ds-data props)
    :spec assessment-specs/消化系统Spec
    :section-key :消化系统}])

(defn digestive-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id ds-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :消化系统
      :spec                  assessment-specs/消化系统Spec
      :data                  ds-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-digestive-system-spec")}]))

(defn digestive-system-card  "消化系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> CoffeeOutlined {:style {:marginRight "8px"}}]
           :title        "消化系统"
           :bg-color     "#eff8ff"
           :data-sub     [::subs/digestive-system-data]
           :data-key     :ds-data
           :summary-view digestive-system-summary-view
           :detailed-view digestive-system-detailed-view}
          props)])

;; Hematologic System Card
(defn- hematologic-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:hs-data props)
    :spec assessment-specs/血液系统Spec
    :section-key :血液系统}])

(defn hematologic-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id hs-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :血液系统
      :spec                  assessment-specs/血液系统Spec
      :data                  hs-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-hematologic-system-spec")}]))

(defn hematologic-system-card "血液系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> ExperimentOutlined {:style {:marginRight "8px"}}]
           :title        "血液系统"
           :bg-color     "#fff0f6"
           :data-sub     [::subs/hematologic-system-data]
           :data-key     :hs-data
           :summary-view hematologic-system-summary-view
           :detailed-view hematologic-system-detailed-view}
          props)])

;; Immune System Card
(defn- immune-system-summary-view [props]
  [as/assessment-summary-view
   {:data (:is-data props)
    :spec assessment-specs/免疫系统Spec
    :section-key :免疫系统}])

(defn immune-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id is-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :免疫系统
      :spec                  assessment-specs/免疫系统Spec
      :data                  is-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-immune-system-spec")}]))

(defn immune-system-card        "免疫系统" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
           :title        "免疫系统"
           :bg-color     "#f6ffed"
           :data-sub     [::subs/immune-system-data]
           :data-key     :is-data
           :summary-view immune-system-summary-view
           :detailed-view immune-system-detailed-view}
          props)])

;; Special Medication History Card
(defn- special-medication-history-summary-view [props]
  [as/assessment-summary-view
   {:data (:smh-data props)
    :spec assessment-specs/特殊用药史Spec
    :section-key :特殊用药史}])

(defn special-medication-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id smh-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :特殊用药史
      :spec                  assessment-specs/特殊用药史Spec
      :data                  smh-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-special-medication-history-spec")}]))

(defn special-medication-history-card "特殊用药史" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
           :title        "特殊用药史"
           :bg-color     "#fffbe6"
           :data-sub     [::subs/special-medication-history-data]
           :data-key     :smh-data
           :summary-view special-medication-history-summary-view
           :detailed-view special-medication-history-detailed-view}
          props)])

;; Special Disease History Card
(defn- special-disease-history-summary-view [props]
  [as/assessment-summary-view
   {:data (:sdh-data props)
    :spec assessment-specs/特殊疾病病史Spec
    :section-key :特殊疾病病史}])

(defn special-disease-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sdh-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :特殊疾病病史
      :spec                  assessment-specs/特殊疾病病史Spec
      :data                  sdh-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-special-disease-history-spec")}]))

(defn special-disease-history-card "特殊疾病病史" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> WarningOutlined {:style {:marginRight "8px"}}]
           :title        "特殊疾病病史"
           :bg-color     "#fff1f0"
           :data-sub     [::subs/special-disease-history-data]
           :data-key     :sdh-data
           :summary-view special-disease-history-summary-view
           :detailed-view special-disease-history-detailed-view}
          props)])

;; Nutritional Assessment Card
(defn- nutritional-assessment-summary-view [props]
  [as/assessment-summary-view
   {:data (:na-data props)
    :spec assessment-specs/营养评估Spec
    :section-key :营养评估}])

(defn nutritional-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id na-data on-show-summary]} props
        extra [:<>
               [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
                [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
                [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
               [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
                [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
                [:p {:style {:fontSize "12px" :color "gray"}}
                 "0 分：健康；" [:br]
                 "1-2 分：衰弱前期；" [:br]
                 "≥3 分：衰弱。"]]]]
    [adv/assessment-section-detailed-view
     {:section-key           :营养评估
      :spec                  assessment-specs/营养评估Spec
      :data                  na-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-nutritional-assessment-spec")
      :extra-content         extra}]))

(defn nutritional-assessment-card "营养评估" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> AppleOutlined {:style {:marginRight "8px"}}]
           :title        "营养评估"
           :bg-color     "#f0fff0"
           :data-sub     [::subs/nutritional-assessment-data]
           :data-key     :na-data
           :summary-view nutritional-assessment-summary-view
           :detailed-view nutritional-assessment-detailed-view}
          props)])

;; Pregnancy Assessment Card
(defn- pregnancy-assessment-summary-view [props]
  [as/assessment-summary-view
   {:data (:pa-data props)
    :spec assessment-specs/妊娠Spec
    :section-key :妊娠}])

(defn pregnancy-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id pa-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :妊娠
      :spec                  assessment-specs/妊娠Spec
      :data                  pa-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-pregnancy-assessment-spec")}]))

(defn pregnancy-assessment-card "妊娠" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> WomanOutlined {:style {:marginRight "8px"}}]
           :title        "妊娠"
           :bg-color     "#fff0f6"
           :data-sub     [::subs/pregnancy-assessment-data]
           :data-key     :pa-data
           :summary-view pregnancy-assessment-summary-view
           :detailed-view pregnancy-assessment-detailed-view}
          props)])

;; Surgical Anesthesia History Card
(defn- surgical-anesthesia-history-summary-view [props]
  [as/assessment-summary-view
   {:data (:sah-data props)
    :spec assessment-specs/手术麻醉史Spec
    :section-key :手术麻醉史}])

(defn surgical-anesthesia-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sah-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :手术麻醉史
      :spec                  assessment-specs/手术麻醉史Spec
      :data                  sah-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-surgical-anesthesia-history-spec")}]))

(defn surgical-anesthesia-history-card "手术麻醉史" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> HistoryOutlined {:style {:marginRight "8px"}}]
           :title        "手术麻醉史"
           :bg-color     "#e6f7ff"
           :data-sub     [::subs/surgical-anesthesia-history-data]
           :data-key     :sah-data
           :summary-view surgical-anesthesia-history-summary-view
           :detailed-view surgical-anesthesia-history-detailed-view}
          props)])

;; Airway Assessment Card
(defn- airway-assessment-summary-view [props]
  [as/assessment-summary-view
   {:data (:aa-data props)
    :spec assessment-specs/气道评估Spec
    :section-key :气道评估}])

(defn airway-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id aa-data on-show-summary]} props
        extra [:<>
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
               [:h4 {:style {:marginTop "16px" :borderTop "1px solid #eee" :paddingTop "10px"}} "详细评估项"]]
    [adv/assessment-section-detailed-view
     {:section-key           :气道评估
      :spec                  assessment-specs/气道评估Spec
      :data                  aa-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-airway-assessment-spec")
      :extra-content         extra}]))

(defn airway-assessment-card "气道评估" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
           :title        "气道评估"
           :bg-color     "#fff7e6"
           :data-sub     [::subs/airway-assessment-data]
           :data-key     :aa-data
           :summary-view airway-assessment-summary-view
           :detailed-view airway-assessment-detailed-view}
          props)])

;; Spinal Anesthesia Assessment Card
(defn- spinal-anesthesia-assessment-summary-view [props]
  [as/assessment-summary-view
   {:data (:saa-data props)
    :spec assessment-specs/椎管内麻醉相关评估Spec
    :section-key :椎管内麻醉相关评估}])

(defn spinal-anesthesia-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id saa-data on-show-summary]} props]
    [adv/assessment-section-detailed-view
     {:section-key           :椎管内麻醉相关评估
      :spec                  assessment-specs/椎管内麻醉相关评估Spec
      :data                  saa-data
      :patient-id            patient-id
      :report-form-instance-fn report-form-instance-fn
      :on-show-summary       on-show-summary
      :form-key              (str patient-id "-spinal-anesthesia-assessment-spec")}]))

(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" [props]
  [ac/assessment-section-card
   (merge {:icon         [:> GatewayOutlined {:style {:marginRight "8px"}}]
           :title        "椎管内麻醉相关评估"
           :bg-color     "#f0f5ff"
           :data-sub     [::subs/spinal-anesthesia-assessment-data]
           :data-key     :saa-data
           :summary-view spinal-anesthesia-assessment-summary-view
           :detailed-view spinal-anesthesia-assessment-detailed-view}
          props)])
