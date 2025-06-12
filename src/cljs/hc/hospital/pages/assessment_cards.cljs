(ns hc.hospital.pages.assessment-cards
  "评估表页面各个表单卡片的组合与布局。"
  (:require ["@ant-design/icons" :refer [HeartOutlined CloudOutlined NodeIndexOutlined
                                           ExperimentOutlined ProjectOutlined CoffeeOutlined
                                           HistoryOutlined MedicineBoxOutlined SecurityScanOutlined
                                           WarningOutlined AppleOutlined WomanOutlined GatewayOutlined]]
            [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
            [hc.hospital.pages.assessment-card-common :as common]
            [hc.hospital.subs :as subs]))

(defn circulatory-system-card [props]
  [common/assessment-card (merge {:section-key :循环系统
                                  :title "循环系统"
                                  :icon [:> HeartOutlined {:style {:marginRight "8px"}}]
                                  :color "#e6f7ff"
                                  :spec assessment-specs/循环系统Spec
                                  :data-sub [::subs/circulatory-system-data]}
                                 props)])

(defn respiratory-system-card [props]
  [common/assessment-card (merge {:section-key :呼吸系统
                                  :title "呼吸系统"
                                  :icon [:> CloudOutlined {:style {:marginRight "8px"}}]
                                  :color "#e6fffb"
                                  :spec assessment-specs/呼吸系统Spec
                                  :data-sub [::subs/respiratory-system-data]}
                                 props)])

(defn mental-neuromuscular-system-card [props]
  [common/assessment-card (merge {:section-key :精神及神经肌肉系统
                                  :title "精神及神经肌肉系统"
                                  :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
                                  :color "#f6ffed"
                                  :spec assessment-specs/精神及神经肌肉系统Spec
                                  :data-sub [::subs/mental-neuromuscular-system-data]}
                                 props)])

(defn endocrine-system-card [props]
  [common/assessment-card (merge {:section-key :内分泌系统
                                  :title "内分泌系统"
                                  :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
                                  :color "#f9f0ff"
                                  :spec assessment-specs/内分泌系统Spec
                                  :data-sub [::subs/endocrine-system-data]}
                                 props)])

(defn liver-kidney-system-card [props]
  [common/assessment-card (merge {:section-key :肝肾病史
                                  :title "肝肾病史"
                                  :icon [:> ProjectOutlined {:style {:marginRight "8px"}}]
                                  :color "#fff7e6"
                                  :spec assessment-specs/肝肾病史Spec
                                  :data-sub [::subs/liver-kidney-system-data]}
                                 props)])

(defn digestive-system-card [props]
  [common/assessment-card (merge {:section-key :消化系统
                                  :title "消化系统"
                                  :icon [:> CoffeeOutlined {:style {:marginRight "8px"}}]
                                  :color "#eff8ff"
                                  :spec assessment-specs/消化系统Spec
                                  :data-sub [::subs/digestive-system-data]}
                                 props)])

(defn hematologic-system-card [props]
  [common/assessment-card (merge {:section-key :血液系统
                                  :title "血液系统"
                                  :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
                                  :color "#fff0f6"
                                  :spec assessment-specs/血液系统Spec
                                  :data-sub [::subs/hematologic-system-data]}
                                 props)])

(defn immune-system-card [props]
  [common/assessment-card (merge {:section-key :免疫系统
                                  :title "免疫系统"
                                  :icon [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
                                  :color "#f6ffed"
                                  :spec assessment-specs/免疫系统Spec
                                  :data-sub [::subs/immune-system-data]}
                                 props)])

(defn special-medication-history-card [props]
  [common/assessment-card (merge {:section-key :特殊用药史
                                  :title "特殊用药史"
                                  :icon [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
                                  :color "#fffbe6"
                                  :spec assessment-specs/特殊用药史Spec
                                  :data-sub [::subs/special-medication-history-data]}
                                 props)])

(defn special-disease-history-card [props]
  [common/assessment-card (merge {:section-key :特殊疾病病史
                                  :title "特殊疾病病史"
                                  :icon [:> WarningOutlined {:style {:marginRight "8px"}}]
                                  :color "#fff1f0"
                                  :spec assessment-specs/特殊疾病病史Spec
                                  :data-sub [::subs/special-disease-history-data]}
                                 props)])

(defn nutritional-assessment-card [props]
  [common/assessment-card (merge {:section-key :营养评估
                                  :title "营养评估"
                                  :icon [:> AppleOutlined {:style {:marginRight "8px"}}]
                                  :color "#f0fff0"
                                  :spec assessment-specs/营养评估Spec
                                  :data-sub [::subs/nutritional-assessment-data]}
                                 props)])

(defn pregnancy-assessment-card [props]
  [common/assessment-card (merge {:section-key :妊娠
                                  :title "妊娠"
                                  :icon [:> WomanOutlined {:style {:marginRight "8px"}}]
                                  :color "#fff0f6"
                                  :spec assessment-specs/妊娠Spec
                                  :data-sub [::subs/pregnancy-assessment-data]}
                                 props)])

(defn surgical-anesthesia-history-card [props]
  [common/assessment-card (merge {:section-key :手术麻醉史
                                  :title "手术麻醉史"
                                  :icon [:> HistoryOutlined {:style {:marginRight "8px"}}]
                                  :color "#e6f7ff"
                                  :spec assessment-specs/手术麻醉史Spec
                                  :data-sub [::subs/surgical-anesthesia-history-data]}
                                 props)])

(defn airway-assessment-card [props]
  [common/assessment-card (merge {:section-key :气道评估
                                  :title "气道评估"
                                  :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
                                  :color "#fff7e6"
                                  :spec assessment-specs/气道评估Spec
                                  :data-sub [::subs/airway-assessment-data]}
                                 props)])

(defn spinal-anesthesia-assessment-card [props]
  [common/assessment-card (merge {:section-key :椎管内麻醉相关评估
                                  :title "椎管内麻醉相关评估"
                                  :icon [:> GatewayOutlined {:style {:marginRight "8px"}}]
                                  :color "#f0f5ff"
                                  :spec assessment-specs/椎管内麻醉相关评估Spec
                                  :data-sub [::subs/spinal-anesthesia-assessment-data]}
                                 props)])
