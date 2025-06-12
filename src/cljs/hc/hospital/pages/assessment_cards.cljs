(ns hc.hospital.pages.assessment-cards
  "评估表页面各个表单卡片的组合与布局。"
  (:require
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined
                                 HeartOutlined HistoryOutlined
                                 MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined
                                 UserOutlined WarningOutlined WomanOutlined]]
   ;; ["antd" :refer [Form Row Col]] ; Removed
   ;; ["react" :as React] ; Removed
   ;; [clojure.string :as str] ; Removed
   ;; [taoensso.timbre :as timbre] ; Removed
   ;; [malli.core :as m] ; Removed
   [hc.hospital.components.reusable-assessment-card :as reusable-card]
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
   [hc.hospital.events :as events] ; Still needed for update-event vector
   [hc.hospital.subs :as subs] ; Still needed for data-sub vector
   ;; [hc.hospital.utils :as utils] ; Removed
   ;; [hc.hospital.ui-helpers :as ui-helpers] ; Removed
   ;; [hc.hospital.natural-language-generators :as nlg] ; Removed
   ;; [hc.hospital.form-utils :as form-utils] ; Removed
   [re-frame.core :as rf] ; Kept for safety, event/sub keywords might need it.
   ;; [reagent.core :as r] ; Removed
   ))

(defn circulatory-system-card "循环系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :循环系统
    :title "循环系统"
    :icon [:> HeartOutlined {:style {:marginRight "8px"}}]
    :color "#e6f7ff"
    :spec assessment-specs/循环系统Spec
    :data-sub [::subs/circulatory-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

(defn respiratory-system-card "呼吸系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :呼吸系统
    :title "呼吸系统"
    :icon [:> CloudOutlined {:style {:marginRight "8px"}}]
    :color "#e6fffb"
    :spec assessment-specs/呼吸系统Spec
    :data-sub [::subs/respiratory-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Mental Neuromuscular System Card
(defn mental-neuromuscular-system-card "精神及神经肌肉系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :精神及神经肌肉系统
    :title "精神及神经肌肉系统"
    :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
    :color "#f6ffed"
    :spec assessment-specs/精神及神经肌肉系统Spec
    :data-sub [::subs/mental-neuromuscular-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Endocrine System Card
(defn endocrine-system-card "内分泌系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :内分泌系统
    :title "内分泌系统"
    :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
    :color "#f9f0ff"
    :spec assessment-specs/内分泌系统Spec
    :data-sub [::subs/endocrine-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Liver Kidney System Card
(defn liver-kidney-system-card "肝肾病史" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :肝肾病史
    :title "肝肾病史"
    :icon [:> ProjectOutlined {:style {:marginRight "8px"}}]
    :color "#fff7e6"
    :spec assessment-specs/肝肾病史Spec
    :data-sub [::subs/liver-kidney-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Digestive System Card
(defn digestive-system-card  "消化系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :消化系统
    :title "消化系统"
    :icon [:> CoffeeOutlined {:style {:marginRight "8px"}}]
    :color "#eff8ff"
    :spec assessment-specs/消化系统Spec
    :data-sub [::subs/digestive-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Hematologic System Card
(defn hematologic-system-card "血液系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :血液系统
    :title "血液系统"
    :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}] ;; Same icon as Endocrine
    :color "#fff0f6"
    :spec assessment-specs/血液系统Spec
    :data-sub [::subs/hematologic-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Immune System Card
(defn immune-system-card        "免疫系统" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :免疫系统
    :title "免疫系统"
    :icon [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
    :color "#f6ffed" ;; Same color as Mental Neuromuscular
    :spec assessment-specs/免疫系统Spec
    :data-sub [::subs/immune-system-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Special Medication History Card
(defn special-medication-history-card "特殊用药史" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :特殊用药史
    :title "特殊用药史"
    :icon [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
    :color "#fffbe6"
    :spec assessment-specs/特殊用药史Spec
    :data-sub [::subs/special-medication-history-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Special Disease History Card
(defn special-disease-history-card "特殊疾病病史" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :特殊疾病病史
    :title "特殊疾病病史"
    :icon [:> WarningOutlined {:style {:marginRight "8px"}}]
    :color "#fff1f0"
    :spec assessment-specs/特殊疾病病史Spec
    :data-sub [::subs/special-disease-history-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None for this card
    }])

;; Nutritional Assessment Card
(def nutritional-assessment-static-content
  [:<>
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
    [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
    [:p {:style {:fontSize "12px" :color "gray"}}
     "0 分：健康；" [:br]
     "1-2 分：衰弱前期；" [:br]
     "≥3 分：衰弱。"]] ; Closes p and the second div
   ] ; Closes outer :<> fragment
) ; Closes def

(defn nutritional-assessment-card "营养评估" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :营养评估
    :title "营养评估"
    :icon [:> AppleOutlined {:style {:marginRight "8px"}}]
    :color "#f0fff0"
    :spec assessment-specs/营养评估Spec
    :data-sub [::subs/nutritional-assessment-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    :static-content nutritional-assessment-static-content
    ;; :summary-view-fn - Omitted to use default summary generation
    }])

;; Pregnancy Assessment Card
(defn pregnancy-assessment-card "妊娠" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :妊娠
    :title "妊娠"
    :icon [:> WomanOutlined {:style {:marginRight "8px"}}]
    :color "#fff0f6" ;; Same as Hematologic
    :spec assessment-specs/妊娠Spec
    :data-sub [::subs/pregnancy-assessment-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None
    }])

;; Surgical Anesthesia History Card
(defn surgical-anesthesia-history-card "手术麻醉史" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :手术麻醉史
    :title "手术麻醉史"
    :icon [:> HistoryOutlined {:style {:marginRight "8px"}}]
    :color "#e6f7ff" ;; Same as Circulatory
    :spec assessment-specs/手术麻醉史Spec
    :data-sub [::subs/surgical-anesthesia-history-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None
    }])

;; Airway Assessment Card
(def airway-assessment-static-content
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
  [reusable-card/reusable-assessment-card
   {:assessment-key :气道评估
    :title "气道评估"
    :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}] ;; Same as Mental Neuromuscular
    :color "#fff7e6" ;; Same as Liver Kidney
    :spec assessment-specs/气道评估Spec
    :data-sub [::subs/airway-assessment-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    :static-content airway-assessment-static-content
    ;; :summary-view-fn - Omitted to use default summary generation
    }])

;; Spinal Anesthesia Assessment Card
(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" [props]
  [reusable-card/reusable-assessment-card
   {:assessment-key :椎管内麻醉相关评估
    :title "椎管内麻醉相关评估"
    :icon [:> GatewayOutlined {:style {:marginRight "8px"}}]
    :color "#f0f5ff"
    :spec assessment-specs/椎管内麻醉相关评估Spec
    :data-sub [::subs/spinal-anesthesia-assessment-data]
    :update-event [::events/update-canonical-assessment-section]
    :report-form-instance-fn (:report-form-instance-fn props)
    ;; :summary-view-fn - Omitted to use default summary generation
    ;; :static-content - None
    }])
