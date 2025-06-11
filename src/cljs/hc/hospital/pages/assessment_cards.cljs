(ns hc.hospital.pages.assessment-cards
  (:require
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined CoffeeOutlined
                                 ExperimentOutlined GatewayOutlined
                                 HeartOutlined HistoryOutlined
                                 MedicineBoxOutlined NodeIndexOutlined
                                 ProjectOutlined SecurityScanOutlined
                                 UserOutlined WarningOutlined WomanOutlined]]
   ;; Added Row and Col back as they are used for the summary button layout.
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
   [hc.hospital.natural-language-generators :as nlg] ; Added nlg
   [hc.hospital.form-utils :as form-utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

(defn- circulatory-system-summary-view [props]
  (let [{:keys [circulatory-data]} props]
    (if (seq circulatory-data)
      (nlg/generate-summary-component circulatory-data
                                      assessment-specs/循环系统Spec
                                      :循环系统)
      ;; When circulatory-data is nil or empty, provide a consistent empty state structure
      [:div.summary-section {:key :circulatory-system-empty
                             :style {:padding "10px" :border "1px solid #ddd" :margin-bottom "10px" :border-radius "4px"}}
       [:h3 {:style {:font-size "16px" :font-weight "bold" :margin-top "0" :margin-bottom "8px"}}
        (nlg/schema-key->display-label :循环系统) "："]
       [:p {:style {:margin "0"}} "暂无数据可供总结。"]])))

(defn- circulatory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id circulatory-data on-show-summary]} props
        [form] (Form.useForm)
        ;; 初始化表单值的数据处理流程
        initial-form-values (let [data-from-db (or circulatory-data {}) ; 1. 从数据库获取原始数据，如果为空则使用空 map
                                  ;; 2. 应用枚举字段的默认值
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            assessment-specs/循环系统Spec)
                                  ;; 3. 自动将所有在 Spec 中标记为 :is-date? true 的日期字符串转换为 dayjs 对象
                                  data-with-parsed-dates (form-utils/preprocess-date-fields
                                                           data-with-enum-defaults
                                                           assessment-specs/循环系统Spec)
                                  ;; 最终的初始值，移除了手动的 default-values-override 和 merge
                                  final-initial-values data-with-parsed-dates]
                              ;; 此前的 cond-> 逻辑块已根据用户反馈移除 (用于处理特定字段如 :有无 的显式设置)。
                              ;; 现在直接返回经过枚举默认化和日期自动预处理后的数据。
                              final-initial-values)
        _ (timbre/info "circulatory-system-detailed-view: final initial-form-values after all processing (cond-> logic removed):" (clj->js initial-form-values))
        ;; 表单提交时的处理函数
        on-finish-fn (fn [values]
                       (timbre/info "circulatory-system-detailed-view: on-finish-fn raw JS values:" values)
                       (let [values-clj (js->clj values :keywordize-keys true) ; 1. 将 JS 表单值转换为 ClojureScript map
                             _ (timbre/info "circulatory-system-detailed-view: on-finish-fn cljs values-clj before transformation:" (clj->js values-clj))
                             ;; 2. 自动将所有 dayjs 对象转换回 ISO 日期字符串，以便存储或传输
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  assessment-specs/循环系统Spec)]
                         (timbre/info "circulatory-system-detailed-view: on-finish-fn transformed-values for dispatch:" (clj->js transformed-values))
                         (rf/dispatch [::events/update-canonical-assessment-section :循环系统 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :循环系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/循环系统Spec
          form-items (into [:<>]
                           (mapv (fn [[field-key field-schema optional? _]]
                                   (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                                 (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-circulatory-system-spec")
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
    (if (seq respiratory-data)
      (let [summary-hiccup (nlg/generate-summary-component respiratory-data assessment-specs/呼吸系统Spec :呼吸系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无呼吸系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无呼吸系统评估数据可供总结。"])))

(defn respiratory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id respiratory-data on-show-summary]} props
        [form] (Form.useForm)
        ;; 初始化表单值的数据处理流程
        initial-form-values (let [data-from-db (or respiratory-data {}) ; 1. 从数据库获取原始数据，如果为空则使用空 map
                                  ;; 2. 应用枚举字段的默认值
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            assessment-specs/呼吸系统Spec)
                                  ;; 3. 自动将所有在 Spec 中标记为 :is-date? true 的日期字符串转换为 dayjs 对象
                                  final-initial-values (form-utils/preprocess-date-fields
                                                         data-with-enum-defaults
                                                         assessment-specs/呼吸系统Spec)]
                              final-initial-values)
        ;; 表单提交时的处理函数
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true) ; 1. 将 JS 表单值转换为 ClojureScript map
                             ;; 2. 自动将所有 dayjs 对象转换回 ISO 日期字符串，以便存储或传输
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  assessment-specs/呼吸系统Spec)]
                         (rf/dispatch [::events/update-canonical-assessment-section :呼吸系统 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :呼吸系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/呼吸系统Spec
          form-items (into [:<>]
                           (mapv (fn [[field-key field-schema optional? _]]
                                   (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                                 (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-respiratory-system-spec")
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
    (if (seq mn-data)
      (let [summary-hiccup (nlg/generate-summary-component mn-data assessment-specs/精神及神经肌肉系统Spec :精神及神经肌肉系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无精神及神经肌肉系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无精神及神经肌肉系统评估数据可供总结。"])))

(defn mental-neuromuscular-system-detailed-view [props]
  (timbre/info "mental-neuromuscular-system-detailed-view: Props received:" (clj->js props))
  (let [{:keys [report-form-instance-fn patient-id mn-data on-show-summary]} props
        _ (timbre/info "mental-neuromuscular-system-detailed-view: mn-data from props:" (clj->js mn-data))
        [form] (Form.useForm)
        ;; 初始化表单值的数据处理流程
        initial-form-values (let [data-from-db (or mn-data {}) ; 1. 从数据库获取原始数据，如果为空则使用空 map
                                  ;; 2. 应用枚举字段的默认值
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            assessment-specs/精神及神经肌肉系统Spec)
                                  ;; 3. 自动将所有在 Spec 中标记为 :is-date? true 的日期字符串转换为 dayjs 对象
                                  final-initial-values (form-utils/preprocess-date-fields
                                                         data-with-enum-defaults
                                                         assessment-specs/精神及神经肌肉系统Spec)]
                              final-initial-values)
        _ (timbre/info "mental-neuromuscular-system-detailed-view: Calculated initial-form-values:" (clj->js initial-form-values))
        ;; 表单提交时的处理函数
        on-finish-fn (fn [values]
                       (timbre/info "mental-neuromuscular-system-detailed-view: on-finish-fn - raw JS values from form:" values)
                       (let [values-clj (js->clj values :keywordize-keys true)
                             _ (timbre/info "mental-neuromuscular-system-detailed-view: on-finish-fn - values-clj after js->clj:" (clj->js values-clj))
                             ;; 2. 自动将所有 dayjs 对象转换回 ISO 日期字符串，以便存储或传输
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  assessment-specs/精神及神经肌肉系统Spec)]
                         (timbre/info "mental-neuromuscular-system-detailed-view: on-finish-fn - transformed-values for dispatch:" (clj->js transformed-values))
                         (rf/dispatch [::events/update-canonical-assessment-section :精神及神经肌肉系统 transformed-values])))]

    ;; Effect to update form when mn-data (and thus initial-form-values) changes
    (React/useEffect
     (fn []
       (timbre/info "mental-neuromuscular-system-detailed-view: useEffect [initial-form-values] triggered. current initial-form-values:" (clj->js initial-form-values))
       (.resetFields form)
       (.setFieldsValue form (clj->js initial-form-values))
       js/undefined) ; Return undefined for cleanup
     #js [initial-form-values]) ; Dependency: re-run if initial-form-values changes reference

    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :精神及神经肌肉系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/精神及神经肌肉系统Spec
          form-items (into [:<>]
                           (mapv (fn [[field-key field-schema optional? _]]
                                   (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                                 (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-mental-neuromuscular-system-spec")
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
    (if (seq endo-data)
      (let [summary-hiccup (nlg/generate-summary-component endo-data assessment-specs/内分泌系统Spec :内分泌系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无内分泌系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无内分泌系统评估数据可供总结。"])))

(defn endocrine-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id endo-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or endo-data {})
                              assessment-specs/内分泌系统Spec)
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :内分泌系统 (js->clj values :keywordize-keys true)]))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :内分泌系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/内分泌系统Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-endocrine-system-spec")
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
    (if (seq lk-data)
      (let [summary-hiccup (nlg/generate-summary-component lk-data assessment-specs/肝肾病史Spec :肝肾病史)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无肝肾病史评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无肝肾病史评估数据可供总结。"])))

(defn liver-kidney-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id lk-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [data-from-db (or lk-data {})
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            assessment-specs/肝肾病史Spec)]
                              (form-utils/preprocess-date-fields
                                data-with-enum-defaults
                                assessment-specs/肝肾病史Spec))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  assessment-specs/肝肾病史Spec)]
                         (rf/dispatch [::events/update-canonical-assessment-section :肝肾病史 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :肝肾病史 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/肝肾病史Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-liver-kidney-system-spec")
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
    (if (seq ds-data)
      (let [summary-hiccup (nlg/generate-summary-component ds-data assessment-specs/消化系统Spec :消化系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无消化系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无消化系统评估数据可供总结。"])))

(defn digestive-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id ds-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or ds-data {})
                              assessment-specs/消化系统Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :消化系统 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :消化系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/消化系统Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-digestive-system-spec")
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
    (if (seq hs-data)
      (let [summary-hiccup (nlg/generate-summary-component hs-data assessment-specs/血液系统Spec :血液系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无血液系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无血液系统评估数据可供总结。"])))

(defn hematologic-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id hs-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or hs-data {})
                              assessment-specs/血液系统Spec)
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :血液系统 (js->clj values :keywordize-keys true)]))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :血液系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/血液系统Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-hematologic-system-spec")
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
    (if (seq is-data)
      (let [summary-hiccup (nlg/generate-summary-component is-data assessment-specs/免疫系统Spec :免疫系统)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无免疫系统评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无免疫系统评估数据可供总结。"])))

(defn immune-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id is-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or is-data {})
                              assessment-specs/免疫系统Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :免疫系统 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :免疫系统 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/免疫系统Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-immune-system-spec")
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
    (if (seq smh-data)
      (let [summary-hiccup (nlg/generate-summary-component smh-data assessment-specs/特殊用药史Spec :特殊用药史)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无特殊用药史评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无特殊用药史评估数据可供总结。"])))

(defn special-medication-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id smh-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or smh-data {})
                              assessment-specs/特殊用药史Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :特殊用药史 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :特殊用药史 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/特殊用药史Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-special-medication-history-spec")
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
    (if (seq sdh-data)
      (let [summary-hiccup (nlg/generate-summary-component sdh-data assessment-specs/特殊疾病病史Spec :特殊疾病病史)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无特殊疾病病史评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无特殊疾病病史评估数据可供总结。"])))

(defn special-disease-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sdh-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or sdh-data {})
                              assessment-specs/特殊疾病病史Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :特殊疾病病史 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :特殊疾病病史 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/特殊疾病病史Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-special-disease-history-spec")
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
    (if (seq na-data)
      (let [summary-hiccup (nlg/generate-summary-component na-data assessment-specs/营养评估Spec :营养评估)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无营养评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无营养评估数据可供总结。"])))

(defn nutritional-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id na-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or na-data {})
                              assessment-specs/营养评估Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :营养评估 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :营养评估 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/营养评估Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-nutritional-assessment-spec")
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        :children
        [:<>
         form-items
         [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px" :marginTop "10px"}}
          [:h5 {:style {:marginBottom "4px"}} "营养评分说明:"]
          [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
         [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginTop "10px"}}
          [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
          [:p {:style {:fontSize "12px" :color "gray"}}
           "0 分：健康；" [:br]
           "1-2 分：衰弱前期；" [:br]
           "≥3 分：衰弱。"]]
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
    (if (seq pa-data)
      (let [summary-hiccup (nlg/generate-summary-component pa-data assessment-specs/妊娠Spec :妊娠)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无妊娠评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无妊娠评估数据可供总结。"])))

(defn pregnancy-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id pa-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (form-utils/apply-enum-defaults-to-data
                              (or pa-data {})
                              assessment-specs/妊娠Spec)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :妊娠 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :妊娠 form)) ; Use spec keyword
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/妊娠Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-pregnancy-assessment-spec")
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
    (if (seq sah-data)
      (let [summary-hiccup (nlg/generate-summary-component sah-data assessment-specs/手术麻醉史Spec :手术麻醉史)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无手术麻醉史评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无手术麻醉史评估数据可供总结。"])))

(defn surgical-anesthesia-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sah-data on-show-summary]} props
        [form] (Form.useForm)
        ;; 初始化表单值的数据处理流程
        initial-form-values (let [data-from-db (or sah-data {}) ; 1. 从数据库获取原始数据，如果为空则使用空 map
                                  ;; 2. 应用枚举字段的默认值
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            assessment-specs/手术麻醉史Spec)
                                  ;; 3. 自动将所有在 Spec 中标记为 :is-date? true 的日期字符串转换为 dayjs 对象
                                  final-initial-values (form-utils/preprocess-date-fields
                                                         data-with-enum-defaults
                                                         assessment-specs/手术麻醉史Spec)]
                              (timbre/info "surgical-anesthesia-history-detailed-view: final initial-form-values after processing:" (clj->js final-initial-values))
                              final-initial-values)
        ;; 表单提交时的处理函数
        on-finish-fn (fn [values]
                       (timbre/info "surgical-anesthesia-history-detailed-view: on-finish-fn raw JS values:" values)
                       (let [values-clj (js->clj values :keywordize-keys true) ; 1. 将 JS 表单值转换为 ClojureScript map
                             _ (timbre/info "surgical-anesthesia-history-detailed-view: on-finish-fn cljs values-clj before transformation:" (clj->js values-clj))
                             ;; 2. 自动将所有 dayjs 对象转换回 ISO 日期字符串，以便存储或传输
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  assessment-specs/手术麻醉史Spec)]
                         (timbre/info "surgical-anesthesia-history-detailed-view: on-finish-fn transformed-values for dispatch:" (clj->js transformed-values))
                         (rf/dispatch [::events/update-canonical-assessment-section :手术麻醉史 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :手术麻醉史 form))
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/手术麻醉史Spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-surgical-anesthesia-history-spec")
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
    (if (seq aa-data)
      (let [summary-hiccup (nlg/generate-summary-component aa-data assessment-specs/气道评估Spec :气道评估)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无气道评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无气道评估数据可供总结。"])))

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
    (if (seq saa-data)
      (let [summary-hiccup (nlg/generate-summary-component saa-data assessment-specs/椎管内麻醉相关评估Spec :椎管内麻醉相关评估)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}} "暂无椎管内麻醉相关评估数据可供总结 (内容为空)。"]))
      [:div {:style {:padding "10px"}} "暂无椎管内麻醉相关评估数据可供总结。"])))

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
