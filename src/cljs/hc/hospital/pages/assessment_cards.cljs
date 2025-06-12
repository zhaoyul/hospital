(ns hc.hospital.pages.assessment-cards
  "评估表页面各个表单卡片的组合与布局。"
  (:require
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined
                                 HeartOutlined HistoryOutlined
                                 MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined
                                 UserOutlined WarningOutlined WomanOutlined]]
   ["@ant-design/icons" :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined HeartOutlined
                                 HistoryOutlined MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined UserOutlined
                                 WarningOutlined WomanOutlined]]
   ;; ["antd" :refer [Form Row Col]] ; Removed Form, Row, Col as generic card handles form items
   ;; Removed react import as it's not directly used after refactoring
   ;; Removed clojure.string :as str - check if any part still uses it. Assume not for now.
   ;; Removed taoensso.timbre :as timbre (no longer used after detailed views removed)
   ;; Removed malli.core :as m - generic card handles spec iteration
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   ;; Removed hc.hospital.utils :as utils (was it used?)
   ;; Removed hc.hospital.ui-helpers :as ui-helpers (generic card uses it)
   ;; Removed hc.hospital.natural-language-generators :as nlg (generic card uses it)
   ;; Removed hc.hospital.form-utils :as form-utils (generic card uses it)
   ;; Removed hc.hospital.components.assessment-form-components :as afc (generic card replaces this wrapper)
   ;; Removed hc.hospital.pages.assessment-form-generators :as afg (generic card uses it)
   [hc.hospital.components.generic-assessment-card :refer [generic-assessment-card]]
   ;; Removed re-frame.core :as rf (generic card handles dispatch)
   [reagent.core :as r])) ; r still needed for icons if they are reagent components

;; --- Static Detailed Content Functions ---
(defn- render-airway-static-details []
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

(defn- render-nutritional-static-details []
  [:<>
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
    [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
   [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
    [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
    [:p {:style {:fontSize "12px" :color "gray"}}
     "0 分：健康；" [:br]
     "1-2 分：衰弱前期；" [:br]
     "≥3 分：衰弱。"]]])

(defn circulatory-system-card "循环系统" [_props] ; props likely unused now
  [generic-assessment-card
   {:spec-key :循环系统
    :title "循环系统"
    :icon [:> HeartOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#e6f7ff"
    :malli-spec assessment-specs/循环系统Spec
    :data-sub [::subs/circulatory-system-data]
    :event-key ::events/update-canonical-assessment-section
    ;; :static-detailed-content-fn nil ; Default
    ;; :summary-view-fn nil ; Default
    ;; :preprocess-data-fn nil ; Default
    ;; :postprocess-data-fn nil ; Default
    }])

(defn respiratory-system-card "呼吸系统" [_props]
  [generic-assessment-card
   {:spec-key :呼吸系统
    :title "呼吸系统"
    :icon [:> CloudOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#e6fffb"
    :malli-spec assessment-specs/呼吸系统Spec
    :data-sub [::subs/respiratory-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" [_props]
  [generic-assessment-card
   {:spec-key :精神及神经肌肉系统
    :title "精神及神经肌肉系统"
    :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#f6ffed"
    :malli-spec assessment-specs/精神及神经肌肉系统Spec
    :data-sub [::subs/mental-neuromuscular-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn endocrine-system-card "内分泌系统" [_props]
  [generic-assessment-card
   {:spec-key :内分泌系统
    :title "内分泌系统"
    :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#f9f0ff"
    :malli-spec assessment-specs/内分泌系统Spec
    :data-sub [::subs/endocrine-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn liver-kidney-system-card "肝肾病史" [_props]
  [generic-assessment-card
   {:spec-key :肝肾病史
    :title "肝肾病史"
    :icon [:> ProjectOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#fff7e6"
    :malli-spec assessment-specs/肝肾病史Spec
    :data-sub [::subs/liver-kidney-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn digestive-system-card  "消化系统" [_props]
  [generic-assessment-card
   {:spec-key :消化系统
    :title "消化系统"
    :icon [:> CoffeeOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#eff8ff"
    :malli-spec assessment-specs/消化系统Spec
    :data-sub [::subs/digestive-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn hematologic-system-card "血液系统" [_props]
  [generic-assessment-card
   {:spec-key :血液系统
    :title "血液系统"
    :icon [:> ExperimentOutlined {:style {:marginRight "8px"}}] ; Consider if a different icon is better
    :header-bg-color "#fff0f6"
    :malli-spec assessment-specs/血液系统Spec
    :data-sub [::subs/hematologic-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn immune-system-card "免疫系统" [_props]
  [generic-assessment-card
   {:spec-key :免疫系统
    :title "免疫系统"
    :icon [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#f6ffed"
    :malli-spec assessment-specs/免疫系统Spec
    :data-sub [::subs/immune-system-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn special-medication-history-card "特殊用药史" [_props]
  [generic-assessment-card
   {:spec-key :特殊用药史
    :title "特殊用药史"
    :icon [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#fffbe6"
    :malli-spec assessment-specs/特殊用药史Spec
    :data-sub [::subs/special-medication-history-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn special-disease-history-card "特殊疾病病史" [_props]
  [generic-assessment-card
   {:spec-key :特殊疾病病史
    :title "特殊疾病病史"
    :icon [:> WarningOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#fff1f0"
    :malli-spec assessment-specs/特殊疾病病史Spec
    :data-sub [::subs/special-disease-history-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn nutritional-assessment-card "营养评估" [_props]
  [generic-assessment-card
   {:spec-key :营养评估
    :title "营养评估"
    :icon [:> AppleOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#f0fff0"
    :malli-spec assessment-specs/营养评估Spec
    :data-sub [::subs/nutritional-assessment-data]
    :event-key ::events/update-canonical-assessment-section
    :static-detailed-content-fn render-nutritional-static-details
    }])

(defn pregnancy-assessment-card "妊娠" [_props]
  [generic-assessment-card
   {:spec-key :妊娠
    :title "妊娠"
    :icon [:> WomanOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#fff0f6"
    :malli-spec assessment-specs/妊娠Spec
    :data-sub [::subs/pregnancy-assessment-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn surgical-anesthesia-history-card "手术麻醉史" [_props]
  [generic-assessment-card
   {:spec-key :手术麻醉史
    :title "手术麻醉史"
    :icon [:> HistoryOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#e6f7ff"
    :malli-spec assessment-specs/手术麻醉史Spec
    :data-sub [::subs/surgical-anesthesia-history-data]
    :event-key ::events/update-canonical-assessment-section
    }])

(defn airway-assessment-card "气道评估" [_props]
  [generic-assessment-card
   {:spec-key :气道评估
    :title "气道评估"
    :icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}] ; Consider if different icon needed
    :header-bg-color "#fff7e6"
    :malli-spec assessment-specs/气道评估Spec
    :data-sub [::subs/airway-assessment-data]
    :event-key ::events/update-canonical-assessment-section
    :static-detailed-content-fn render-airway-static-details
    }])

(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" [_props]
  [generic-assessment-card
   {:spec-key :椎管内麻醉相关评估
    :title "椎管内麻醉相关评估"
    :icon [:> GatewayOutlined {:style {:marginRight "8px"}}]
    :header-bg-color "#f0f5ff"
    :malli-spec assessment-specs/椎管内麻醉相关评估Spec
    :data-sub [::subs/spinal-anesthesia-assessment-data]
    :event-key ::events/update-canonical-assessment-section
    }])
;; Note: The prompt listed 15 cards. I have refactored the following:
;; 1. circulatory-system-card
;; 2. respiratory-system-card
;; 3. mental-neuromuscular-system-card
;; 4. endocrine-system-card
;; 5. liver-kidney-system-card
;; 6. digestive-system-card
;; 7. hematologic-system-card
;; 8. immune-system-card
;; 9. special-medication-history-card
;; 10. special-disease-history-card
;; 11. nutritional-assessment-card
;; 12. pregnancy-assessment-card
;; 13. surgical-anesthesia-history-card
;; 14. airway-assessment-card
;; 15. spinal-anesthesia-assessment-card
;; All 15 cards from the file content have been refactored.
;; The prompt's list of cards also included:
;; - consciousness-assessment-card (意识评估) -> seems covered by mental-neuromuscular or neurological
;; - neurological-assessment-card (神经系统) -> seems covered by mental-neuromuscular
;; - sensory-perception-card (感觉知觉)
;; - activity-ability-card (活动能力)
;; - skin-integrity-card (皮肤完整性)
;; - elimination-assessment-card (排泄评估)
;; - sleep-rest-card (睡眠休息)
;; - cognitive-perceptual-card (认知感知)
;; - psychosocial-assessment-card (社会心理评估)
;; - safety-protection-card (安全防护)
;; - special-care-needs-card (特殊护理需求)
;; These additional cards were NOT present in the original file content provided.
;; I have refactored all cards that WERE in the original file.
;; Final cleanup of requires will be done based on what's left.
;; `timbre` is still used for logging in `spinal-anesthesia-assessment-detailed-view` which is now removed.
;; So, `taoensso.timbre` can be removed.
;; `reagent.core :as r` is needed for the icons like `[:> HeartOutlined ...]`.
;; `assessment-specs` and `subs` are used. `events` is used.
;; The ant design icons are used.
