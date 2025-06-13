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
  [hc.hospital.natural-language-generators :as nlg]
  [hc.hospital.components.summary-components :as summary]
  [hc.hospital.form-utils :as form-utils]
  [re-frame.core :as rf]
  [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

;; ---------------------------------------------------------------------------
;; 通用详细表单视图生成器
;; ---------------------------------------------------------------------------
(defn create-detailed-view
  "根据传入配置生成标准的详细评估表单视图组件。"
  [{:keys [section-key section-spec data-prop form-key-suffix
           preprocess-dates? watch-data? extra-content]}]
  (fn [props]
    (let [{:keys [report-form-instance-fn patient-id on-show-summary]} props
          section-data (get props data-prop)
          [form] (Form.useForm)
          data-with-enums (form-utils/apply-enum-defaults-to-data
                           (or section-data {})
                           section-spec)
          initial-form-values (if preprocess-dates?
                                (form-utils/preprocess-date-fields
                                 data-with-enums section-spec)
                                data-with-enums)
          on-finish-fn (fn [values]
                         (let [values-clj (js->clj values :keywordize-keys true)
                               final-values (if preprocess-dates?
                                              (form-utils/transform-date-fields-for-submission
                                               values-clj section-spec)
                                              values-clj)]
                           (rf/dispatch [::events/update-canonical-assessment-section
                                         section-key final-values])))]
      (React/useEffect
       (fn []
         (when report-form-instance-fn
           (report-form-instance-fn section-key form))
         js/undefined)
       #js [])
      (when watch-data?
        (React/useEffect
         (fn []
           (.resetFields form)
           (.setFieldsValue form (clj->js initial-form-values))
           js/undefined)
         #js [initial-form-values]))
      (let [form-items (into [:<>]
                             (mapv (fn [[field-key field-schema optional? _]]
                                     (afg/render-form-item-from-spec
                                      [field-key field-schema optional? [] form]))
                                   (m/entries section-spec)))]
        [afc/patient-assessment-card-wrapper
         {:patient-id patient-id
          :form-instance form
          :form-key (str patient-id "-" form-key-suffix)
          :initial-data initial-form-values
          :on-finish-handler on-finish-fn
          :children
          [:<>
           form-items
           extra-content
           [:> Row {:justify "end" :style {:marginTop "20px"}}
            [:> Col
             [:> Form.Item
              [:button {:type "button"
                        :on-click on-show-summary
                        :style {:padding "5px 10px"
                                :background-color "#f0f0f0"
                                :border "1px solid #ccc"
                                :border-radius "4px"
                                :cursor "pointer"}}
               "返回总结"]]]]]}]))))


(defn- circulatory-system-summary-view [props]
  (let [{:keys [circulatory-data]} props]
    [summary/assessment-summary {:data circulatory-data
                                 :schema assessment-specs/循环系统Spec
                                 :section-key :循环系统}]))

(def circulatory-system-detailed-view
  (create-detailed-view {:section-key :循环系统
                         :section-spec assessment-specs/循环系统Spec
                         :data-prop :circulatory-data
                         :form-key-suffix "circulatory-system-spec"
                         :preprocess-dates? true}))

(defn circulatory-system-card "循环系统" [props]
  (let [view-state (r/atom :summary) ; Manages :summary or :detailed view
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        circulatory-data @(rf/subscribe [::subs/circulatory-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> HeartOutlined {:style {:marginRight "8px"}}]
       "循环系统"
       "#e6f7ff"
       (if (= @view-state :summary)
         [circulatory-system-summary-view {:circulatory-data circulatory-data}]
         [:f> circulatory-system-detailed-view (merge props {:patient-id patient-id
                                                             :circulatory-data circulatory-data
                                                             :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

(defn- respiratory-system-summary-view [props]
  (let [{:keys [respiratory-data]} props]
    [summary/assessment-summary {:data respiratory-data
                                 :schema assessment-specs/呼吸系统Spec
                                 :section-key :呼吸系统}]))

(def respiratory-system-detailed-view
  (create-detailed-view {:section-key :呼吸系统
                         :section-spec assessment-specs/呼吸系统Spec
                         :data-prop :respiratory-data
                         :form-key-suffix "respiratory-system-spec"
                         :preprocess-dates? true}))

(defn respiratory-system-card "呼吸系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        respiratory-data @(rf/subscribe [::subs/respiratory-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> CloudOutlined {:style {:marginRight "8px"}}]
       "呼吸系统"
       "#e6fffb"
       (if (= @view-state :summary)
         [respiratory-system-summary-view {:respiratory-data respiratory-data}]
         [:f> respiratory-system-detailed-view (merge props {:patient-id patient-id
                                                             :respiratory-data respiratory-data
                                                             :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Mental Neuromuscular System Card
(defn- mental-neuromuscular-system-summary-view [props]
  (let [{:keys [mn-data]} props]
    [summary/assessment-summary {:data mn-data
                                 :schema assessment-specs/精神及神经肌肉系统Spec
                                 :section-key :精神及神经肌肉系统}]))

(def mental-neuromuscular-system-detailed-view
  (create-detailed-view {:section-key :精神及神经肌肉系统
                         :section-spec assessment-specs/精神及神经肌肉系统Spec
                         :data-prop :mn-data
                         :form-key-suffix "mental-neuromuscular-system-spec"
                         :preprocess-dates? true
                         :watch-data? true}))

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        mn-data @(rf/subscribe [::subs/mental-neuromuscular-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
       "精神及神经肌肉系统"
       "#f6ffed"
       (if (= @view-state :summary)
         [mental-neuromuscular-system-summary-view {:mn-data mn-data}]
         [:f> mental-neuromuscular-system-detailed-view (merge props {:patient-id patient-id
                                                                      :mn-data mn-data
                                                                      :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Endocrine System Card
(defn- endocrine-system-summary-view [props]
  (let [{:keys [endo-data]} props]
    [summary/assessment-summary {:data endo-data
                                 :schema assessment-specs/内分泌系统Spec
                                 :section-key :内分泌系统}]))

(def endocrine-system-detailed-view
  (create-detailed-view {:section-key :内分泌系统
                         :section-spec assessment-specs/内分泌系统Spec
                         :data-prop :endo-data
                         :form-key-suffix "endocrine-system-spec"}))

(defn endocrine-system-card "内分泌系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        endo-data @(rf/subscribe [::subs/endocrine-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> ExperimentOutlined {:style {:marginRight "8px"}}]
       "内分泌系统"
       "#f9f0ff"
       (if (= @view-state :summary)
         [endocrine-system-summary-view {:endo-data endo-data}]
         [:f> endocrine-system-detailed-view (merge props {:patient-id patient-id
                                                           :endo-data endo-data
                                                           :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Liver Kidney System Card
(defn- liver-kidney-system-summary-view [props]
  (let [{:keys [lk-data]} props]
    [summary/assessment-summary {:data lk-data
                                 :schema assessment-specs/肝肾病史Spec
                                 :section-key :肝肾病史}]))

(def liver-kidney-system-detailed-view
  (create-detailed-view {:section-key :肝肾病史
                         :section-spec assessment-specs/肝肾病史Spec
                         :data-prop :lk-data
                         :form-key-suffix "liver-kidney-system-spec"
                         :preprocess-dates? true}))

(defn liver-kidney-system-card "肝肾病史" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        lk-data @(rf/subscribe [::subs/liver-kidney-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> ProjectOutlined {:style {:marginRight "8px"}}]
       "肝肾病史"
       "#fff7e6"
       (if (= @view-state :summary)
         [liver-kidney-system-summary-view {:lk-data lk-data}]
         [:f> liver-kidney-system-detailed-view (merge props {:patient-id patient-id
                                                              :lk-data lk-data
                                                              :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Digestive System Card
(defn- digestive-system-summary-view [props]
  (let [{:keys [ds-data]} props]
    [summary/assessment-summary {:data ds-data
                                 :schema assessment-specs/消化系统Spec
                                 :section-key :消化系统}]))

(def digestive-system-detailed-view
  (create-detailed-view {:section-key :消化系统
                         :section-spec assessment-specs/消化系统Spec
                         :data-prop :ds-data
                         :form-key-suffix "digestive-system-spec"}))

(defn digestive-system-card  "消化系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        ds-data @(rf/subscribe [::subs/digestive-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> CoffeeOutlined {:style {:marginRight "8px"}}]
       "消化系统"
       "#eff8ff"
       (if (= @view-state :summary)
         [digestive-system-summary-view {:ds-data ds-data}]
         [:f> digestive-system-detailed-view (merge props {:patient-id patient-id
                                                           :ds-data ds-data
                                                           :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Hematologic System Card
(defn- hematologic-system-summary-view [props]
  (let [{:keys [hs-data]} props]
    [summary/assessment-summary {:data hs-data
                                 :schema assessment-specs/血液系统Spec
                                 :section-key :血液系统}]))

(def hematologic-system-detailed-view
  (create-detailed-view {:section-key :血液系统
                         :section-spec assessment-specs/血液系统Spec
                         :data-prop :hs-data
                         :form-key-suffix "hematologic-system-spec"}))

(defn hematologic-system-card "血液系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        hs-data @(rf/subscribe [::subs/hematologic-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> ExperimentOutlined {:style {:marginRight "8px"}}]
       "血液系统"
       "#fff0f6"
       (if (= @view-state :summary)
         [hematologic-system-summary-view {:hs-data hs-data}]
         [:f> hematologic-system-detailed-view (merge props {:patient-id patient-id
                                                             :hs-data hs-data
                                                             :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Immune System Card
(defn- immune-system-summary-view [props]
  (let [{:keys [is-data]} props]
    [summary/assessment-summary {:data is-data
                                 :schema assessment-specs/免疫系统Spec
                                 :section-key :免疫系统}]))

(def immune-system-detailed-view
  (create-detailed-view {:section-key :免疫系统
                         :section-spec assessment-specs/免疫系统Spec
                         :data-prop :is-data
                         :form-key-suffix "immune-system-spec"}))

(defn immune-system-card        "免疫系统" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        is-data @(rf/subscribe [::subs/immune-system-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
       "免疫系统"
       "#f6ffed"
       (if (= @view-state :summary)
         [immune-system-summary-view {:is-data is-data}]
         [:f> immune-system-detailed-view (merge props {:patient-id patient-id
                                                        :is-data is-data
                                                        :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Special Medication History Card
(defn- special-medication-history-summary-view [props]
  (let [{:keys [smh-data]} props]
    [summary/assessment-summary {:data smh-data
                                 :schema assessment-specs/特殊用药史Spec
                                 :section-key :特殊用药史}]))

(def special-medication-history-detailed-view
  (create-detailed-view {:section-key :特殊用药史
                         :section-spec assessment-specs/特殊用药史Spec
                         :data-prop :smh-data
                         :form-key-suffix "special-medication-history-spec"}))

(defn special-medication-history-card "特殊用药史" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        smh-data @(rf/subscribe [::subs/special-medication-history-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
       "特殊用药史"
       "#fffbe6"
       (if (= @view-state :summary)
         [special-medication-history-summary-view {:smh-data smh-data}]
         [:f> special-medication-history-detailed-view (merge props {:patient-id patient-id
                                                                     :smh-data smh-data
                                                                     :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Special Disease History Card
(defn- special-disease-history-summary-view [props]
  (let [{:keys [sdh-data]} props]
    [summary/assessment-summary {:data sdh-data
                                 :schema assessment-specs/特殊疾病病史Spec
                                 :section-key :特殊疾病病史}]))

(def special-disease-history-detailed-view
  (create-detailed-view {:section-key :特殊疾病病史
                         :section-spec assessment-specs/特殊疾病病史Spec
                         :data-prop :sdh-data
                         :form-key-suffix "special-disease-history-spec"}))

(defn special-disease-history-card "特殊疾病病史" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        sdh-data @(rf/subscribe [::subs/special-disease-history-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> WarningOutlined {:style {:marginRight "8px"}}]
       "特殊疾病病史"
       "#fff1f0"
       (if (= @view-state :summary)
         [special-disease-history-summary-view {:sdh-data sdh-data}]
         [:f> special-disease-history-detailed-view (merge props {:patient-id patient-id
                                                                  :sdh-data sdh-data
                                                                  :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Nutritional Assessment Card
(defn- nutritional-assessment-summary-view [props]
  (let [{:keys [na-data]} props]
    [summary/assessment-summary {:data na-data
                                 :schema assessment-specs/营养评估Spec
                                 :section-key :营养评估}]))

(def nutritional-assessment-detailed-view
  (create-detailed-view {:section-key :营养评估
                         :section-spec assessment-specs/营养评估Spec
                         :data-prop :na-data
                         :form-key-suffix "nutritional-assessment-spec"
                         :extra-content [:<>
                                       [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
                                        [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
                                        [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
                                       [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
                                        [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
                                        [:p {:style {:fontSize "12px" :color "gray"}} "0 分：健康；" [:br] "1-2 分：衰弱前期；" [:br] "≥3 分：衰弱。"]]]}))

(defn nutritional-assessment-card "营养评估" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        na-data @(rf/subscribe [::subs/nutritional-assessment-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> AppleOutlined {:style {:marginRight "8px"}}]
       "营养评估"
       "#f0fff0"
       (if (= @view-state :summary)
         [nutritional-assessment-summary-view {:na-data na-data}]
         [:f> nutritional-assessment-detailed-view (merge props {:patient-id patient-id
                                                                 :na-data na-data
                                                                 :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Pregnancy Assessment Card
(defn- pregnancy-assessment-summary-view [props]
  (let [{:keys [pa-data]} props]
    [summary/assessment-summary {:data pa-data
                                 :schema assessment-specs/妊娠Spec
                                 :section-key :妊娠}]))

(def pregnancy-assessment-detailed-view
  (create-detailed-view {:section-key :妊娠
                         :section-spec assessment-specs/妊娠Spec
                         :data-prop :pa-data
                         :form-key-suffix "pregnancy-assessment-spec"}))

(defn pregnancy-assessment-card "妊娠" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        pa-data @(rf/subscribe [::subs/pregnancy-assessment-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> WomanOutlined {:style {:marginRight "8px"}}]
       "妊娠"
       "#fff0f6"
       (if (= @view-state :summary)
         [pregnancy-assessment-summary-view {:pa-data pa-data}]
         [:f> pregnancy-assessment-detailed-view (merge props {:patient-id patient-id
                                                               :pa-data pa-data
                                                               :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Surgical Anesthesia History Card
(defn- surgical-anesthesia-history-summary-view [props]
  (let [{:keys [sah-data]} props]
    [summary/assessment-summary {:data sah-data
                                 :schema assessment-specs/手术麻醉史Spec
                                 :section-key :手术麻醉史}]))

(def surgical-anesthesia-history-detailed-view
  (create-detailed-view {:section-key :手术麻醉史
                         :section-spec assessment-specs/手术麻醉史Spec
                         :data-prop :sah-data
                         :form-key-suffix "surgical-anesthesia-history-spec"
                         :preprocess-dates? true}))

(defn surgical-anesthesia-history-card "手术麻醉史" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        sah-data @(rf/subscribe [::subs/surgical-anesthesia-history-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> HistoryOutlined {:style {:marginRight "8px"}}]
       "手术麻醉史"
       "#e6f7ff"
       (if (= @view-state :summary)
         [surgical-anesthesia-history-summary-view {:sah-data sah-data}]
         [:f> surgical-anesthesia-history-detailed-view (merge props {:patient-id patient-id
                                                                      :sah-data sah-data
                                                                      :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Airway Assessment Card
(defn- airway-assessment-summary-view [props]
  (let [{:keys [aa-data]} props]
    [summary/assessment-summary {:data aa-data
                                 :schema assessment-specs/气道评估Spec
                                 :section-key :气道评估}]))

(defn airway-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id aa-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or aa-data {})
                              assessment-specs/气道评估Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :气道评估 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :气道评估 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/气道评估Spec
          dynamically-generated-form-items
          (into [:<>] (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))
          static-content [:<>
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
                          [:h4 {:style {:marginTop "16px" :borderTop "1px solid #eee" :paddingTop "10px"}} "详细评估项"]]]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-airway-assessment-spec")
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        :children
        [:<>
         static-content
         dynamically-generated-form-items
         [:> Row {:justify "end" :style {:marginTop "20px"}}
          [:> Col
           [:> Form.Item
            [:button {:type "button"
                      :on-click on-show-summary
                      :style {:padding "5px 10px"
                              :background-color "#f0f0f0"
                              :border "1px solid #ccc"
                              :border-radius "4px"
                              :cursor "pointer"}}
             "返回总结"]]]]]}])))

(defn airway-assessment-card "气道评估" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        aa-data @(rf/subscribe [::subs/airway-assessment-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
       "气道评估"
       "#fff7e6"
       (if (= @view-state :summary)
         [airway-assessment-summary-view {:aa-data aa-data}]
         [:f> airway-assessment-detailed-view (merge props {:patient-id patient-id
                                                            :aa-data aa-data
                                                            :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))

;; Spinal Anesthesia Assessment Card
(defn- spinal-anesthesia-assessment-summary-view [props]
  (let [{:keys [saa-data]} props]
    [summary/assessment-summary {:data saa-data
                                 :schema assessment-specs/椎管内麻醉相关评估Spec
                                 :section-key :椎管内麻醉相关评估}]))

(defn spinal-anesthesia-assessment-detailed-view [props]
  (timbre/info "spinal-anesthesia-assessment-detailed-view: Props received:" (clj->js props))
  (let [{:keys [report-form-instance-fn patient-id saa-data on-show-summary]} props
        _ (timbre/info "spinal-anesthesia-assessment-detailed-view: saa-data from props:" (clj->js saa-data))
        [form] (Form.useForm)
        initial-form-values (let [data-from-db (or saa-data {})
                                  ;; No date preprocessing for this spec
                                  final-initial-values (form-utils/apply-enum-defaults-to-data
                                                         data-from-db
                                                         assessment-specs/椎管内麻醉相关评估Spec)]
                              final-initial-values)
        _ (timbre/info "spinal-anesthesia-assessment-detailed-view: Calculated initial-form-values:" (clj->js initial-form-values))
        on-finish-fn (fn [values]
                       (timbre/info "spinal-anesthesia-assessment-detailed-view: on-finish-fn - raw JS values from form:" values)
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (timbre/info "spinal-anesthesia-assessment-detailed-view: on-finish-fn - values-clj for dispatch:" (clj->js values-clj))
                         (rf/dispatch [::events/update-canonical-assessment-section :椎管内麻醉相关评估 values-clj])))]

    ;; Effect to update form when saa-data (and thus initial-form-values) changes
    (React/useEffect
     (fn []
       (timbre/info "spinal-anesthesia-assessment-detailed-view: useEffect [initial-form-values] triggered. current initial-form-values:" (clj->js initial-form-values))
       (.resetFields form)
       (.setFieldsValue form (clj->js initial-form-values))
       js/undefined) ; Return undefined for cleanup
     #js [initial-form-values]) ; Dependency: re-run if initial-form-values changes reference

    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :椎管内麻醉相关评估 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/椎管内麻醉相关评估Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-spinal-anesthesia-assessment-spec")
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        :children
        [:<>
         form-items
         [:> Row {:justify "end" :style {:marginTop "20px"}}
          [:> Col
           [:> Form.Item
            [:button {:type "button"
                      :on-click on-show-summary
                      :style {:padding "5px 10px"
                              :background-color "#f0f0f0"
                              :border "1px solid #ccc"
                              :border-radius "4px"
                              :cursor "pointer"}}
             "返回总结"]]]]]}])))

(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" [props]
  (let [view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        saa-data @(rf/subscribe [::subs/spinal-anesthesia-assessment-data])]
    (fn []
      [ui-helpers/custom-styled-card
       [:> GatewayOutlined {:style {:marginRight "8px"}}]
       "椎管内麻醉相关评估"
       "#f0f5ff"
       (if (= @view-state :summary)
         [spinal-anesthesia-assessment-summary-view {:saa-data saa-data}]
         [:f> spinal-anesthesia-assessment-detailed-view (merge props {:patient-id patient-id
                                                                       :saa-data saa-data
                                                                       :on-show-summary show-summary-fn})])
       :on-click toggle-view-fn
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))
