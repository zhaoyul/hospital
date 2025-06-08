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
   [hc.hospital.summary-generators :as sg] ; Added new namespace
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

(defn- circulatory-system-summary-view [props]
  (let [{:keys [circulatory-data]} props
        content (sg/generate-summary-from-spec circulatory-data assessment-specs/循环系统Spec)]
    [:div {:style {:padding "10px"}}
     content]))

(defn- circulatory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id circulatory-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [base-data (when circulatory-data
                                              (-> circulatory-data
                                                  (update-in [:心脏疾病史 :详情 :充血性心力衰竭史 :上次发作日期] #(when % (utils/parse-date %)))))
                                  default-values {:心脏疾病史 {:有无 :无 ; Using keyword for enum
                                                               :详情 {:冠心病 {:有无 :无}
                                                                      :心律失常 {:有无 :无}
                                                                      :心肌病 {:有无 :无}
                                                                      :心脏瓣膜病变 {:有无 :无} ; Updated key
                                                                      :先天性心脏病 {:有无 :无}
                                                                      :充血性心力衰竭史 {:有无 :无}
                                                                      :肺动脉高压 {:有无 :无}}}
                                                  :心脏起搏器植入史 {:有无 :无} ; Using keyword for enum
                                                  :心脏功能评估 {:NYHA分级 :Ⅰ级} ; Using keyword for enum
                                                  :运动能力评估 {:METs水平 :大于6MET}} ; Using keyword for enum, e.g. :大于6MET (check spec for exact enum)
                                  merged-data (merge default-values (or base-data {}))]
                              ;; Apply defaults specifically for :有无 fields if details exist but :有无 is nil
                              (cond-> merged-data
                                (and (get-in merged-data [:心脏疾病史 :详情]) (nil? (get-in merged-data [:心脏疾病史 :有无])))
                                (assoc-in [:心脏疾病史 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :冠心病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :冠心病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :冠心病 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :心律失常]) (nil? (get-in merged-data [:心脏疾病史 :详情 :心律失常 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :心律失常 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :心肌病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :心肌病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :心肌病 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :心脏瓣膜病变]) (nil? (get-in merged-data [:心脏疾病史 :详情 :心脏瓣膜病变 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :心脏瓣膜病变 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :先天性心脏病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :先天性心脏病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :先天性心脏病 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :充血性心力衰竭史]) (nil? (get-in merged-data [:心脏疾病史 :详情 :充血性心力衰竭史 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :充血性心力衰竭史 :有无] :有)
                                (and (get-in merged-data [:心脏疾病史 :详情 :肺动脉高压]) (nil? (get-in merged-data [:心脏疾病史 :详情 :肺动脉高压 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :肺动脉高压 :有无] :有)
                                (and (get-in merged-data [:心脏起搏器植入史 :详情]) (nil? (get-in merged-data [:心脏起搏器植入史 :有无])))
                                (assoc-in [:心脏起搏器植入史 :有无] :有)))
        _ (timbre/info "circulatory-system-detailed-view: initial-form-values:" (clj->js initial-form-values))
        on-finish-fn (fn [values]
                       (timbre/info "circulatory-system-detailed-view: on-finish-fn raw JS values:" values)
                       (let [values-clj (js->clj values :keywordize-keys true)
                             _ (timbre/info "circulatory-system-detailed-view: on-finish-fn cljs values-clj:" (clj->js values-clj))
                             transformed-values (-> values-clj
                                                    (update-in [:心脏疾病史 :详情 :充血性心力衰竭史 :上次发作日期] #(when % (utils/date->iso-string %))))] ; Path already Chinese
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
  (let [{:keys [respiratory-data]} props
        content (sg/generate-summary-from-spec respiratory-data assessment-specs/呼吸系统Spec)]
    [:div {:style {:padding "10px"}}
     content]))

(defn respiratory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id respiratory-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed local cold-symptom-options, treatment-options
        initial-form-values (let [base-data (when respiratory-data
                                              (-> respiratory-data
                                                  (update-in [:近两周内感冒病史 :详情 :发病日期] #(when % (utils/parse-date %)))
                                                  (update-in [:近一个月内支气管炎或肺炎病史 :详情 :发病日期] #(when % (utils/parse-date %)))
                                                  (update-in [:哮喘病史 :详情 :上次发作日期] #(when % (utils/parse-date %)))))]
                              (-> (or base-data {}) ; Base data should now have Chinese keys
                                  (update-in [:近两周内感冒病史 :有无] #(or % :无)) ; Use keyword for enum
                                  (update-in [:近一个月内支气管炎或肺炎病史 :有无] #(or % :无))
                                  (update-in [:哮喘病史 :有无] #(or % :无))
                                  (update-in [:慢性阻塞性肺疾病 :有无] #(or % :无))
                                  (update-in [:支气管扩张症 :有无] #(or % :无))
                                  (update-in [:肺部结节 :有无] #(or % :无))
                                  (update-in [:肺部肿瘤 :有无] #(or % :无))
                                  (update-in [:是否有肺结核] #(or % :无)))) ; Mapped from [:tuberculosis_history :present]
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj
                                                    (update-in [:近两周内感冒病史 :详情 :发病日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:近一个月内支气管炎或肺炎病史 :详情 :发病日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:哮喘病史 :详情 :上次发作日期] #(when % (utils/date->iso-string %))))] ; Paths already Chinese
                         (rf/dispatch [::events/update-canonical-assessment-section :呼吸系统 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :呼吸系统 form)) ; Use new keyword
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
        :form-key (str patient-id "-respiratory-system-spec") ; Updated form key
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
(defn generate-mental-neuromuscular-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Psycho-cognitive History - Mapped from :psycho_cognitive_history to :精神认知相关疾病史
      (let [pc_data (get data :精神认知相关疾病史)
            pc_present (get pc_data :有无)] ; Mapped :present to :有无
        (cond
          (= pc_present :有) (conj! findings (str "精神认知史:有" (let [symptoms (get-in pc_data [:详情 :症状])] (if (seq symptoms) (str "(" (str/join "/" (map name symptoms)) ")") "")))) ; symptoms under :详情
          (= pc_present :无) (conj! findings "精神认知史:无")
          :else (conj! findings "精神认知史:未评估/不祥")))

      ;; Epilepsy History - Mapped from :epilepsy_history to :癫痫病史
      (let [ep_data (get data :癫痫病史)
            ep_present (get ep_data :有无)] ; Mapped :present to :有无
        (cond
          (= ep_present :有) (conj! findings (str "癫痫史:有" (when-let [status (get-in ep_data [:详情 :治疗情况])] (str "(" (name status) ")")))) ; treatment_status under :详情
          (= ep_present :无) (conj! findings "癫痫史:无")
          :else (conj! findings "癫痫史:未评估/不祥")))

      ;; Vertigo History - Mapped from :vertigo_history to :眩晕病史
      (let [vt_data (get data :眩晕病史)
            vt_present (get vt_data :有无)] ; Mapped :present to :有无
        (cond
          (= vt_present :有) (conj! findings (str "眩晕史:有" (when-let [status (get-in vt_data [:详情 :治疗情况])] (str "(" (name status) ")")))) ; treatment_status under :详情
          (= vt_present :无) (conj! findings "眩晕史:无")
          :else (conj! findings "眩晕史:未评估/不祥")))

      ;; TIA History - Mapped from :tia_history to :短暂性脑缺血发作病史
      (let [tia_data (get data :短暂性脑缺血发作病史)
            tia_present (get tia_data :有无)] ; Mapped :present to :有无
        (cond
          (= tia_present :有) (conj! findings (str "TIA史:有" (when-let [status (get-in tia_data [:详情 :近期发作情况])] (str "(" (name status) ")")))) ; recent_onset_status under :详情
          (= tia_present :无) (conj! findings "TIA史:无")
          :else (conj! findings "TIA史:未评估/不祥")))

      ;; Cerebral Infarction History - Mapped from :cerebral_infarction_history to :脑梗病史
      (let [ci_data (get data :脑梗病史)
            ci_present (get ci_data :有无)] ; Mapped :present to :有无
        (cond
          (= ci_present :有) (conj! findings (str "脑梗史:有" (when-let [status (get-in ci_data [:详情 :治疗情况])] (str "(" (name status) ")")))) ; treatment_status under :详情
          (= ci_present :无) (conj! findings "脑梗史:无")
          :else (conj! findings "脑梗史:未评估")))

      ;; Cerebral Hemorrhage History - Mapped from :cerebral_hemorrhage_history to :脑出血病史
      (let [ch_data (get data :脑出血病史)
            ch_present (get ch_data :有无)] ; Mapped :present to :有无
        (cond
          (= ch_present :有) (conj! findings (str "脑出血史:有" (when-let [status (get-in ch_data [:详情 :治疗情况])] (str "(" (name status) ")")))) ; treatment_status under :详情
          (= ch_present :无) (conj! findings "脑出血史:无")
          :else (conj! findings "脑出血史:未评估")))

      ;; Parkinson's Syndrome - Mapped from :parkinsons_syndrome to :帕金森综合症
      (let [ps_data (get data :帕金森综合症)
            ps_present (get ps_data :有无)] ; Mapped :present to :有无
        (cond
          (= ps_present :有) (conj! findings (str "帕金森综合征:有" (when-let [status (get-in ps_data [:详情 :治疗情况])] (str "(" (name status) ")")))) ; treatment_status under :详情
          (= ps_present :无) (conj! findings "帕金森综合征:无")
          :else (conj! findings "帕金森综合征:未评估")))

      ;; Cranial/Carotid Stenosis - Mapped from :cranial_carotid_stenosis to :颅脑和颈动脉狭窄
      (let [ccs_present (get-in data [:颅脑和颈动脉狭窄 :有无])] ; Mapped :present to :有无
        (cond
          (= ccs_present :有) (conj! findings (str "颅脑/颈动脉狭窄:有" (if-not (str/blank? (get-in data [:颅脑和颈动脉狭窄 :描述])) "(详情)" ""))) ; :details to :描述
          (= ccs_present :无) (conj! findings "颅脑/颈动脉狭窄:无")
          (= ccs_present :不详) (conj! findings "颅脑/颈动脉狭窄:不详")))

      ;; Other Neuromuscular Conditions - Mapped from :other_neuromuscular_conditions to [:其他情况] (within this system)
      (let [onc_data (get data :其他情况)
            onc_present (get onc_data :有无)] ; Mapped :present to :有无
        (when (= onc_present :有)
          (conj! findings (str "其他神经肌肉系统情况:有" (let [symptoms (get-in onc_data [:详情 :症状])] (if (seq symptoms) (str "(" (str/join "/" (map name symptoms)) ")") "")))))) ; symptoms under :详情

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "精神及神经肌肉系统: 未见明显异常"
          (str "精神及神经肌肉系统: " (str/join ", " persistent_findings)))))))

(defn mental-neuromuscular-system-summary-view [props]
  (let [{:keys [mn-data]} props ; Corrected from endo-data to mn-data
        content (generate-mental-neuromuscular-summary mn-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn mental-neuromuscular-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id mn-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls and local option lists
        initial-form-values (let [base-data (when mn-data
                                              (-> mn-data ; Assuming mn-data is already in Chinese keys from subs
                                                  (update-in [:癫痫病史 :详情 :近期发作日期] #(when % (utils/parse-date %)))
                                                  (update-in [:眩晕病史 :详情 :近期发作日期] #(when % (utils/parse-date %)))
                                                  (update-in [:脑梗病史 :详情 :近期发作日期] #(when % (utils/parse-date %)))
                                                  (update-in [:脑出血病史 :详情 :近期发作日期] #(when % (utils/parse-date %)))))]
                              (-> (or base-data {}) ; Base data should now have Chinese keys
                                  (update-in [:精神认知相关疾病史 :有无] #(or % :无)) ; Use keyword for enum
                                  (update-in [:癫痫病史 :有无] #(or % :无))
                                  (update-in [:眩晕病史 :有无] #(or % :无))
                                  (update-in [:短暂性脑缺血发作病史 :有无] #(or % :无))
                                  (update-in [:脑梗病史 :有无] #(or % :无))
                                  (update-in [:脑出血病史 :有无] #(or % :无))
                                  (update-in [:帕金森综合症 :有无] #(or % :无))
                                  (update-in [:颅脑和颈动脉狭窄 :有无] #(or % :无))
                                  (update-in [:其他情况 :有无] #(or % :无))))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj ; Paths are already Chinese from form
                                                    (update-in [:癫痫病史 :详情 :近期发作日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:眩晕病史 :详情 :近期发作日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:脑梗病史 :详情 :近期发作日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:脑出血病史 :详情 :近期发作日期] #(when % (utils/date->iso-string %))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :精神及神经肌肉系统 transformed-values])))]
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
        :form-key (str patient-id "-mental-neuromuscular-system-spec") ; Updated form key
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
(defn generate-endocrine-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Thyroid Disease History - Mapped from :thyroid_disease_history to :甲状腺疾病病史
      (let [td_data (get data :甲状腺疾病病史)
            td_present (get td_data :有无)] ; Mapped :present to :有无
        (cond
          (= td_present :有) (conj! findings (str "甲状腺疾病:有" (let [types (get-in td_data [:详情 :类型])] (if types (str "(" (name types) ")") "")))) ; :types to :类型 under :详情
          (= td_present :无) (conj! findings "甲状腺疾病:无")
          :else (conj! findings "甲状腺疾病:未评估/不祥")))

      ;; Diabetes History - Mapped from :diabetes_history to :糖尿病病史
      (let [db_data (get data :糖尿病病史)
            db_present (get db_data :有无)] ; Mapped :present to :有无
        (cond
          (= db_present :有)
          (let [type (get-in db_data [:详情 :类型]) ; :type to :类型 under :详情
                control (get-in db_data [:详情 :控制方式]) ; :control_method to :控制方式 under :详情
                details_parts (cond-> [] type (conj (name type)) control (conj (name control)))]
            (conj! findings (str "糖尿病:有" (if (seq details_parts) (str "(" (str/join "/" details_parts) ")") ""))))
          (= db_present :无) (conj! findings "糖尿病:无")
          :else (conj! findings "糖尿病:未评估/不祥")))

      ;; Pheochromocytoma - Mapped from :pheochromocytoma to :嗜铬细胞瘤
      (let [ph_data (get data :嗜铬细胞瘤)
            ph_present (get ph_data :有无)] ; Mapped :present to :有无
        (cond
          (= ph_present :有) (conj! findings (str "嗜铬细胞瘤:有" (when-let [status (get-in ph_data [:详情 :控制情况])] (str "(" (name status) ")")))) ; :control_status to :控制情况 under :详情
          (= ph_present :无) (conj! findings "嗜铬细胞瘤:无")
          :else (conj! findings "嗜铬细胞瘤:未评估")))

      ;; Hypercortisolism - Mapped from :hypercortisolism to :皮质醇增多症
      (let [hc_data (get data :皮质醇增多症) ; Note: spec uses 皮质醇增多症, but also has肾上腺皮质功能不全. Assuming this maps to the broader category for summary.
            hc_present (get hc_data :有无)] ; Mapped :present to :有无
        (cond
          (= hc_present :有) (conj! findings (str "皮质醇增多症:有" (when-let [details (get-in hc_data [:类型])] (str "(" (name details) ")")))) ; :details to :类型
          (= hc_present :无) (conj! findings "皮质醇增多症:无")
          :else (conj! findings "皮质醇增多症:未评估/不祥")))

      ;; Gout - Mapped from :gout to :痛风
      (let [gout_present (get-in data [:痛风 :有无])] ; Mapped :present to :有无
        (cond
          (= gout_present :有) (conj! findings (str "痛风:有" (if-not (str/blank? (get-in data [:痛风 :描述])) "(详情)" ""))) ; :details to :描述
          (= gout_present :无) (conj! findings "痛风:无")
          (= gout_present :不详) (conj! findings "痛风:不详")))

      ;; Hypopituitarism - Mapped from :hypopituitarism to :垂体功能减退症
      (let [hypo_present (get-in data [:垂体功能减退症 :有无])] ; Mapped :present to :有无
        (cond
          (= hypo_present :有) (conj! findings (str "垂体功能减退:有" (if-not (str/blank? (get-in data [:垂体功能减退症 :描述])) "(详情)" ""))) ; :details to :描述
          (= hypo_present :无) (conj! findings "垂体功能减退:无")
          (= hypo_present :不详) (conj! findings "垂体功能减退:不详")))

      ;; Other Endocrine Conditions - Mapped from :other_endocrine_conditions to [:其他情况 :内容]
      (when (not (str/blank? (get-in data [:其他情况 :内容])))
        (conj! findings "其他内分泌情况:有记录"))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "内分泌系统: 未见明显异常"
          (str "内分泌系统: " (str/join ", " persistent_findings)))))))

(defn endocrine-system-summary-view [props]
  (let [{:keys [endo-data]} props
        content (generate-endocrine-summary endo-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn endocrine-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id endo-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls and local option lists
        initial-form-values (-> (or endo-data {}) ; Base data should now have Chinese keys
                                (update-in [:甲状腺疾病病史 :有无] #(or % :无)) ; Use keyword for enum
                                ;; The following two were English before, not present in Chinese spec directly under 甲状腺疾病病史 for :有无
                                ;; (update-in [:甲状腺疾病病史 :详情 :甲状腺是否肿大压迫气管] #(or % false)) ; Assuming boolean from spec
                                ;; (update-in [:甲状腺疾病病史 :详情 :是否合并甲状腺心脏病] #(or % false)) ; Assuming boolean from spec
                                (update-in [:糖尿病病史 :有无] #(or % :无))
                                (update-in [:嗜铬细胞瘤 :有无] #(or % :无))
                                (update-in [:皮质醇增多症 :有无] #(or % :无))
                                (update-in [:痛风 :有无] #(or % :无)) ; Corrected key from 痛风病史 to 痛风
                                (update-in [:垂体功能减退症 :有无] #(or % :无))) ; Corrected key
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
        :form-key (str patient-id "-endocrine-system-spec") ; Updated form key
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
(defn generate-liver-kidney-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Liver Function - Mapped from [:liver_function :status] to [:肝功能 :状态]
      (let [lf_status (get-in data [:肝功能 :状态])]
        (cond
          (= lf_status :异常) ; Enum
          (let [alt (get-in data [:肝功能 :详情 :谷丙转氨酶ALT]) ; Path under :详情
                albumin (get-in data [:肝功能 :详情 :血清白蛋白]) ; Path under :详情
                details (transient [])]
            (when alt (conj! details (str "ALT:" alt)))
            (when albumin (conj! details (str "Alb:" albumin)))
            (conj! findings (str "肝功能:异常" (let [pd (persistent! details)] (if (seq pd) (str "(" (str/join ", " pd) ")") "")))))
          (= lf_status :正常) (conj! findings "肝功能:正常") ; Enum
          :else (conj! findings "肝功能:未评估")))

      ;; Liver Disease History - Mapped from [:liver_disease_history :types] to [:肝脏疾病病史 :类型]
      (let [ldh_type (get-in data [:肝脏疾病病史 :类型])] ; Spec uses a single type, not a list of types
        (if (and ldh_type (not= ldh_type :无)) ; Check if type is not :无 (enum for none)
          (conj! findings (str "肝脏疾病:有(" (name ldh_type) ")"))
          (conj! findings "肝脏疾病:无")))

      ;; Kidney Function & Disease (Simplified for summary - check spec for more details)
      (let [kf_status (get-in data [:肾功能 :状态])
            kdh_type (get-in data [:肾脏疾病病史 :类型])]
        (cond
          (= kf_status :异常) (conj! findings "肾功能:异常")
          (= kf_status :正常) (conj! findings "肾功能:正常")
          :else (conj! findings "肾功能:未评估"))
        (if (and kdh_type (not= kdh_type :无))
          (conj! findings (str "肾脏疾病:有(" (name kdh_type) ")"))
          (conj! findings "肾脏疾病:无")))


      ;; Other Liver/Kidney Conditions - Mapped from :other_liver_kidney_conditions to [:其他情况 :内容]
      (when (not (str/blank? (get-in data [:其他情况 :内容])))
        (conj! findings "其他肝肾情况:有记录"))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "肝肾病史: 未见明显异常"
          (str "肝肾病史: " (str/join ", " persistent_findings)))))))

(defn liver-kidney-system-summary-view [props]
  (let [{:keys [lk-data]} props
        content (generate-liver-kidney-summary lk-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn liver-kidney-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id lk-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [defaults {:肝功能 {:状态 :正常} ; Using keyword for enum
                                            :肾功能 {:状态 :正常} ; Using keyword for enum
                                            :肝脏疾病病史 {:类型 :无} ; Using keyword for enum
                                            :肾脏疾病病史 {:类型 :无 :慢性肾脏病分期 nil :尿毒症 {:有无透析治疗 :无}}}]; Using keyword for enum
                              (merge defaults (or lk-data {})))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :肝肾病史 values-clj])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :肝肾病史 form)) ; Use new keyword
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
        :form-key (str patient-id "-liver-kidney-system-spec") ; Updated form key
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
(defn generate-digestive-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Acute Gastroenteritis History - Mapped from :acute_gastroenteritis_history to :急性胃肠炎病史
      (let [agh_data (get data :急性胃肠炎病史)
            agh_has (get agh_data :有无)] ; Mapped :has to :有无
        (cond
          (= agh_has :有) (conj! findings (str "急性胃肠炎:有" (let [s (get-in agh_data [:详情 :症状])] (if (seq s) (str "(" (str/join "/" (map name s)) ")") "")))) ; :symptoms to :症状 under :详情
          (= agh_has :无) (conj! findings "急性胃肠炎:无")
          :else (conj! findings "急性胃肠炎:未评估/不祥")))

      ;; Esophageal, Gastric, Duodenal History - Mapped from :esophageal_gastric_duodenal_history to :食管胃十二指肠疾病病史
      (let [egdh_data (get data :食管胃十二指肠疾病病史)
            egdh_has (get egdh_data :有无)] ; Mapped :has to :有无
        (cond
          (= egdh_has :有) (conj! findings (str "食管胃十二指肠疾病:有" (let [s (get-in egdh_data [:详情 :症状])] (if (seq s) (str "(" (str/join "/" (map name s)) ")") "")))) ; :symptoms to :症状 under :详情
          (= egdh_has :无) (conj! findings "食管胃十二指肠疾病:无")
          :else (conj! findings "食管胃十二指肠疾病:未评估")))

      ;; Chronic Digestive History - Mapped from :chronic_digestive_history to :慢性消化疾病病史
      (let [cdh_data (get data :慢性消化疾病病史)
            cdh_has (get cdh_data :有无)] ; Mapped :has to :有无
        (cond
          (= cdh_has :有) (conj! findings (str "慢性消化疾病:有" (let [s (get-in cdh_data [:详情 :症状])] (if (seq s) (str "(" (str/join "/" (map name s)) ")") "")))) ; :symptoms to :症状 under :详情
          (= cdh_has :无) (conj! findings "慢性消化疾病:无")
          :else (conj! findings "慢性消化疾病:未评估/不祥")))

      ;; Other Digestive Conditions - Mapped from :other_conditions to [:其他情况 :内容]
      (when (not (str/blank? (get-in data [:其他情况 :内容])))
        (conj! findings "其他消化系统情况:有记录"))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "消化系统: 未见明显异常"
          (str "消化系统: " (str/join ", " persistent_findings)))))))

(defn digestive-system-summary-view [props]
  (let [{:keys [ds-data]} props
        content (generate-digestive-summary ds-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn digestive-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id ds-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls and local option lists
        initial-form-values (-> (or ds-data {}) ; Base data is already in Chinese keys
                                (update-in [:急性胃肠炎病史 :有无] #(or % :无)) ; Use keyword for enum
                                (update-in [:食管胃十二指肠疾病病史 :有无] #(or % :无))
                                (update-in [:慢性消化疾病病史 :有无] #(or % :无))) ; Corrected key, use enum
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
        :form-key (str patient-id "-digestive-system-spec") ; Updated form key
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
(defn generate-hematologic-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Anemia - Mapped from :anemia to :贫血
      (let [anemia_data (get data :贫血)
            anemia_has (get anemia_data :有无)] ; Mapped :has to :有无
        (cond
          (= anemia_has :有) ; Enum
          (let [hb (get-in anemia_data [:详情 :Hb值])] ; Mapped :hb to :Hb值 under :详情
            (conj! findings (str "贫血:有" (when hb (str "(Hb:" hb "g/L)")))))
          (= anemia_has :无) (conj! findings "贫血:无") ; Enum
          :else (conj! findings "贫血:未评估")))

      ;; Coagulation Dysfunction - Mapped from :coagulation_dysfunction to :凝血功能障碍
      (let [coag_data (get data :凝血功能障碍)
            coag_has (get coag_data :有无)] ; Mapped :has to :有无
        (cond
          (= coag_has :有) ; Enum
          (let [details (transient [])
                coag_details (get coag_data :详情)]
            (when (get coag_details :PT值) (conj! details (str "PT:" (get coag_details :PT值))))
            (when (get coag_details :APTT值) (conj! details (str "APTT:" (get coag_details :APTT值))))
            (when (get coag_details :INR值) (conj! details (str "INR:" (get coag_details :INR值))))
            (when (get coag_details :血小板计数) (conj! details (str "PLT:" (get coag_details :血小板计数))))
            (when (get coag_details :D二聚体值) (conj! details (str "D-dimer:" (get coag_details :D二聚体值))))
            (conj! findings (str "凝血功能障碍:有" (let [pd (persistent! details)] (if (seq pd) (str "(" (str/join ", " pd) ")") "")))))
          (= coag_has :无) (conj! findings "凝血功能障碍:无") ; Enum
          :else (conj! findings "凝血功能障碍:未评估")))

      ;; Thrombosis History - Mapped from :thrombosis_history to :血栓史
      (let [thromb_data (get data :血栓史)
            thromb_has (get thromb_data :有无)] ; Mapped :has to :有无
        (cond
          (= thromb_has :有) (conj! findings (str "血栓史:有" (if-not (str/blank? (get-in thromb_data [:描述])) "(详情)" ""))) ; :details to :描述
          (= thromb_has :无) (conj! findings "血栓史:无") ; Enum
          :else (conj! findings "血栓史:未评估")))

      ;; Lower Limb DVT - Mapped from :lower_limb_dvt to :下肢深静脉血栓
      (let [dvt_data (get data :下肢深静脉血栓)
            dvt_has (get dvt_data :有无)] ; Mapped :has to :有无
        (cond
          (= dvt_has :有) (conj! findings (str "下肢DVT:有" (if-not (str/blank? (get-in dvt_data [:描述])) "(详情)" ""))) ; :details to :描述
          (= dvt_has :无) (conj! findings "下肢DVT:无") ; Enum
          :else (conj! findings "下肢DVT:未评估/不祥")))

      ;; Vascular Ultrasound Results - Mapped from :vascular_ultrasound_results to [:血管超声 :内容]
      (when (not (str/blank? (get-in data [:血管超声 :内容])))
        (conj! findings "血管超声:有记录"))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "血液系统: 未见明显异常"
          (str "血液系统: " (str/join ", " persistent_findings)))))))

(defn hematologic-system-summary-view [props]
  (let [{:keys [hs-data]} props
        content (generate-hematologic-summary hs-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn hematologic-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id hs-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (-> (or hs-data {}) ; Base data is already in Chinese keys
                                (update-in [:贫血 :有无] #(or % :无))
                                (update-in [:凝血功能障碍 :有无] #(or % :无))
                                (update-in [:血栓史 :有无] #(or % :无))
                                (update-in [:下肢深静脉血栓 :有无] #(or % :无)))
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :血液系统 (js->clj values :keywordize-keys true)]))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :血液系统 form)) ; Use new keyword
                       js/undefined)
                     #js [])
    (let [section-spec assessment-specs/血液系统Spec ; Use the spec
          form-items (into
                      [:<>]
                      (mapv (fn [[field-key field-schema optional? _]]
                              (afg/render-form-item-from-spec [field-key field-schema optional? [] form])) ; <--- UPDATED TO afg/
                            (m/entries section-spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-hematologic-system-spec") ; Spec-based key
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
(defn generate-immune-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Immune Dysfunction - Mapped from :immune_dysfunction to :免疫功能障碍
      (let [id_data (get data :免疫功能障碍)
            id_has (get id_data :有无)] ; Mapped :has to :有无
        (cond
          (= id_has :有) ; Enum
          (let [type (get-in id_data [:详情 :类型]) ; Mapped :type to :类型 under :详情
                details (get-in id_data [:详情 :其他类型描述]) ; Mapped :type_other_details to :其他类型描述 under :详情
                type_str (if (and type (= type :其他) (not (str/blank? details))) ; Check for :其他 enum
                           (str "其他(" details ")")
                           (name type))]
            (conj! findings (str "免疫功能障碍:有" (when type_str (str "(" type_str ")")))))
          (= id_has :无) (conj! findings "免疫功能障碍:无") ; Enum
          :else (conj! findings "免疫功能障碍:未评估/不祥")))

      ;; Autoimmune Disease - Mapped from :autoimmune_disease to :自身免疫性疾病
      (let [ad_data (get data :自身免疫性疾病)
            ad_has (get ad_data :有无)] ; Mapped :has to :有无
        (cond
          (= ad_has :有) ; Enum
          (let [symptoms (get-in ad_data [:详情 :症状]) ; Mapped :symptoms to :症状 under :详情
                symptoms_details (get-in ad_data [:详情 :其他症状描述]) ; Mapped :symptoms_other_details to :其他症状描述 under :详情
                symptoms_display (when (seq symptoms)
                                   (mapv #(if (= % :其他) ; Check for :其他 enum
                                            (if (not (str/blank? symptoms_details))
                                              (str "其他(" symptoms_details ")")
                                              "其他")
                                            (name %)) ; Use name for other enums
                                         symptoms))]
            (conj! findings (str "自身免疫性疾病:有" (if (seq symptoms_display) (str "(" (str/join "/" symptoms_display) ")") ""))))
          (= ad_has :无) (conj! findings "自身免疫性疾病:无") ; Enum
          :else (conj! findings "自身免疫性疾病:未评估/不祥")))

      ;; Other Immune Conditions - Mapped from :other_immune_conditions to [:其他情况 :内容]
      (when (not (str/blank? (get-in data [:其他情况 :内容])))
        (conj! findings "其他免疫系统情况:有记录"))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "免疫系统: 未见明显异常"
          (str "免疫系统: " (str/join ", " persistent_findings)))))))

(defn immune-system-summary-view [props]
  (let [{:keys [is-data]} props
        content (generate-immune-summary is-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn immune-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id is-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls and local option lists
        initial-form-values (-> (or is-data {}) ; Base data is already in Chinese keys
                                (update-in [:免疫功能障碍 :有无] #(or % :无)) ; Use keyword for enum
                                (update-in [:自身免疫性疾病 :有无] #(or % :无))) ; Use keyword for enum
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
        :form-key (str patient-id "-immune-system-spec") ; Updated form key
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        ;; :on-values-change-handler on-values-change-fn ; Removed for now
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
(defn generate-special-medication-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])
          ;; Spec keys for Special Medication History:
          ;; :抗凝或抗血小板药物, :糖皮质激素, :肿瘤治疗, :药物滥用依赖史, :神经安定类药物, :GLP1受体激动剂, :其他药物使用
          med_fields [[:抗凝或抗血小板药物 "抗凝/抗血小板"]
                      [:糖皮质激素 "糖皮质激素"]
                      [:肿瘤治疗 "肿瘤治疗"] ; Changed from :肿瘤治疗药物
                      [:药物滥用依赖史 "药物滥用依赖"] ; Changed from :毒麻及精神类药物滥用或依赖
                      [:神经安定类药物 "神经安定类药物"]
                      [:GLP1受体激动剂 "GLP-1激动剂"]]]
      (doseq [[med-key med-name] med_fields]
        (let [med_present (get-in data [med-key :有无])] ; Mapped :present to :有无
          (when (= med_present :有) ; Enum
            (conj! findings (str med-name ":有" (if-not (str/blank? (get-in data [med-key :描述])) "(详情)" "")))))) ; :details to :描述

      ;; Other Drug Use - Mapped from :other_drug_use to [:其他药物使用 :内容]
      (let [other_drug_use_details (get-in data [:其他药物使用 :内容])]
        (when (not (str/blank? other_drug_use_details))
          (conj! findings (str "其他药物使用:" other_drug_use_details))))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "特殊用药史: 无特殊用药记录"
          (str "特殊用药史: " (str/join ", " persistent_findings)))))))

(defn special-medication-history-summary-view [props]
  (let [{:keys [smh-data]} props
        content (generate-special-medication-summary smh-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn special-medication-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id smh-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls
        initial-form-values (let [defaults {:抗凝或抗血小板药物 {:有无 :无} ; Use keyword for enum
                                            :糖皮质激素 {:有无 :无}
                                            :肿瘤治疗 {:有无 :无} ; Updated key
                                            :药物滥用依赖史 {:有无 :无} ; Updated key
                                            :神经安定类药物 {:有无 :无}
                                            :GLP1受体激动剂 {:有无 :无}}]
                              (merge defaults (or smh-data {}))) ; Base data is already in Chinese keys
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
        :form-key (str patient-id "-special-medication-history-spec") ; Updated form key
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        ;; :on-values-change-handler on-values-change-fn ; Removed
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
(defn generate-special-disease-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Marfan Syndrome - Mapped from :marfan_syndrome to :马方综合征
      (let [marfan_data (get data :马方综合征)
            marfan_present (get marfan_data :有无)] ; Mapped :present to :有无
        (cond
          (= marfan_present :有) ; Enum
          (let [details (get marfan_data :详情)
                eye_lesion (get details :眼部病变晶状体脱位) ; Boolean from spec
                cv_lesions (get details :心血管病变) ; Vector of enums
                sk_lesions (get details :骨骼病变) ; Vector of enums
                lesion_parts (transient [])]
            (when eye_lesion (conj! lesion_parts "眼部病变(晶状体脱位)"))
            (when (seq cv_lesions) (conj! lesion_parts (str "心血管(" (str/join "/" (map name cv_lesions)) ")")))
            (when (seq sk_lesions) (conj! lesion_parts (str "骨骼(" (str/join "/" (map name sk_lesions)) ")")))
            (conj! findings (str "马方综合征:有" (let [pl (persistent! lesion_parts)] (if (seq pl) (str "(" (str/join ", " pl) ")") "")))))
          (= marfan_present :不详) (conj! findings "马方综合征:不详"))) ; Enum

      ;; Other Special Diseases - Mapped from :other_special_diseases to [:其他特殊疾病 :内容]
      (let [other_details_str (get-in data [:其他特殊疾病 :内容])]
        (when (not (str/blank? other_details_str))
          (conj! findings (str "其他特殊疾病:" other_details_str))))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "特殊疾病病史: 无特殊疾病记录"
          (str "特殊疾病病史: " (str/join ", " persistent_findings)))))))

(defn special-disease-history-summary-view [props]
  (let [{:keys [sdh-data]} props
        content (generate-special-disease-summary sdh-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn special-disease-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sdh-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch call for marfan-lesions
        initial-form-values (let [defaults {:马方综合征 {:有无 :无}}] ; Use keyword for enum
                              (merge defaults (or sdh-data {}))) ; Base data is already in Chinese keys
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
        :form-key (str patient-id "-special-disease-history-spec") ; Updated form key
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        ;; :on-values-change-handler on-values-change-fn ; Removed for now
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
(defn generate-nutritional-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Nutritional Score - Mapped from :nutritional_score to :营养评分
      (let [ns_data (get data :营养评分)]
        (if ns_data
          (let [bmi_risk (= :有 (get ns_data :BMI小于20点5)) ; Mapped to spec keys, use enum
                weight_loss_risk (= :有 (get ns_data :过去1至3个月体重下降))
                intake_risk (= :有 (get ns_data :过去1周摄食减少))
                illness_risk (= :有 (get ns_data :有严重疾病如ICU治疗))
                num_ns_risks (+ (if bmi_risk 1 0) (if weight_loss_risk 1 0) (if intake_risk 1 0) (if illness_risk 1 0))
                risk_level (get ns_data :风险等级)] ; Get risk level from data
            (if risk_level
              (conj! findings (str "营养评分:" (name risk_level) "(" num_ns_risks "项阳性)"))
              (if (>= num_ns_risks 2)
                (conj! findings (str "营养评分:有风险(" num_ns_risks "项阳性)"))
                (conj! findings "营养评分:无明显风险"))))
          (conj! findings "营养评分:未评估")))

      ;; FRAIL Score - Mapped from :frail_score to :FRAIL针对大于60岁病人
      (let [fs_data (get data :FRAIL针对大于60岁病人)]
        (if fs_data
          (let [fatigue (= :有 (get fs_data :疲乏)) ; Mapped to spec keys, use enum
                resistance (= :有 (get fs_data :阻力增加或耐力减退))
                ambulation (= :有 (get fs_data :自由活动下降))
                illness_gt_5 (= :有 (get fs_data :存在5种以上疾病))
                loss_of_weight (= :有 (get fs_data :体重下降1年或更短内大于5百分比))
                num_fs_risks (+ (if fatigue 1 0) (if resistance 1 0) (if ambulation 1 0) (if illness_gt_5 1 0) (if loss_of_weight 1 0))
                conclusion (get fs_data :结论)] ; Get conclusion from data
            (if conclusion
              (conj! findings (str "FRAIL评估:" (name conclusion) "(" num_fs_risks "分)"))
              (cond
                (>= num_fs_risks 3) (conj! findings (str "FRAIL评估:衰弱(" num_fs_risks "分)"))
                (and (>= num_fs_risks 1) (<= num_fs_risks 2)) (conj! findings (str "FRAIL评估:衰弱前期(" num_fs_risks "分)"))
                :else (conj! findings "FRAIL评估:健康(0分)"))))
          (conj! findings "FRAIL评估:未评估")))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "营养评估: 数据不完整或未评估"
          (str "营养评估: " (str/join ", " persistent_findings)))))))

(defn nutritional-assessment-summary-view [props]
  (let [{:keys [na-data]} props
        content (generate-nutritional-summary na-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn nutritional-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id na-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (-> (or na-data {}) ; Base data is already in Chinese keys
                                ;; Paths within :营养评分
                                (update-in [:营养评分 :BMI小于20点5] #(or % :无)) ; Use keyword for enum
                                (update-in [:营养评分 :过去1至3个月体重下降] #(or % :无))
                                (update-in [:营养评分 :过去1周摄食减少] #(or % :无))
                                (update-in [:营养评分 :有严重疾病如ICU治疗] #(or % :无))
                                ;; Paths within :FRAIL针对大于60岁病人
                                (update-in [:FRAIL针对大于60岁病人 :疲乏] #(or % :无))
                                (update-in [:FRAIL针对大于60岁病人 :阻力增加或耐力减退] #(or % :无))
                                (update-in [:FRAIL针对大于60岁病人 :自由活动下降] #(or % :无))
                                (update-in [:FRAIL针对大于60岁病人 :存在5种以上疾病] #(or % :无))
                                (update-in [:FRAIL针对大于60岁病人 :体重下降1年或更短内大于5百分比] #(or % :无)))
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
      ;; The spec has two main keys: :营养评分 and :FRAIL针对大于60岁病人
      ;; These will be rendered as map sections by afg/render-conditional-map-section
      ;; because they don't have the :有无/:详情 pattern, their fields will be rendered directly.
      ;; We also add the original descriptive texts as non-form elements.
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-nutritional-assessment-spec") ; Updated form key
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
(defn generate-pregnancy-summary [data]
  (if (or (nil? data) (empty? data))
    "妊娠状态: 未知(无数据)"
    ;; Spec root key is :妊娠, with sub-keys like :是否妊娠
    (let [is_pregnant (get-in data [:是否妊娠])] ; Mapped :is_pregnant to :是否妊娠
      (cond
        (= is_pregnant :有) ; Enum
        (let [summary_parts (transient ["妊娠:是"])
              details (get data :详情) ; Get the :详情 map
              gest_week (get details :孕周) ; Mapped :gestational_week to :孕周
              obstetric_history_map (get details :孕产史) ; Mapped :obstetric_history to :孕产史 (map)
              comorbid_conditions (get details :合并产科情况) ; Mapped :comorbid_obstetric_conditions to :合并产科情况 (list of enums)
              other_comorbid_details (get details :其他产科情况描述) ; Mapped :comorbid_obstetric_conditions_other_details to :其他产科情况描述
              other_preg_conditions (get-in data [:其他情况 :内容])] ; Mapped :other_pregnancy_conditions to [:其他情况 :内容]

          (if gest_week
            (conj! summary_parts (str "孕周:" (name gest_week)))
            (conj! summary_parts "孕周:未提供"))

          (when obstetric_history_map
            (let [oh_str (str "G" (or (:足月 obstetric_history_map) 0)
                              "P" (or (:早产 obstetric_history_map) 0)
                              "A" (or (:流产 obstetric_history_map) 0)
                              "L" (or (:存活 obstetric_history_map) 0))]
              (conj! summary_parts (str "孕产史:" oh_str))))

          (if (seq comorbid_conditions)
            (let [display_comorbid (mapv #(if (= % :其他) ; Enum
                                            (if (not (str/blank? other_comorbid_details))
                                              (str "其他(" other_comorbid_details ")")
                                              "其他产科情况")
                                            (name %)) ; Enum
                                         comorbid_conditions)]
              (conj! summary_parts (str "合并产科情况:" (str/join "/" display_comorbid))))
            (conj! summary_parts "合并产科情况:无"))

          (when (not (str/blank? other_preg_conditions))
            (conj! summary_parts (str "其他妊娠相关:" other_preg_conditions)))
          (str/join ", " (persistent! summary_parts)))

        (= is_pregnant :无) "妊娠:否" ; Enum
        :else "妊娠状态:不祥/未评估"))))

(defn pregnancy-assessment-summary-view [props]
  (let [{:keys [pa-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-pregnancy-summary pa-data)]))

(defn pregnancy-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id pa-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Spec path: [:妊娠]
        ;; Removed local option lists and useWatch calls
        initial-form-values (let [base-data (or pa-data {}) ; Base data is already in Chinese keys
                                  default-values {:是否妊娠 :无}] ; Default from original logic, use enum
                              (merge default-values base-data))
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
        :form-key (str patient-id "-pregnancy-assessment-spec") ; Updated form key
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
(defn generate-surgical-anesthesia-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    ;; Spec root key is :手术麻醉史
    (let [history_data (get data :手术麻醉史)
          history_present (get history_data :有无) ; Mapped to :有无
          family_hyperthermia_data (get data :有血缘关系的人发生过恶性高热史)
          family_hyperthermia_present (get family_hyperthermia_data :有无) ; Mapped to :有无
          parts [(str "手术麻醉史:" (cond (= history_present :有) "有" (= history_present :不详) "不详" :else "无")) ; Enums
                 (str "恶性高热家族史:" (if (= family_hyperthermia_present :有) "有" "无"))]] ; Enums
      (str/join ", " parts))))

(defn surgical-anesthesia-history-summary-view [props]
  (let [{:keys [sah-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-surgical-anesthesia-summary sah-data)]))

(defn surgical-anesthesia-history-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id sah-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Spec path: [:手术麻醉史]
        ;; Removed local option lists and useWatch calls
        initial-form-values (let [base-data (when sah-data ; Base data is already in Chinese keys
                                              (-> sah-data
                                                  (update-in [:手术麻醉史 :详情 :具体上次麻醉日期] #(when % (utils/parse-date %)))))
                                  default-values {:手术麻醉史 {:有无 :无} ; Use enums
                                                  :有血缘关系的人发生过恶性高热史 {:有无 :无}}] ; Use enums
                              (merge default-values (or base-data {})))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj ; Paths are already Chinese
                                                    (update-in [:手术麻醉史 :详情 :具体上次麻醉日期] #(when % (utils/date->iso-string %))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :手术麻醉史 transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn :手术麻醉史 form)) ; Use spec keyword
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
        :form-key (str patient-id "-surgical-anesthesia-history-spec") ; Updated form key
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
(defn generate-airway-summary [data]
  ;; Spec root key is :气道评估
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [assessment data ; Data is already :气道评估
          diff-intubation (get assessment :既往困难插管史) ; Mapped
          mallampati (get assessment :改良Mallampati分级) ; Mapped
          mouth-opening (get-in assessment [:张口度 :分级]) ; Mapped
          thyromental-dist (get assessment :甲颏距离cm) ; Mapped (value not class)
          parts (cond-> []
                  (and diff-intubation (not= diff-intubation :无) (not= diff-intubation :不详) (not (nil? diff-intubation))) ; Enums
                  (conj (str "困难插管史:" (name diff-intubation)))
                  mallampati
                  (conj (str "Mallampati:" (name mallampati))) ; Enum
                  (and mouth-opening (not= mouth-opening :大于等于3横指)) ; Enum
                  (conj (str "张口度:" (name mouth-opening)))
                  (and thyromental-dist (< thyromental-dist 6.0)) ; Assuming number, check spec (OptionalNumber)
                  (conj (str "甲颏距离:" thyromental-dist "cm")))]
      (if (empty? parts)
        "气道评估: 未见明显异常"
        (str "气道评估: " (str/join ", " parts))))))

(defn airway-assessment-summary-view [props]
  (let [{:keys [aa-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-airway-summary aa-data)]))

(defn airway-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id aa-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed local option lists and useWatch calls from original implementation
        initial-form-values (let [base-data (or aa-data {}) ; Base data is already in Chinese keys
                                  defaults {:既往困难通气史 :不详 ; Enums
                                            :既往困难插管史 :不详
                                            :张口度 {:分级 :大于等于3横指}
                                            :甲颏距离cm nil
                                            :头颈活动度 {:分级 :正常活动}
                                            :改良Mallampati分级 :Ⅰ级
                                            :上唇咬合试验ULBT :1级 ; Note: Spec uses 上唇咬合试验
                                            :鼾症 {:有无 :不详}
                                            :气道相关疾病 {:有无 :不详}
                                            :现存气道症状 {:有无 :不详}
                                            :食管手术史 {:有无 :不详 :是否存在返流 false}}]
                              (merge defaults base-data))
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
        :form-key (str patient-id "-airway-assessment-spec") ; Updated form key
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
(defn generate-spinal-anesthesia-summary [data]
  ;; Spec root key is :椎管内麻醉相关评估
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [assessment data ; Data is already :椎管内麻醉相关评估
          critical-fields {[:中枢神经系统 :脑肿瘤] "脑肿瘤"
                           [:外周神经系统 :脊髓损伤] "脊髓损伤"
                           [:腰椎间盘突出 :有无] "腰椎间盘突出" ; Check for :有无
                           [:心血管系统 :主动脉瓣狭窄] "主动脉瓣狭窄"
                           [:穿刺点检查 :局部感染] "穿刺点感染"
                           [:局麻药过敏] "局麻药过敏"} ; Direct key
          risk-factors (reduce (fn [acc [path label]]
                                 (let [value (if (coll? path) (get-in assessment path) (get assessment path))]
                                   (if (or (= value :有) (= value true)) ; Check for :有 enum or true boolean
                                     (conj acc label)
                                     acc)))
                               []
                               critical-fields)]
      (if (seq risk-factors)
        (str "椎管内麻醉风险: " (str/join ", " risk-factors))
        "椎管内麻醉评估: 无明确风险因素"))))

(defn spinal-anesthesia-assessment-summary-view [props]
  (let [{:keys [saa-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-spinal-anesthesia-summary saa-data)]))

(defn spinal-anesthesia-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id saa-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [base-data (or saa-data {}) ; Base data is already in Chinese keys
                                  ;; Map original defaults to new spec paths.
                                  defaults {:中枢神经系统 {:脑肿瘤 :无, :脑出血 :无, :严重颅脑外伤 :无, :癫痫 :无} ; Enums
                                            :外周神经系统 {:多发性硬化 :无, :脊髓损伤 :无, :脊柱侧弯 :无, :脊柱畸形 :无,
                                                           :椎管内肿瘤 :无, :强制性脊柱炎 :无, :腰椎手术史 :无}
                                            :腰椎间盘突出 {:有无 :无, :下肢麻木症状 :无}
                                            :心血管系统 {:主动脉瓣狭窄 :无, :肥厚型梗阻型心肌病 :无,
                                                         :抗凝或抗血小板药物 {:有无 :无}}
                                            :穿刺点检查 {:既往穿刺困难史 :无, :局部感染 :无, :畸形 :无}
                                            :局麻药过敏 :无}]
                              (merge defaults base-data))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)]
                         (rf/dispatch [::events/update-canonical-assessment-section :椎管内麻醉相关评估 values-clj])))]
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
        :form-key (str patient-id "-spinal-anesthesia-assessment-spec") ; Updated form key
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        ;; :on-values-change-handler ; Removed
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
