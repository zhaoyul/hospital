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
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Data-driven rendering helpers have been moved to hc.hospital.pages.assessment-form-generators

(defn- generate-circulatory-summary "循环系统总结" [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; ECG Description
      (when (not (str/blank? (get-in data [:ecg_description])))
        (conj! findings "ECG描述:有记录"))

      ;; Cardiac Disease History
      (let [cardiac_history_data (get data :cardiac_disease_history)
            cdh_has (get cardiac_history_data :has)]
        (cond
          (= cdh_has "有")
          (let [specific_cardiac_issues (transient [])
                cad_data (get cardiac_history_data :coronary_artery_disease)
                arr_data (get cardiac_history_data :arrhythmia)]
            ;; CAD
            (when (= (get cad_data :has) "有")
              (let [symptoms (get cad_data :symptoms)]
                (cond
                  (= symptoms "心梗") (conj! specific_cardiac_issues "冠心病(心梗)")
                  (= symptoms "不稳定性心绞痛") (conj! specific_cardiac_issues "冠心病(不稳定性心绞痛)")
                  :else (conj! specific_cardiac_issues "冠心病"))))
            ;; Arrhythmia
            (when (= (get arr_data :has) "有")
              (let [arr_type (get arr_data :type)]
                (if (or (= arr_type "高危型") (= arr_type "中危型"))
                  (conj! specific_cardiac_issues (str "心律失常(" arr_type ")"))
                  (conj! specific_cardiac_issues "心律失常"))))
            ;; Cardiomyopathy
            (when (= (get-in cardiac_history_data [:cardiomyopathy :has]) "有")
              (conj! specific_cardiac_issues "心肌病"))
            ;; Valvular Heart Disease
            (when (= (get-in cardiac_history_data [:valvular_heart_disease :has]) "有")
              (conj! specific_cardiac_issues "瓣膜病"))
            ;; Congenital Heart Disease
            (when (= (get-in cardiac_history_data [:congenital_heart_disease :has]) "有")
              (conj! specific_cardiac_issues "先心病"))
            ;; Congestive Heart Failure
            (when (= (get-in cardiac_history_data [:congestive_heart_failure :has]) "有")
              (conj! specific_cardiac_issues "心衰史"))
            ;; Pulmonary Hypertension
            (when (= (get-in cardiac_history_data [:pulmonary_hypertension :has]) "有")
              (conj! specific_cardiac_issues "肺动脉高压"))

            (let [persistent_specific_issues (persistent! specific_cardiac_issues)]
              (if (seq persistent_specific_issues)
                (conj! findings (str "心脏病史:异常 (" (str/join ", " persistent_specific_issues) ")"))
                (conj! findings "心脏病史:异常"))))

          (= cdh_has "无")
          (conj! findings "心脏病史:无")

          :else ; nil or other
          (conj! findings "心脏病史:未评估")))

      ;; Pacemaker History
      (let [pm_has (get-in data [:pacemaker_history :has])
            pm_type (get-in data [:pacemaker_history :type])]
        (cond
          (= pm_has "有") (conj! findings (str "起搏器:有" (when pm_type (str "(" pm_type ")"))))
          (= pm_has "无") (conj! findings "起搏器:无")
          (= pm_has "不祥") (conj! findings "起搏器:不祥")))

      ;; Cardiac Ultrasound Findings
      (when (not (str/blank? (get-in data [:cardiac_ultrasound_findings :details])))
        (conj! findings "心脏彩超:有记录"))

      ;; Coronary CTA/Angiography
      (when (not (str/blank? (get-in data [:coronary_cta_angiography_results :details])))
        (conj! findings "冠脉CTA/造影:有记录"))

      ;; Cardiac Function Assessment
      (let [cardiac_function (get-in data [:cardiac_function_assessment :class])]
        (cond
          (or (nil? cardiac_function) (= cardiac_function "未评估")) (conj! findings "心功能:未评估")
          (= cardiac_function "Ⅰ 级") (conj! findings "心功能:正常(Ⅰ级)")
          :else (conj! findings (str "心功能:" cardiac_function "(异常)"))))

      ;; Exercise Capacity Assessment
      (let [exercise_capacity (get-in data [:exercise_capacity_assessment :level])]
        (cond
          (or (nil? exercise_capacity) (= exercise_capacity "未评估")) (conj! findings "运动能力:未评估")
          (= exercise_capacity "运动能力正常") (conj! findings "运动能力:正常")
          :else (conj! findings (str "运动能力:" exercise_capacity "(异常)"))))

      ;; Other Cardiac Info
      (when (not (str/blank? (get-in data [:other_cardiac_info :details])))
        (conj! findings "其他循环系统情况:有记录"))

      ;; Final Summary
      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "循环系统: 未见明显异常"
          (str "循环系统: " (str/join ", " persistent_findings)))))))

(defn- circulatory-system-summary-view [props]
  (let [{:keys [circulatory-data]} props
        content (generate-circulatory-summary circulatory-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn- circulatory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id circulatory-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [base-data (when circulatory-data
                                              (-> circulatory-data
                                                  (update-in [:心脏疾病史 :详情 :充血性心力衰竭史 :上次发作日期] #(when % (utils/parse-date %)))))
                                  default-values {:心脏疾病史 {:有无 "无"
                                                               :详情 {:冠心病 {:有无 "无"}
                                                                      :心律失常 {:有无 "无"}
                                                                      :心肌病 {:有无 "无"}
                                                                      :瓣膜性心脏病 {:有无 "无"}
                                                                      :先天性心脏病 {:有无 "无"}
                                                                      :充血性心力衰竭史 {:有无 "无"}
                                                                      :肺动脉高压 {:有无 "无"}}}
                                                  :心脏起搏器植入史 {:有无 "无"}
                                                  :心脏功能评估 {:NYHA分级 "Ⅰ级"}
                                                  :运动能力评估 {:METs水平 "运动能力正常"}}
                                  merged-data (merge default-values (or base-data {}))]
                              ;; Apply defaults specifically for :有无 fields if details exist but :有无 is nil
                              (cond-> merged-data
                                (and (get-in merged-data [:心脏疾病史 :详情]) (nil? (get-in merged-data [:心脏疾病史 :有无])))
                                (assoc-in [:心脏疾病史 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :冠心病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :冠心病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :冠心病 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :心律失常]) (nil? (get-in merged-data [:心脏疾病史 :详情 :心律失常 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :心律失常 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :心肌病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :心肌病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :心肌病 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :瓣膜性心脏病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :瓣膜性心脏病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :瓣膜性心脏病 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :先天性心脏病]) (nil? (get-in merged-data [:心脏疾病史 :详情 :先天性心脏病 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :先天性心脏病 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :充血性心力衰竭史]) (nil? (get-in merged-data [:心脏疾病史 :详情 :充血性心力衰竭史 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :充血性心力衰竭史 :有无] "有")
                                (and (get-in merged-data [:心脏疾病史 :详情 :肺动脉高压]) (nil? (get-in merged-data [:心脏疾病史 :详情 :肺动脉高压 :有无])))
                                (assoc-in [:心脏疾病史 :详情 :肺动脉高压 :有无] "有")
                                (and (get-in merged-data [:心脏起搏器植入史 :详情]) (nil? (get-in merged-data [:心脏起搏器植入史 :有无])))
                                (assoc-in [:心脏起搏器植入史 :有无] "有")))
        _ (timbre/info "circulatory-system-detailed-view: initial-form-values:" (clj->js initial-form-values))
        on-finish-fn (fn [values]
                       (timbre/info "circulatory-system-detailed-view: on-finish-fn raw JS values:" values)
                       (let [values-clj (js->clj values :keywordize-keys true)
                             _ (timbre/info "circulatory-system-detailed-view: on-finish-fn cljs values-clj:" (clj->js values-clj))
                             transformed-values (-> values-clj
                                                    (update-in [:心脏疾病史 :详情 :充血性心力衰竭史 :上次发作日期] #(when % (utils/date->iso-string %))))]
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

(defn- generate-respiratory-summary [data]
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [findings (transient [])]
      ;; Cold History
      (let [cold_data (get data :cold_history_last_2_weeks)
            cold_present (get cold_data :present)]
        (cond
          (= cold_present "有")
          (let [symptoms (get cold_data :symptoms)
                symptoms_str (if (seq symptoms) (str "(" (str/join "/" symptoms) ")") "")]
            (conj! findings (str "近期感冒:有" symptoms_str)))
          (= cold_present "无")
          (conj! findings "近期感冒:无")
          :else
          (conj! findings "近期感冒:未评估/不祥")))

      ;; Bronchitis/Pneumonia
      (let [bp_data (get data :bronchitis_pneumonia_last_month)
            bp_present (get bp_data :present)]
        (cond
          (= bp_present "有")
          (let [treatment (get bp_data :treatment_status)]
            (conj! findings (str "近期支气管炎/肺炎:有" (when treatment (str "(" treatment ")")))))
          (= bp_present "无")
          (conj! findings "近期支气管炎/肺炎:无")
          :else
          (conj! findings "近期支气管炎/肺炎:未评估/不祥")))

      ;; Asthma History
      (let [asthma_data (get data :asthma_history)
            asthma_present (get asthma_data :present)]
        (cond
          (= asthma_present "有")
          (let [med_status (get asthma_data :medication_status)]
            (conj! findings (str "哮喘病史:有" (when med_status (str "(" med_status ")")))))
          (= asthma_present "无")
          (conj! findings "哮喘病史:无")
          :else
          (conj! findings "哮喘病史:未评估/不祥")))

      ;; COPD History
      (let [copd_data (get data :copd_history)
            copd_present (get copd_data :present)]
        (cond
          (= copd_present "有")
          (let [treatment (get copd_data :treatment_status)]
            (conj! findings (str "COPD病史:有" (when treatment (str "(" treatment ")")))))
          (= copd_present "无")
          (conj! findings "COPD病史:无")
          :else
          (conj! findings "COPD病史:未评估/不祥")))

      ;; Bronchiectasis History
      (let [bronch_data (get data :bronchiectasis_history)
            bronch_present (get bronch_data :present)]
        (cond
          (= bronch_present "有")
          (let [treatment (get bronch_data :treatment_status)]
            (conj! findings (str "支气管扩张:有" (when treatment (str "(" treatment ")")))))
          (= bronch_present "无")
          (conj! findings "支气管扩张:无")
          :else
          (conj! findings "支气管扩张:未评估/不祥")))

      ;; Pulmonary Nodules History
      (let [nod_data (get data :pulmonary_nodules_history)
            nod_present (get nod_data :present)]
        (cond
          (= nod_present "有")
          (let [treatment (get nod_data :treatment_status)]
            (conj! findings (str "肺部结节:有" (when treatment (str "(" treatment ")")))))
          (= nod_present "无")
          (conj! findings "肺部结节:无")
          :else
          (conj! findings "肺部结节:未评估/不祥")))

      ;; Lung Tumor History
      (let [tumor_data (get data :lung_tumor_history)
            tumor_present (get tumor_data :present)]
        (cond
          (= tumor_present "有")
          (let [treatment (get tumor_data :treatment_status)]
            (conj! findings (str "肺部肿瘤:有" (when treatment (str "(" treatment ")")))))
          (= tumor_present "无")
          (conj! findings "肺部肿瘤:无")
          :else
          (conj! findings "肺部肿瘤:未评估/不祥")))

      ;; Chest X-ray
      (when (not (str/blank? (get data :chest_xray_results)))
        (conj! findings "胸片:有记录"))

      ;; Chest CT
      (when (not (str/blank? (get data :chest_ct_results)))
        (conj! findings "胸部CT:有记录"))

      ;; Pulmonary Function
      (when (not (str/blank? (get data :pulmonary_function_test_results)))
        (conj! findings "肺功能:有记录"))

      ;; Blood Gas Analysis
      (when (not (str/blank? (get data :blood_gas_analysis_results)))
        (conj! findings "血气分析:有记录"))

      ;; Tuberculosis History
      (let [tb_data (get data :tuberculosis_history)
            tb_present (get tb_data :present)]
        (cond
          (= tb_present "有")
          (let [treatment (get tb_data :treatment_status)
                infectious (get tb_data :infectious)
                details (transient [])]
            (when treatment (conj! details (str "治疗:" treatment)))
            (when infectious (conj! details (str "传染性:" infectious)))
            (conj! findings (str "肺结核:有" (if (seq (persistent! details)) (str "(" (str/join ", " (persistent! details)) ")") ""))))
          (= tb_present "无")
          (conj! findings "肺结核:无")
          :else
          (conj! findings "肺结核:未评估")))

      ;; Other Respiratory Conditions
      (when (not (str/blank? (get data :other_respiratory_conditions)))
        (conj! findings "其他呼吸系统情况:有记录"))

      ;; Final Summary
      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "呼吸系统: 未见明显异常"
          (str "呼吸系统: " (str/join ", " persistent_findings)))))))

(defn respiratory-system-summary-view [props]
  (let [{:keys [respiratory-data]} props
        content (generate-respiratory-summary respiratory-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn respiratory-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id respiratory-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed local cold-symptom-options, treatment-options
        initial-form-values (let [base-data (when respiratory-data
                                              (-> respiratory-data
                                                  (assoc-in [:cold_history_last_2_weeks :onset_date]
                                                            (utils/parse-date (get-in respiratory-data [:cold_history_last_2_weeks :onset_date])))
                                                  (assoc-in [:bronchitis_pneumonia_last_month :onset_date]
                                                            (utils/parse-date (get-in respiratory-data [:bronchitis_pneumonia_last_month :onset_date])))
                                                  (assoc-in [:asthma_history :last_episode_date]
                                                            (utils/parse-date (get-in respiratory-data [:asthma_history :last_episode_date])))))]
                              (-> (or base-data {})
                                  (update-in [:cold_history_last_2_weeks :present] #(or % "无"))
                                  (update-in [:bronchitis_pneumonia_last_month :present] #(or % "无"))
                                  (update-in [:asthma_history :present] #(or % "无"))
                                  (update-in [:copd_history :present] #(or % "无"))
                                  (update-in [:bronchiectasis_history :present] #(or % "无"))
                                  (update-in [:pulmonary_nodules_history :present] #(or % "无"))
                                  (update-in [:lung_tumor_history :present] #(or % "无"))
                                  (update-in [:tuberculosis_history :present] #(or % "无"))
                                  (update-in [:tuberculosis_history :infectious] #(or % "无"))))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj
                                                    (update-in [:近两周内感冒病史 :详情 :发病日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:近一个月内支气管炎或肺炎病史 :详情 :发病日期] #(when % (utils/date->iso-string %)))
                                                    (update-in [:哮喘病史 :详情 :上次发作日期] #(when % (utils/date->iso-string %))))]
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
      ;; Psycho-cognitive History
      (let [pc_data (get data :psycho_cognitive_history)
            pc_present (get pc_data :present)]
        (cond
          (= pc_present "有") (conj! findings (str "精神认知史:有" (let [symptoms (get pc_data :symptoms)] (if (seq symptoms) (str "(" (str/join "/" symptoms) ")") ""))))
          (= pc_present "无") (conj! findings "精神认知史:无")
          :else (conj! findings "精神认知史:未评估/不祥")))

      ;; Epilepsy History
      (let [ep_data (get data :epilepsy_history)
            ep_present (get ep_data :present)]
        (cond
          (= ep_present "有") (conj! findings (str "癫痫史:有" (when-let [status (get ep_data :treatment_status)] (str "(" status ")"))))
          (= ep_present "无") (conj! findings "癫痫史:无")
          :else (conj! findings "癫痫史:未评估/不祥")))

      ;; Vertigo History
      (let [vt_data (get data :vertigo_history)
            vt_present (get vt_data :present)]
        (cond
          (= vt_present "有") (conj! findings (str "眩晕史:有" (when-let [status (get vt_data :treatment_status)] (str "(" status ")"))))
          (= vt_present "无") (conj! findings "眩晕史:无")
          :else (conj! findings "眩晕史:未评估/不祥")))

      ;; TIA History
      (let [tia_data (get data :tia_history)
            tia_present (get tia_data :present)]
        (cond
          (= tia_present "有") (conj! findings (str "TIA史:有" (when-let [status (get tia_data :recent_onset_status)] (str "(" status ")"))))
          (= tia_present "无") (conj! findings "TIA史:无")
          :else (conj! findings "TIA史:未评估/不祥")))

      ;; Cerebral Infarction History
      (let [ci_data (get data :cerebral_infarction_history)
            ci_present (get ci_data :present)]
        (cond
          (= ci_present "有") (conj! findings (str "脑梗史:有" (when-let [status (get ci_data :treatment_status)] (str "(" status ")"))))
          (= ci_present "无") (conj! findings "脑梗史:无")
          :else (conj! findings "脑梗史:未评估")))

      ;; Cerebral Hemorrhage History
      (let [ch_data (get data :cerebral_hemorrhage_history)
            ch_present (get ch_data :present)]
        (cond
          (= ch_present "有") (conj! findings (str "脑出血史:有" (when-let [status (get ch_data :treatment_status)] (str "(" status ")"))))
          (= ch_present "无") (conj! findings "脑出血史:无")
          :else (conj! findings "脑出血史:未评估")))

      ;; Parkinson's Syndrome
      (let [ps_data (get data :parkinsons_syndrome)
            ps_present (get ps_data :present)]
        (cond
          (= ps_present "有") (conj! findings (str "帕金森综合征:有" (when-let [status (get ps_data :treatment_status)] (str "(" status ")"))))
          (= ps_present "无") (conj! findings "帕金森综合征:无")
          :else (conj! findings "帕金森综合征:未评估")))

      ;; Cranial/Carotid Stenosis
      (let [ccs_present (get-in data [:cranial_carotid_stenosis :present])]
        (cond
          (= ccs_present "有") (conj! findings (str "颅脑/颈动脉狭窄:有" (if-not (str/blank? (get-in data [:cranial_carotid_stenosis :details])) "(详情)" "")))
          (= ccs_present "无") (conj! findings "颅脑/颈动脉狭窄:无")
          (= ccs_present "不祥") (conj! findings "颅脑/颈动脉狭窄:不祥")))

      ;; Other Neuromuscular Conditions
      (let [onc_data (get data :other_neuromuscular_conditions)
            onc_present (get onc_data :present)]
        (when (= onc_present "有")
          (conj! findings (str "其他神经肌肉系统情况:有" (let [symptoms (get onc_data :symptoms)] (if (seq symptoms) (str "(" (str/join "/" symptoms) ")") ""))))))

      (let [persistent_findings (persistent! findings)]
        (if (empty? persistent_findings)
          "精神及神经肌肉系统: 未见明显异常"
          (str "精神及神经肌肉系统: " (str/join ", " persistent_findings)))))))

(defn mental-neuromuscular-system-summary-view [props]
  (let [{:keys [endo-data]} props
        content (generate-mental-neuromuscular-summary endo-data)]
    [:div {:style {:padding "10px"}}
     content]))

(defn mental-neuromuscular-system-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id mn-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed useWatch calls and local option lists
        initial-form-values (let [base-data (when mn-data
                                              (-> mn-data
                                                  (assoc-in [:epilepsy_history :last_seizure_date] (utils/parse-date (get-in mn-data [:epilepsy_history :last_seizure_date])))
                                                  (assoc-in [:vertigo_history :last_episode_date] (utils/parse-date (get-in mn-data [:vertigo_history :last_episode_date])))
                                                  (assoc-in [:cerebral_infarction_history :last_episode_date] (utils/parse-date (get-in mn-data [:cerebral_infarction_history :last_episode_date])))
                                                  (assoc-in [:cerebral_hemorrhage_history :last_episode_date] (utils/parse-date (get-in mn-data [:cerebral_hemorrhage_history :last_episode_date])))))]
                              (-> (or base-data {})
                                  (update-in [:psycho_cognitive_history :present] #(or % "无"))
                                  (update-in [:epilepsy_history :present] #(or % "无"))
                                  (update-in [:vertigo_history :present] #(or % "无"))
                                  (update-in [:tia_history :present] #(or % "无"))
                                  (update-in [:cerebral_infarction_history :present] #(or % "无"))
                                  (update-in [:cerebral_hemorrhage_history :present] #(or % "无"))
                                  (update-in [:parkinsons_syndrome :present] #(or % "无"))
                                  (update-in [:cranial_carotid_stenosis :present] #(or % "无"))
                                  (update-in [:颅脑及颈动脉狭窄情况 :有无] #(or % "无"))
                                  (update-in [:其他神经肌肉系统相关情况 :有无] #(or % "无"))))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj
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
      ;; Thyroid Disease History
      (let [td_data (get data :thyroid_disease_history)
            td_present (get td_data :present)]
        (cond
          (= td_present "有") (conj! findings (str "甲状腺疾病:有" (let [types (get td_data :types)] (if (seq types) (str "(" (str/join "/" types) ")") ""))))
          (= td_present "无") (conj! findings "甲状腺疾病:无")
          :else (conj! findings "甲状腺疾病:未评估/不祥")))

      ;; Diabetes History
      (let [db_data (get data :diabetes_history)
            db_present (get db_data :present)]
        (cond
          (= db_present "有")
          (let [type (get db_data :type)
                control (get db_data :control_method)
                details_parts (cond-> [] type (conj type) control (conj control))]
            (conj! findings (str "糖尿病:有" (if (seq details_parts) (str "(" (str/join "/" details_parts) ")") ""))))
          (= db_present "无") (conj! findings "糖尿病:无")
          :else (conj! findings "糖尿病:未评估/不祥")))

      ;; Pheochromocytoma
      (let [ph_data (get data :pheochromocytoma)
            ph_present (get ph_data :present)]
        (cond
          (= ph_present "有") (conj! findings (str "嗜铬细胞瘤:有" (when-let [status (get ph_data :control_status)] (str "(" status ")"))))
          (= ph_present "无") (conj! findings "嗜铬细胞瘤:无")
          :else (conj! findings "嗜铬细胞瘤:未评估")))

      ;; Hypercortisolism
      (let [hc_data (get data :hypercortisolism)
            hc_present (get hc_data :present)]
        (cond
          (= hc_present "有") (conj! findings (str "皮质醇增多症:有" (when-let [details (get hc_data :details)] (str "(" details ")"))))
          (= hc_present "无") (conj! findings "皮质醇增多症:无")
          :else (conj! findings "皮质醇增多症:未评估/不祥")))

      ;; Gout
      (let [gout_present (get-in data [:gout :present])]
        (cond
          (= gout_present "有") (conj! findings (str "痛风:有" (if-not (str/blank? (get-in data [:gout :details])) "(详情)" "")))
          (= gout_present "无") (conj! findings "痛风:无")
          (= gout_present "不祥") (conj! findings "痛风:不祥")))

      ;; Hypopituitarism
      (let [hypo_present (get-in data [:hypopituitarism :present])]
        (cond
          (= hypo_present "有") (conj! findings (str "垂体功能减退:有" (if-not (str/blank? (get-in data [:hypopituitarism :details])) "(详情)" "")))
          (= hypo_present "无") (conj! findings "垂体功能减退:无")
          (= hypo_present "不祥") (conj! findings "垂体功能减退:不祥")))

      ;; Other Endocrine Conditions
      (when (not (str/blank? (get data :other_endocrine_conditions)))
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
        initial-form-values (-> (or endo-data {})
                                (update-in [:thyroid_disease_history :present] #(or % "无"))
                                (update-in [:thyroid_disease_history :airway_compression] #(or % "无"))
                                (update-in [:thyroid_disease_history :thyroid_heart_disease] #(or % "无"))
                                (update-in [:diabetes_history :present] #(or % "无"))
                                (update-in [:pheochromocytoma :present] #(or % "无"))
                                (update-in [:hypercortisolism :present] #(or % "无"))
                                (update-in [:皮质醇增多症病史 :有无] #(or % "无"))
                                (update-in [:痛风病史 :有无] #(or % "无"))
                                (update-in [:垂体功能减退症病史 :有无] #(or % "无"))) ; Corrected: Exactly one ')' to close the -> form.
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
      ;; Liver Function
      (let [lf_status (get-in data [:liver_function :status])]
        (cond
          (= lf_status "异常")
          (let [alt (get-in data [:liver_function :alt])
                albumin (get-in data [:liver_function :albumin])
                details (transient [])]
            (when alt (conj! details (str "ALT:" alt)))
            (when albumin (conj! details (str "Alb:" albumin)))
            (conj! findings (str "肝功能:异常" (let [pd (persistent! details)] (if (seq pd) (str "(" (str/join ", " pd) ")") "")))))
          (= lf_status "正常") (conj! findings "肝功能:正常")
          :else (conj! findings "肝功能:未评估")))

      ;; Liver Disease History
      (let [ldh_types (get-in data [:liver_disease_history :types])
            actual_types (if (seq ldh_types) (remove #{"none"} ldh_types) nil)]
        (if (seq actual_types)
          (conj! findings (str "肝脏疾病:有(" (str/join "/" actual_types) ")"))
          (conj! findings "肝脏疾病:无")))

      ;; Other Liver/Kidney Conditions
      (when (not (str/blank? (get data :other_liver_kidney_conditions)))
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
        initial-form-values (let [defaults {:肝功能 {:状态 "正常"}
                                            :既往肾功能不全史 "否"
                                            :目前是否规律透析 "否"}]
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
      ;; Acute Gastroenteritis History
      (let [agh_data (get data :acute_gastroenteritis_history)
            agh_has (get agh_data :has)]
        (cond
          (= agh_has "有") (conj! findings (str "急性胃肠炎:有" (let [s (get agh_data :symptoms)] (if (seq s) (str "(" (str/join "/" s) ")") ""))))
          (= agh_has "无") (conj! findings "急性胃肠炎:无")
          :else (conj! findings "急性胃肠炎:未评估/不祥")))

      ;; Esophageal, Gastric, Duodenal History
      (let [egdh_data (get data :esophageal_gastric_duodenal_history)
            egdh_has (get egdh_data :has)]
        (cond
          (= egdh_has "有") (conj! findings (str "食管胃十二指肠疾病:有" (let [s (get egdh_data :symptoms)] (if (seq s) (str "(" (str/join "/" s) ")") ""))))
          (= egdh_has "无") (conj! findings "食管胃十二指肠疾病:无")
          :else (conj! findings "食管胃十二指肠疾病:未评估")))

      ;; Chronic Digestive History
      (let [cdh_data (get data :chronic_digestive_history)
            cdh_has (get cdh_data :has)]
        (cond
          (= cdh_has "有") (conj! findings (str "慢性消化疾病:有" (let [s (get cdh_data :symptoms)] (if (seq s) (str "(" (str/join "/" s) ")") ""))))
          (= cdh_has "无") (conj! findings "慢性消化疾病:无")
          :else (conj! findings "慢性消化疾病:未评估/不祥")))

      ;; Other Digestive Conditions
      (when (not (str/blank? (get data :other_conditions)))
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
        initial-form-values (-> (or ds-data {})
                                (update-in [:急性胃肠炎病史 :有无] #(or % "无"))
                                (update-in [:食管胃十二指肠疾病病史 :有无] #(or % "无"))
                                (update-in [:慢性消化系统疾病病史 :有无] #(or % "无"))) ; Corrected: Single parenthesis
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
      ;; Anemia
      (let [anemia_data (get data :anemia)
            anemia_has (get anemia_data :has)]
        (cond
          (= anemia_has "有")
          (let [hb (get anemia_data :hb)]
            (conj! findings (str "贫血:有" (when hb (str "(Hb:" hb "g/L)")))))
          (= anemia_has "无") (conj! findings "贫血:无")
          :else (conj! findings "贫血:未评估")))

      ;; Coagulation Dysfunction
      (let [coag_data (get data :coagulation_dysfunction)
            coag_has (get coag_data :has)]
        (cond
          (= coag_has "有")
          (let [details (transient [])]
            (when (get coag_data :pt) (conj! details (str "PT:" (get coag_data :pt))))
            (when (get coag_data :aptt) (conj! details (str "APTT:" (get coag_data :aptt))))
            (when (get coag_data :inr) (conj! details (str "INR:" (get coag_data :inr))))
            (when (get coag_data :platelet_count) (conj! details (str "PLT:" (get coag_data :platelet_count))))
            (when (get coag_data :d_dimer) (conj! details (str "D-dimer:" (get coag_data :d_dimer))))
            (conj! findings (str "凝血功能障碍:有" (let [pd (persistent! details)] (if (seq pd) (str "(" (str/join ", " pd) ")") "")))))
          (= coag_has "无") (conj! findings "凝血功能障碍:无")
          :else (conj! findings "凝血功能障碍:未评估")))

      ;; Thrombosis History
      (let [thromb_data (get data :thrombosis_history)
            thromb_has (get thromb_data :has)]
        (cond
          (= thromb_has "有") (conj! findings (str "血栓史:有" (if-not (str/blank? (get thromb_data :details)) "(详情)" "")))
          (= thromb_has "无") (conj! findings "血栓史:无")
          :else (conj! findings "血栓史:未评估")))

      ;; Lower Limb DVT
      (let [dvt_data (get data :lower_limb_dvt)
            dvt_has (get dvt_data :has)]
        (cond
          (= dvt_has "有") (conj! findings (str "下肢DVT:有" (if-not (str/blank? (get dvt_data :details)) "(详情)" "")))
          (= dvt_has "无") (conj! findings "下肢DVT:无")
          :else (conj! findings "下肢DVT:未评估/不祥")))

      ;; Vascular Ultrasound Results
      (when (not (str/blank? (get data :vascular_ultrasound_results)))
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
        initial-form-values (or hs-data {}) ; Assuming hs-data is already structured per spec or nil
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
      ;; Immune Dysfunction
      (let [id_data (get data :immune_dysfunction)
            id_has (get id_data :has)]
        (cond
          (= id_has "有")
          (let [type (get id_data :type)
                details (get id_data :type_other_details)
                type_str (if (and type (= type "other_immune_dysfunction") (not (str/blank? details)))
                           (str "其他(" details ")")
                           type)]
            (conj! findings (str "免疫功能障碍:有" (when type_str (str "(" type_str ")")))))
          (= id_has "无") (conj! findings "免疫功能障碍:无")
          :else (conj! findings "免疫功能障碍:未评估/不祥")))

      ;; Autoimmune Disease
      (let [ad_data (get data :autoimmune_disease)
            ad_has (get ad_data :has)]
        (cond
          (= ad_has "有")
          (let [symptoms (get ad_data :symptoms)
                symptoms_details (get ad_data :symptoms_other_details)
                symptoms_display (when (seq symptoms)
                                   (mapv #(if (= % "other_autoimmune_symptom")
                                            (if (not (str/blank? symptoms_details))
                                              (str "其他(" symptoms_details ")")
                                              "其他")
                                            %)
                                         symptoms))]
            (conj! findings (str "自身免疫性疾病:有" (if (seq symptoms_display) (str "(" (str/join "/" symptoms_display) ")") ""))))
          (= ad_has "无") (conj! findings "自身免疫性疾病:无")
          :else (conj! findings "自身免疫性疾病:未评估/不祥")))

      ;; Other Immune Conditions
      (when (not (str/blank? (get data :other_immune_conditions)))
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
        initial-form-values (-> (or is-data {})
                                (update-in [:免疫功能障碍 :有无] #(or % "无"))
                                (update-in [:自身免疫性疾病 :有无] #(or % "无"))) ; Corrected: Single parenthesis
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
          med_fields [[:anticoagulant_antiplatelet "抗凝/抗血小板"]
                      [:glucocorticoids "糖皮质激素"]
                      [:cancer_treatment "肿瘤治疗"]
                      [:drug_abuse_dependence "药物滥用依赖"]
                      [:neuroleptic_drugs "神经安定类药物"]
                      [:glp1_agonists "GLP-1激动剂"]]]
      (doseq [[med-key med-name] med_fields]
        (let [med_present (get-in data [med-key :present])]
          (when (= med_present "有")
            (conj! findings (str med-name ":有" (if-not (str/blank? (get-in data [med-key :details])) "(详情)" ""))))))

      ;; Other Drug Use
      (let [other_drug_use_details (get data :other_drug_use)]
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
        initial-form-values (let [defaults {:抗凝或抗血小板药物 {:有无 "无"}
                                            :糖皮质激素 {:有无 "无"}
                                            :肿瘤治疗药物 {:有无 "无"}
                                            :毒麻及精神类药物滥用或依赖 {:有无 "无"}
                                            :神经安定类药物 {:有无 "无"}
                                            :GLP1受体激动剂 {:有无 "无"}}]
                              (merge defaults (or smh-data {})))
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
      ;; Marfan Syndrome
      (let [marfan_data (get data :marfan_syndrome)
            marfan_present (get marfan_data :present)]
        (cond
          (= marfan_present "有")
          (let [lesions (get marfan_data :related_lesions)
                cv_other_details (get marfan_data :cardiovascular_other_details)
                sk_other_details (get marfan_data :skeletal_other_details)
                lesion_names {"eye_lesion_lens_dislocation" "眼部病变(晶状体脱位)"
                              "cardiovascular_aortic_aneurysm" "心血管(主动脉瘤)"
                              "cardiovascular_aortic_dissection" "心血管(主动脉夹层)"
                              "cardiovascular_mitral_valve_disease" "心血管(二尖瓣病变)"
                              "cardiovascular_other" (if (not (str/blank? cv_other_details)) (str "心血管其他(" cv_other_details ")") "心血管其他")
                              "skeletal_scoliosis" "骨骼(脊柱侧弯)"
                              "skeletal_chest_deformity" "骨骼(胸廓畸形)"
                              "skeletal_other" (if (not (str/blank? sk_other_details)) (str "骨骼其他(" sk_other_details ")") "骨骼其他")}
                display_lesions (when (seq lesions) (mapv #(get lesion_names % %) lesions))]
            (conj! findings (str "马方综合征:有" (if (seq display_lesions) (str "(" (str/join "/" display_lesions) ")") ""))))
          (= marfan_present "不祥") (conj! findings "马方综合征:不祥")))

      ;; Other Special Diseases
      (let [other_details_str (get data :other_special_diseases)]
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
        initial-form-values (let [defaults {:马方综合征 {:有无 "无"}}]
                              (merge defaults (or sdh-data {})))
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
      ;; Nutritional Score
      (let [ns_data (get data :nutritional_score)]
        (if ns_data
          (let [bmi_risk (= "有" (get ns_data :bmi_lt_20_5))
                weight_loss_risk (= "有" (get ns_data :weight_loss_last_3_months))
                intake_risk (= "有" (get ns_data :reduced_intake_last_week))
                illness_risk (= "有" (get ns_data :severe_illness))
                num_ns_risks (+ (if bmi_risk 1 0) (if weight_loss_risk 1 0) (if intake_risk 1 0) (if illness_risk 1 0))]
            (if (>= num_ns_risks 2)
              (conj! findings (str "营养评分:有风险(" num_ns_risks "项阳性)"))
              (conj! findings "营养评分:无明显风险")))
          (conj! findings "营养评分:未评估")))

      ;; FRAIL Score
      (let [fs_data (get data :frail_score)]
        (if fs_data
          (let [fatigue (= "有" (get fs_data :fatigue))
                resistance (= "有" (get fs_data :resistance))
                ambulation (= "有" (get fs_data :ambulation))
                illness_gt_5 (= "有" (get fs_data :illness_gt_5))
                loss_of_weight (= "有" (get fs_data :loss_of_weight_gt_5_percent))
                num_fs_risks (+ (if fatigue 1 0) (if resistance 1 0) (if ambulation 1 0) (if illness_gt_5 1 0) (if loss_of_weight 1 0))]
            (cond
              (>= num_fs_risks 3) (conj! findings (str "FRAIL评估:衰弱(" num_fs_risks "分)"))
              (and (>= num_fs_risks 1) (<= num_fs_risks 2)) (conj! findings (str "FRAIL评估:衰弱前期(" num_fs_risks "分)"))
              :else (conj! findings "FRAIL评估:健康(0分)")))
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
        initial-form-values (-> (or na-data {})
                                (update-in [:nutritional_score :bmi_lt_20_5] #(or % "无"))
                                (update-in [:nutritional_score :weight_loss_last_3_months] #(or % "无"))
                                (update-in [:nutritional_score :reduced_intake_last_week] #(or % "无"))
                                (update-in [:nutritional_score :severe_illness] #(or % "无"))
                                (update-in [:frail_score :fatigue] #(or % "无"))
                                (update-in [:frail_score :resistance] #(or % "无"))
                                (update-in [:frail_score :ambulation] #(or % "无"))
                                (update-in [:营养评分 :BMI小于20点5] #(or % "无"))
                                (update-in [:营养评分 :近3个月体重下降] #(or % "无"))
                                (update-in [:营养评分 :近1周摄食减少] #(or % "无"))
                                (update-in [:营养评分 :患有严重疾病] #(or % "无"))
                                (update-in [:FRAIL针对大于60岁病人 :疲乏] #(or % "无"))
                                (update-in [:FRAIL针对大于60岁病人 :上楼梯困难] #(or % "无"))
                                (update-in [:FRAIL针对大于60岁病人 :独立行走100m困难] #(or % "无"))
                                (update-in [:FRAIL针对大于60岁病人 :患有5种以上慢性病] #(or % "无"))
                                (update-in [:FRAIL针对大于60岁病人 :体重下降大于5百分比] #(or % "无"))) ; Corrected: Single parenthesis
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
    (let [is_pregnant (get data :is_pregnant)]
      (cond
        (= is_pregnant "有")
        (let [summary_parts (transient ["妊娠:是"])
              gest_week (get data :gestational_week)
              obstetric_history (get data :obstetric_history)
              comorbid_conditions (get data :comorbid_obstetric_conditions)
              other_comorbid_details (get data :comorbid_obstetric_conditions_other_details)
              other_preg_conditions (get data :other_pregnancy_conditions)]

          (if gest_week
            (conj! summary_parts (str "孕周:" gest_week))
            (conj! summary_parts "孕周:未提供"))

          (when (not (str/blank? obstetric_history))
            (conj! summary_parts (str "孕产史:" obstetric_history)))

          (if (seq comorbid_conditions)
            (let [display_comorbid (mapv #(if (= % "other_obstetric_conditions")
                                            (if (not (str/blank? other_comorbid_details))
                                              (str "其他(" other_comorbid_details ")")
                                              "其他产科情况")
                                            %)
                                         comorbid_conditions)]
              (conj! summary_parts (str "合并产科情况:" (str/join "/" display_comorbid))))
            (conj! summary_parts "合并产科情况:无"))

          (when (not (str/blank? other_preg_conditions))
            (conj! summary_parts (str "其他妊娠相关:" other_preg_conditions)))
          (str/join ", " (persistent! summary_parts)))

        (= is_pregnant "无") "妊娠:否"
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
        initial-form-values (let [base-data (or pa-data {})
                                  default-values {:是否妊娠 "无"}] ; Default from original logic
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
    (let [history-present (get-in data [:手术麻醉史 :有无]) ; Updated path
          family-hyperthermia (get-in data [:有血缘关系的人发生过恶性高热史 :有无]) ; Updated path
          parts [(str "手术麻醉史:" (cond (= history-present "有") "有" (= history-present "不祥") "不祥" :else "无"))
                 (str "恶性高热家族史:" (if (= family-hyperthermia "有") "有" "无"))]]
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
        initial-form-values (let [base-data (when sah-data
                                              (-> sah-data
                                                  (update-in [:手术麻醉史 :详情 :具体上次麻醉日期] #(when % (utils/parse-date %)))))
                                  default-values {:手术麻醉史 {:有无 "无"}
                                                  :有血缘关系的人发生过恶性高热史 {:有无 "无"}}]
                              (merge default-values (or base-data {})))
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (-> values-clj
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
  (if (or (nil? data) (empty? data) (nil? (:detailed_assessment data)))
    "无数据"
    (let [assessment (:detailed_assessment data)
          diff-intubation (get assessment :difficult_intubation_history)
          mallampati (get assessment :mallampati_classification)
          mouth-opening (get-in assessment [:mouth_opening :degree])
          thyromental-dist (get-in assessment [:thyromental_distance_class])
          parts (cond-> []
                  (and diff-intubation (not= diff-intubation "无") (not (nil? diff-intubation)))
                  (conj (str "困难插管史:" diff-intubation))
                  mallampati
                  (conj (str "Mallampati:" mallampati))
                  (and mouth-opening (not= mouth-opening "gte_3_fingers"))
                  (conj (str "张口度:" mouth-opening))
                  (and thyromental-dist (not= thyromental-dist "gt_6_5_cm"))
                  (conj (str "甲颏距离:" thyromental-dist)))]
      (if (empty? parts)
        "未见明显异常"
        (str/join ", " parts)))))

(defn airway-assessment-summary-view [props]
  (let [{:keys [aa-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-airway-summary aa-data)]))

(defn airway-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id aa-data on-show-summary]} props
        [form] (Form.useForm)
        ;; Removed local option lists and useWatch calls from original implementation
        initial-form-values (let [base-data (or aa-data {})
                                  defaults {:既往困难通气史 :不详
                                            :既往困难插管史 :不详
                                            :张口度 {:分级 :大于等于3横指}
                                            :甲颏距离cm nil
                                            :头颈活动度 {:分级 :正常活动}
                                            :改良Mallampati分级 :Ⅰ级
                                            :上唇咬合试验ULBT :1级
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
  (if (or (nil? data) (empty? data))
    "无数据"
    (let [critical-fields {[:central_nervous_system :brain_tumor] "脑肿瘤"
                           [:peripheral_nervous_system :spinal_cord_injury] "脊髓损伤"
                           [:lumbar_disc_herniation :present] "腰椎间盘突出"
                           [:cardiovascular_system :aortic_stenosis] "主动脉瓣狭窄"
                           [:puncture_site_inspection :local_infection] "穿刺点感染"
                           [:local_anesthetic_allergy] "局麻药过敏"}
          risk-factors (reduce (fn [acc [path label]]
                                 (if (= (get-in data path) "有")
                                   (conj acc label)
                                   acc))
                               []
                               critical-fields)]
      (if (seq risk-factors)
        (str/join ", " risk-factors)
        "无明确风险因素"))))

(defn spinal-anesthesia-assessment-summary-view [props]
  (let [{:keys [saa-data]} props]
    [:div {:style {:padding "10px"}}
     (generate-spinal-anesthesia-summary saa-data)]))

(defn spinal-anesthesia-assessment-detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id saa-data on-show-summary]} props
        [form] (Form.useForm)
        initial-form-values (let [base-data (or saa-data {})
                                  ;; Map original defaults to new spec paths.
                                  ;; Original: [:central_nervous_system :brain_tumor] "无"
                                  ;; New: [:椎管内麻醉相关评估 :中枢神经系统 :脑肿瘤] :无 (assuming enum uses keywords)
                                  defaults {:中枢神经系统 {:脑肿瘤 :无, :脑出血 :无, :严重颅脑外伤 :无, :癫痫 :无}
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
