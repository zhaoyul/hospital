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

;; ---------------------------------------------------------------------------
;; 通用评估卡片生成器
;; ---------------------------------------------------------------------------
(defn create-assessment-card
  "根据给定配置生成可在总结和详细视图间切换的评估卡片组件。"
  [{:keys [icon title header-color data-sub data-key summary-view detailed-view]}]
  (fn [props]
    (let [view-state (r/atom :summary)
          show-summary #(reset! view-state :summary)
          toggle-view #(reset! view-state (if (= @view-state :summary) :detailed :summary))
          patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
          card-data @(rf/subscribe [data-sub])]
      (fn []
        [ui-helpers/custom-styled-card
         [:> icon {:style {:marginRight "8px"}}]
         title
         header-color
         (if (= @view-state :summary)
           [summary-view {data-key card-data}]
           [:f> detailed-view (merge props {:patient-id patient-id
                                           data-key card-data
                                           :on-show-summary show-summary})])
         :on-click toggle-view
         :view-state @view-state
         :card-style {:cursor "pointer"}
         :card-body-style {:padding "0px"}]))))


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

(def circulatory-system-card
  (create-assessment-card {:icon HeartOutlined
                           :title "循环系统"
                           :header-color "#e6f7ff"
                           :data-sub ::subs/circulatory-system-data
                           :data-key :circulatory-data
                           :summary-view circulatory-system-summary-view
                           :detailed-view circulatory-system-detailed-view}))

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

(def respiratory-system-card
  (create-assessment-card {:icon CloudOutlined
                           :title "呼吸系统"
                           :header-color "#e6fffb"
                           :data-sub ::subs/respiratory-system-data
                           :data-key :respiratory-data
                           :summary-view respiratory-system-summary-view
                           :detailed-view respiratory-system-detailed-view}))

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

(def mental-neuromuscular-system-card
  (create-assessment-card {:icon NodeIndexOutlined
                           :title "精神及神经肌肉系统"
                           :header-color "#f6ffed"
                           :data-sub ::subs/mental-neuromuscular-system-data
                           :data-key :mn-data
                           :summary-view mental-neuromuscular-system-summary-view
                           :detailed-view mental-neuromuscular-system-detailed-view}))

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

(def endocrine-system-card
  (create-assessment-card {:icon ExperimentOutlined
                           :title "内分泌系统"
                           :header-color "#f9f0ff"
                           :data-sub ::subs/endocrine-system-data
                           :data-key :endo-data
                           :summary-view endocrine-system-summary-view
                           :detailed-view endocrine-system-detailed-view}))

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

(def liver-kidney-system-card
  (create-assessment-card {:icon ProjectOutlined
                           :title "肝肾病史"
                           :header-color "#fff7e6"
                           :data-sub ::subs/liver-kidney-system-data
                           :data-key :lk-data
                           :summary-view liver-kidney-system-summary-view
                           :detailed-view liver-kidney-system-detailed-view}))

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

(def digestive-system-card
  (create-assessment-card {:icon CoffeeOutlined
                           :title "消化系统"
                           :header-color "#eff8ff"
                           :data-sub ::subs/digestive-system-data
                           :data-key :ds-data
                           :summary-view digestive-system-summary-view
                           :detailed-view digestive-system-detailed-view}))

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

(def hematologic-system-card
  (create-assessment-card {:icon ExperimentOutlined
                           :title "血液系统"
                           :header-color "#fff0f6"
                           :data-sub ::subs/hematologic-system-data
                           :data-key :hs-data
                           :summary-view hematologic-system-summary-view
                           :detailed-view hematologic-system-detailed-view}))

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

(def immune-system-card
  (create-assessment-card {:icon SecurityScanOutlined
                           :title "免疫系统"
                           :header-color "#f6ffed"
                           :data-sub ::subs/immune-system-data
                           :data-key :is-data
                           :summary-view immune-system-summary-view
                           :detailed-view immune-system-detailed-view}))

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

(def special-medication-history-card
  (create-assessment-card {:icon MedicineBoxOutlined
                           :title "特殊用药史"
                           :header-color "#fffbe6"
                           :data-sub ::subs/special-medication-history-data
                           :data-key :smh-data
                           :summary-view special-medication-history-summary-view
                           :detailed-view special-medication-history-detailed-view}))

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

(def special-disease-history-card
  (create-assessment-card {:icon WarningOutlined
                           :title "特殊疾病病史"
                           :header-color "#fff1f0"
                           :data-sub ::subs/special-disease-history-data
                           :data-key :sdh-data
                           :summary-view special-disease-history-summary-view
                           :detailed-view special-disease-history-detailed-view}))

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

(def nutritional-assessment-card
  (create-assessment-card {:icon AppleOutlined
                           :title "营养评估"
                           :header-color "#f0fff0"
                           :data-sub ::subs/nutritional-assessment-data
                           :data-key :na-data
                           :summary-view nutritional-assessment-summary-view
                           :detailed-view nutritional-assessment-detailed-view}))

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

(def pregnancy-assessment-card
  (create-assessment-card {:icon WomanOutlined
                           :title "妊娠"
                           :header-color "#fff0f6"
                           :data-sub ::subs/pregnancy-assessment-data
                           :data-key :pa-data
                           :summary-view pregnancy-assessment-summary-view
                           :detailed-view pregnancy-assessment-detailed-view}))

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

(def surgical-anesthesia-history-card
  (create-assessment-card {:icon HistoryOutlined
                           :title "手术麻醉史"
                           :header-color "#e6f7ff"
                           :data-sub ::subs/surgical-anesthesia-history-data
                           :data-key :sah-data
                           :summary-view surgical-anesthesia-history-summary-view
                           :detailed-view surgical-anesthesia-history-detailed-view}))

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

(def airway-assessment-card
  (create-assessment-card {:icon NodeIndexOutlined
                           :title "气道评估"
                           :header-color "#fff7e6"
                           :data-sub ::subs/airway-assessment-data
                           :data-key :aa-data
                           :summary-view airway-assessment-summary-view
                           :detailed-view airway-assessment-detailed-view}))

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

(def spinal-anesthesia-assessment-card
  (create-assessment-card {:icon GatewayOutlined
                           :title "椎管内麻醉相关评估"
                           :header-color "#f0f5ff"
                           :data-sub ::subs/spinal-anesthesia-assessment-data
                           :data-key :saa-data
                           :summary-view spinal-anesthesia-assessment-summary-view
                           :detailed-view spinal-anesthesia-assessment-detailed-view}))
