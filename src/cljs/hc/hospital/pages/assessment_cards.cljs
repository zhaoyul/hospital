(ns hc.hospital.pages.assessment-cards
  (:require
   ["antd" :refer [Checkbox Col DatePicker Empty Form Input InputNumber Radio
                   Row Select]]
   ["dayjs" :as dayjs]
   ["@ant-design/icons"  :refer [AppleOutlined CloudOutlined
                                 CoffeeOutlined ExperimentOutlined
                                 GatewayOutlined HeartOutlined
                                 HistoryOutlined MedicineBoxOutlined
                                 NodeIndexOutlined ProjectOutlined
                                 SecurityScanOutlined UserOutlined
                                 WarningOutlined WomanOutlined]]
   [hc.hospital.components.assessment-form-components :as afc]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [hc.hospital.ui-helpers :refer [custom-styled-card]]
   [hc.hospital.utils :as utils]
   [reagent.core :as r]
   [re-frame.core :as rf]))


(defn circulatory-system-card "循环系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        circulatory-data @(rf/subscribe [::subs/circulatory-system-data])
        [form] (Form.useForm)
        ;; Removed Form.useWatch for radio groups that will be handled by form-item-radio-conditional
        ;; Watchers for nested conditions or conditions not directly tied to a radio group handled by the new component remain.
        arrhythmia-has (Form.useWatch [:cardiac_disease_history :arrhythmia :has] form) ; Still needed for complex children logic
        pacemaker-type (Form.useWatch [:pacemaker_history :type] form) ; Example of a nested condition

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        treatment-status-options [{:label "治愈" :value "治愈"} {:label "好转" :value "好转"}
                                  {:label "仍有症状" :value "仍有症状"} {:label "未治疗" :value "未治疗"}]
        cardiac-function-options [{:value "Ⅰ 级" :label "Ⅰ 级：心功能正常，体力活动不受限制。一般体力活动不引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅱ 级" :label "Ⅱ 级：心功能较差，体力活动轻度受限制。休息时无症状，一般体力活动引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅲ 级" :label "Ⅲ 级：心功能不全，体力活动明显受限制。休息时无症状，但小于一般体力活动即可引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅳ 级" :label "Ⅳ 级：心功能衰竭，不能从事任何体力劳动。休息状态下也出现心衰症状，体力活动后加重"}]
        exercise-capacity-options [{:value "运动能力正常" :label "运动能力正常。可耐受慢跑、跳绳等较高强度的身体训练 >6MET"}
                                   {:value "运动能力轻度下降" :label "运动能力轻度下降。可胜任日常家务工作或骑自行车 3-6MET"}
                                   {:value "运动能力明显下降" :label "运动能力明显下降。仅能从事文书工作或缓慢步行 <3MET"}]
        initial-form-values (when circulatory-data
                              (-> circulatory-data
                                  (assoc-in [:cardiac_disease_history :congestive_heart_failure :last_episode_date]
                                            (utils/parse-date (get-in circulatory-data [:cardiac_disease_history :congestive_heart_failure :last_episode_date])))))
        on-finish-fn (fn [values]
                       (let [transformed-values (-> values
                                                    (assoc-in [:cardiac_disease_history :congestive_heart_failure :last_episode_date]
                                                              (when-let [d (get-in values [:cardiac_disease_history :congestive_heart_failure :last_episode_date])]
                                                                (utils/date->iso-string d))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :circulatory_system (js->clj transformed-values :keywordize-keys true)])))
        form-items [:<>
                    ;; 心电图 (ECG)
                    [:> Form.Item {:label "心电图" :name [:ecg_description]}
                     [:> Input.TextArea {:placeholder "请描述ECG结果"
                                         :rows 3}]]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "心脏疾病病史"
                      :radio-name [:cardiac_disease_history :has]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children
                      [:<>
                       ;; Coronary Artery Disease (冠心病)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "冠心病"]
                         :radio-name [:cardiac_disease_history :coronary_artery_disease :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:label "症状" :name [:cardiac_disease_history :coronary_artery_disease :symptoms]}
                                     [:> Select {:placeholder "选择症状" :style {:width "100%"} :allowClear true
                                                 :options [{:value "无症状" :label "无症状"}
                                                           {:value "稳定性心绞痛" :label "稳定性心绞痛"}
                                                           {:value "不稳定性心绞痛" :label "不稳定性心绞痛"}
                                                           {:value "心梗" :label "心梗"}]}]]
                                    [:> Form.Item {:label "心脏支架" :name [:cardiac_disease_history :coronary_artery_disease :stent]}
                                     [:> Radio.Group {:options yes-no-options}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :coronary_artery_disease :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :coronary_artery_disease :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Arrhythmia (心律失常)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心律失常"]
                         :radio-name [:cardiac_disease_history :arrhythmia :has]
                         :radio-options yes-no-unknown-options
                         :conditional-value "有" ; Shows children if "有"
                         :extra-condition-values ["不祥"] ; Also show children if "不祥"
                         :value-for-children-wrapper "有" ; Only wrap with div if "有" (to allow specific styling for "有")
                         :children [:<>
                                    (when (= arrhythmia-has "有") ; Specific input for "有"
                                      [:> Form.Item {:name [:cardiac_disease_history :arrhythmia :has_details]}
                                       [:> Input {:placeholder "心律失常类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}}]])
                                    ;; Fields for "有" or "不祥"
                                    [:> Form.Item {:label "类型" :name [:cardiac_disease_history :arrhythmia :type]}
                                     [:> Select {:placeholder "选择类型" :style {:width "100%"} :allowClear true
                                                 :options [{:value "低危型" :label "低危型"}
                                                           {:value "中危型" :label "中危型"}
                                                           {:value "高危型" :label "高危型"}]}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :arrhythmia :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :arrhythmia :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Cardiomyopathy (心肌病)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心肌病"]
                         :radio-name [:cardiac_disease_history :cardiomyopathy :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:name [:cardiac_disease_history :cardiomyopathy :has_details]}
                                     [:> Input {:placeholder "心肌病类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :cardiomyopathy :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :cardiomyopathy :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Valvular Heart Disease (心脏瓣膜病变)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心脏瓣膜病变"]
                         :radio-name [:cardiac_disease_history :valvular_heart_disease :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:name [:cardiac_disease_history :valvular_heart_disease :has_details]}
                                     [:> Input {:placeholder "心脏瓣膜病变类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :valvular_heart_disease :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :valvular_heart_disease :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Congenital Heart Disease (先天性心脏病)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "先天性心脏病"]
                         :radio-name [:cardiac_disease_history :congenital_heart_disease :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:name [:cardiac_disease_history :congenital_heart_disease :has_details]}
                                     [:> Input {:placeholder "先天性心脏病类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :congenital_heart_disease :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :congenital_heart_disease :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Congestive Heart Failure (充血性心力衰竭病史)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "充血性心力衰竭病史"]
                         :radio-name [:cardiac_disease_history :congestive_heart_failure :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:label "上次发作日期" :name [:cardiac_disease_history :congestive_heart_failure :last_episode_date]}
                                     [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :congestive_heart_failure :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :congestive_heart_failure :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ;; Pulmonary Hypertension (肺动脉高压)
                       [:f> afc/form-item-radio-conditional
                        {:form-instance form
                         :label [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "肺动脉高压"]
                         :radio-name [:cardiac_disease_history :pulmonary_hypertension :has]
                         :radio-options yes-no-options
                         :conditional-value "有"
                         :children [:<>
                                    [:> Form.Item {:name [:cardiac_disease_history :pulmonary_hypertension :has_details]}
                                     [:> Input {:placeholder "肺动脉高压类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}}]]
                                    [:> Form.Item {:label "治疗情况" :name [:cardiac_disease_history :pulmonary_hypertension :treatment_status]}
                                     [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                                 :options treatment-status-options}]]
                                    [:> Form.Item {:label "治疗用药" :name [:cardiac_disease_history :pulmonary_hypertension :medication]}
                                     [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2}]]]}]
                       ]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "心脏起搏器植入史"
                      :radio-name [:pacemaker_history :has]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "起搏器类型" :name [:pacemaker_history :type]}
                                  [:> Radio.Group {}
                                   [:> Radio {:value "临时起搏器"} "临时起搏器"]
                                   [:> Radio {:value "永久起搏器"} "永久起搏器"]]]
                                 (when (= pacemaker-type "永久起搏器") ; This nested condition remains
                                   [:> Form.Item {:label "工作状态" :name [:pacemaker_history :working_status]}
                                    [:> Input {:placeholder "描述永久起搏器工作状态"}]])]}]

                    ;; Cardiac Ultrasound Findings Section
                    [:> Form.Item {:label "心脏彩超检查" :name [:cardiac_ultrasound_findings :details]}
                     [:> Input.TextArea {:placeholder "请描述心脏彩超检查内容"
                                         :rows 4}]]

                    ;; Coronary CTA/Angiography Results Section
                    [:> Form.Item {:label "冠脉CTA/冠脉造影结果" :name [:coronary_cta_angiography_results :details]}
                     [:> Input.TextArea {:placeholder "请描述冠脉CTA/冠脉造影结果内容"
                                         :rows 4}]]

                    ;; Cardiac Function Assessment Section
                    [:> Form.Item {:label "心脏功能评估 (NYHA分级)" :name [:cardiac_function_assessment :class]}
                     [:> Radio.Group {}
                      (for [opt cardiac-function-options]
                        ^{:key (:value opt)} [:> Radio {:value (:value opt) :style {:display "block" :height "auto" :lineHeight "22px" :whiteSpace "normal" :marginBottom "8px"}}
                                              (:label opt)])]]

                    ;; Exercise Capacity Assessment Section
                    [:> Form.Item {:label "运动能力评估" :name [:exercise_capacity_assessment :level]}
                     [:> Radio.Group {}
                      (for [opt exercise-capacity-options]
                        ^{:key (:value opt)} [:> Radio {:value (:value opt) :style {:display "block" :height "auto" :lineHeight "22px" :whiteSpace "normal" :marginBottom "8px"}}
                                              (:label opt)])]]

                    ;; Other Conditions Section
                    [:> Form.Item {:label "其他循环系统相关情况" :name [:other_cardiac_info :details]}
                     [:> Input.TextArea {:placeholder "请描述其他循环系统相关情况"
                                         :rows 4}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> HeartOutlined {:style {:marginRight "8px"}}]
      :title "循环系统"
      :header-color "#e6f7ff"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-circulatory-detailed")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil ; No onValuesChange for this card yet
      :children form-items}]))


(defn respiratory-system-card "呼吸系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        respiratory-data @(rf/subscribe [::subs/respiratory-system-data])
        [form] (Form.useForm)
        ;; Removed Form.useWatch calls that are now handled by form-item-radio-conditional

        cold-symptom-options [{:label "咳嗽" :value "cough"}
                              {:label "流涕" :value "runny_nose"}
                              {:label "发热" :value "fever"}
                              {:label "咽痛" :value "sore_throat"}
                              {:label "其他" :value "other"}]
        treatment-options [{:value "未治疗" :label "未治疗"}
                           {:value "药物治疗" :label "药物治疗"}
                           {:value "已痊愈" :label "已痊愈"}
                           {:value "其他" :label "其他"}]
        initial-form-values (when respiratory-data
                              (-> respiratory-data
                                  (assoc-in [:cold_history_last_2_weeks :onset_date]
                                            (utils/parse-date (get-in respiratory-data [:cold_history_last_2_weeks :onset_date])))
                                  (assoc-in [:bronchitis_pneumonia_last_month :onset_date]
                                            (utils/parse-date (get-in respiratory-data [:bronchitis_pneumonia_last_month :onset_date])))
                                  (assoc-in [:asthma_history :last_episode_date]
                                            (utils/parse-date (get-in respiratory-data [:asthma_history :last_episode_date])))))
        on-finish-fn (fn [values]
                       (let [transformed-values (-> values
                                                    (assoc-in [:cold_history_last_2_weeks :onset_date]
                                                              (when-let [d (get-in values [:cold_history_last_2_weeks :onset_date])]
                                                                (utils/date->iso-string d)))
                                                    (assoc-in [:bronchitis_pneumonia_last_month :onset_date]
                                                              (when-let [d (get-in values [:bronchitis_pneumonia_last_month :onset_date])]
                                                                (utils/date->iso-string d)))
                                                    (assoc-in [:asthma_history :last_episode_date]
                                                              (when-let [d (get-in values [:asthma_history :last_episode_date])]
                                                                (utils/date->iso-string d))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :respiratory_system (js->clj transformed-values :keywordize-keys true)])))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "近两周内感冒病史"
                      :radio-name [:cold_history_last_2_weeks :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "发病日期" :name [:cold_history_last_2_weeks :onset_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "症状" :name [:cold_history_last_2_weeks :symptoms]}
                                  [:> Checkbox.Group {:options cold-symptom-options}]]
                                 [:> Form.Item {:label "治疗情况" :name [:cold_history_last_2_weeks :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "近一个月内支气管炎 / 肺炎病史"
                      :radio-name [:bronchitis_pneumonia_last_month :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "发病日期" :name [:bronchitis_pneumonia_last_month :onset_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:bronchitis_pneumonia_last_month :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "哮喘病史"
                      :radio-name [:asthma_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "上次发作日期" :name [:asthma_history :last_episode_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:asthma_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-options}]]
                                 [:> Form.Item {:label "用药情况" :name [:asthma_history :medication_status]}
                                  [:> Select {:placeholder "选择用药情况" :style {:width "100%"} :allowClear true
                                              :options [{:value "规律吸入激素" :label "规律吸入激素"}
                                                        {:value "按需使用支扩剂" :label "按需使用支扩剂"}
                                                        {:value "其他" :label "其他"} {:value "未用药" :label "未用药"}]}]]
                                 [:> Form.Item {:label "用药详情" :name [:asthma_history :medication_details]}
                                  [:> Input.TextArea {:placeholder "请描述具体用药情况" :rows 2}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "慢性阻塞性肺疾病 (COPD)"
                      :radio-name [:copd_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:> Form.Item {:label "治疗情况" :name [:copd_history :treatment_status]}
                                 [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                             :options treatment-options}]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "支气管扩张症"
                      :radio-name [:bronchiectasis_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:> Form.Item {:label "治疗情况" :name [:bronchiectasis_history :treatment_status]}
                                 [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                             :options treatment-options}]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "肺部结节"
                      :radio-name [:pulmonary_nodules_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:> Form.Item {:label "治疗情况" :name [:pulmonary_nodules_history :treatment_status]}
                                 [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                             :options treatment-options}]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "肺部肿瘤"
                      :radio-name [:lung_tumor_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:> Form.Item {:label "治疗情况" :name [:lung_tumor_history :treatment_status]}
                                 [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                             :options treatment-options}]]}]

                    [:> Form.Item {:label "胸片" :name [:chest_xray_results]}
                     [:> Input.TextArea {:placeholder "请描述胸片结果" :rows 2}]]

                    [:> Form.Item {:label "胸部 CT" :name [:chest_ct_results]}
                     [:> Input.TextArea {:placeholder "请描述胸部 CT 结果" :rows 2}]]

                    [:> Form.Item {:label "肺功能" :name [:pulmonary_function_test_results]}
                     [:> Input.TextArea {:placeholder "请描述肺功能检查结果" :rows 2}]]

                    [:> Form.Item {:label "血气分析" :name [:blood_gas_analysis_results]}
                     [:> Input.TextArea {:placeholder "请描述血气分析结果" :rows 2}]]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "是否有肺结核"
                      :radio-name [:tuberculosis_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "治疗情况" :name [:tuberculosis_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-options}]]
                                 [:> Form.Item {:label "传染性" :name [:tuberculosis_history :infectious]}
                                  [:> Radio.Group {}
                                   [:> Radio {:value "无"} "无"]
                                   [:> Radio {:value "有"} "有"]
                                   [:> Radio {:value "不详"} "不祥"]]]]}]
                    [:> Form.Item {:label "其他呼吸系统相关情况" :name [:other_respiratory_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他呼吸系统相关情况请在此注明" :rows 3}]]]]

    [afc/patient-assessment-card-wrapper
     {:icon [:> CloudOutlined {:style {:marginRight "8px"}}]
      :title "呼吸系统"
      :header-color "#e6fffb"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-respiratory")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        mn-data @(rf/subscribe [::subs/mental-neuromuscular-system-data])
        [form] (Form.useForm)
        ;; Watchers that are still needed for complex conditional logic within children
        psycho-cognitive-symptoms (Form.useWatch [:psycho_cognitive_history :symptoms] form)
        other-neuromuscular-symptoms (Form.useWatch [:other_neuromuscular_conditions :symptoms] form)
        cranial-stenosis-present (Form.useWatch [:cranial_carotid_stenosis :present] form) ; For inline conditional input

        psycho-cog-symptom-options [{:label "智力发育迟缓" :value "intellectual_disability"}
                                    {:label "焦虑症" :value "anxiety_disorder"}
                                    {:label "抑郁症" :value "depression"}
                                    {:label "精神分裂" :value "schizophrenia"}
                                    {:label "睡眠障碍" :value "sleep_disorder"}
                                    {:label "孤独症" :value "autism"}
                                    {:label "病情稳定" :value "stable_condition"}
                                    {:label "其他" :value "other_symptoms"}]
        general-treatment-status-options [{:value "治愈" :label "治愈"}
                                          {:value "好转" :label "好转"}
                                          {:value "仍有症状" :label "仍有症状"}
                                          {:value "未治疗" :label "未治疗"}
                                          {:value "病情稳定" :label "病情稳定"}
                                          {:value "其他" :label "其他"}]
        initial-form-values (when mn-data
                              (-> mn-data
                                  (assoc-in [:epilepsy_history :last_seizure_date] (utils/parse-date (get-in mn-data [:epilepsy_history :last_seizure_date])))
                                  (assoc-in [:vertigo_history :last_episode_date] (utils/parse-date (get-in mn-data [:vertigo_history :last_episode_date])))
                                  (assoc-in [:cerebral_infarction_history :last_episode_date] (utils/parse-date (get-in mn-data [:cerebral_infarction_history :last_episode_date])))
                                  (assoc-in [:cerebral_hemorrhage_history :last_episode_date] (utils/parse-date (get-in mn-data [:cerebral_hemorrhage_history :last_episode_date])))))
        on-finish-fn (fn [values]
                       (let [transformed-values (-> values
                                                    (assoc-in [:epilepsy_history :last_seizure_date] (when-let [d (get-in values [:epilepsy_history :last_seizure_date])] (utils/date->iso-string d)))
                                                    (assoc-in [:vertigo_history :last_episode_date] (when-let [d (get-in values [:vertigo_history :last_episode_date])] (utils/date->iso-string d)))
                                                    (assoc-in [:cerebral_infarction_history :last_episode_date] (when-let [d (get-in values [:cerebral_infarction_history :last_episode_date])] (utils/date->iso-string d)))
                                                    (assoc-in [:cerebral_hemorrhage_history :last_episode_date] (when-let [d (get-in values [:cerebral_hemorrhage_history :last_episode_date])] (utils/date->iso-string d))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :mental_neuromuscular_system (js->clj transformed-values :keywordize-keys true)])))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "精神认知相关疾病史"
                      :radio-name [:psycho_cognitive_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "症状" :name [:psycho_cognitive_history :symptoms]}
                                  [:> Checkbox.Group {:options psycho-cog-symptom-options}]]
                                 (when (some #{"other_symptoms"} psycho-cognitive-symptoms)
                                   [:> Form.Item {:label "其他症状详情" :name [:psycho_cognitive_history :symptoms_other_details]}
                                    [:> Input {:placeholder "请描述其他症状"}]])
                                 [:> Form.Item {:label "治疗情况" :name [:psycho_cognitive_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]
                                 [:> Form.Item {:label "治疗用药" :name [:psycho_cognitive_history :medication]}
                                  [:> Input.TextArea {:placeholder "请描述治疗用药" :rows 2}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "癫痫病史"
                      :radio-name [:epilepsy_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "近期发作日期" :name [:epilepsy_history :last_seizure_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:epilepsy_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]
                                 [:> Form.Item {:label "治疗用药" :name [:epilepsy_history :medication]}
                                  [:> Input.TextArea {:placeholder "请描述治疗用药" :rows 2}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "眩晕病史"
                      :radio-name [:vertigo_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "近期发作日期" :name [:vertigo_history :last_episode_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:vertigo_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "短暂性脑缺血发作病史 (TIA)"
                      :radio-name [:tia_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "近期发作情况" :name [:tia_history :recent_onset_status]}
                                  [:> Select {:placeholder "选择近期发作情况" :style {:width "100%"} :allowClear true
                                              :options [{:value "近 3 月内无发作" :label "近 3 月内无发作"}
                                                        {:value "近 3 月内有发作" :label "近 3 月内有发作"}]}]]
                                 [:> Form.Item {:label "治疗情况" :name [:tia_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "脑梗病史"
                      :radio-name [:cerebral_infarction_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "近期发作日期" :name [:cerebral_infarction_history :last_episode_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:cerebral_infarction_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]
                                 [:> Form.Item {:label "目前用药" :name [:cerebral_infarction_history :medication]}
                                  [:> Input.TextArea {:placeholder "请描述目前用药" :rows 2}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "脑出血病史"
                      :radio-name [:cerebral_hemorrhage_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "近期发作日期" :name [:cerebral_hemorrhage_history :last_episode_date]}
                                  [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"}]]
                                 [:> Form.Item {:label "治疗情况" :name [:cerebral_hemorrhage_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "帕金森综合症"
                      :radio-name [:parkinsons_syndrome :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "诊断年限 (年)" :name [:parkinsons_syndrome :diagnosis_duration_years]}
                                  [:> InputNumber {:placeholder "请输入年数" :style {:width "100%"} :min 0}]]
                                 [:> Form.Item {:label "治疗情况" :name [:parkinsons_syndrome :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]
                                 [:> Form.Item {:label "用药情况" :name [:parkinsons_syndrome :medication]}
                                  [:> Input.TextArea {:placeholder "请描述用药情况" :rows 2}]]]}]
                    ;; Cranial Carotid Stenosis - an Input appears if "有", not a div block.
                    ;; This pattern is simpler and might not need the form-item-radio-conditional component unless we want to standardize all.
                    ;; For now, leaving as is, as per instructions to prioritize div-revealing patterns.
                    [:> Form.Item {:label "颅脑和颈动脉狭窄" :name [:cranial_carotid_stenosis :present]}
                     [:> Radio.Group {}
                      [:> Radio {:value "无"} "无"]
                      [:> Radio {:value "有"} "有"]
                      [:> Radio {:value "不祥"} "不祥"]]
                     (when (= cranial-stenosis-present "有") ; Still use useWatch for this inline conditional input
                       [:> Form.Item {:name [:cranial_carotid_stenosis :details]}
                        [:> Input {:placeholder "请描述狭窄详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}}]])]


                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "其他神经肌肉系统情况"
                      :radio-name [:other_neuromuscular_conditions :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children (let [other-symptom-options [{:label "重症肌无力" :value "myasthenia_gravis"}
                                                             {:label "格林巴利综合征" :value "guillain_barre"}
                                                             {:label "帕金森病史" :value "parkinsons_disease"}
                                                             {:label "脊髓灰质炎后综合征" :value "post_polio_syndrome"}
                                                             {:label "多发性硬化症" :value "multiple_sclerosis"}
                                                             {:label "肌营养不良" :value "muscular_dystrophy"}
                                                             {:label "其他" :value "other_specific_conditions"}]]
                                  [:<>
                                   [:> Form.Item {:label "症状" :name [:other_neuromuscular_conditions :symptoms]}
                                    [:> Checkbox.Group {:options other-symptom-options}]]
                                   (when (some #{"other_specific_conditions"} other-neuromuscular-symptoms)
                                     [:> Form.Item {:label "其他症状详情" :name [:other_neuromuscular_conditions :symptoms_other_details]}
                                      [:> Input {:placeholder "请描述其他具体情况"}]])])}]]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> UserOutlined {:style {:marginRight "8px"}}]
      :title "精神及神经肌肉系统"
      :header-color "#fffbe6"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-mental-neuromuscular")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil ; No onValuesChange for this card
      :children form-items}]))


(defn endocrine-system-card "内分泌系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        endo-data @(rf/subscribe [::subs/endocrine-system-data])
        [form] (Form.useForm)
        ;; Watchers for nested/internal conditional logic
        thyroid-types (Form.useWatch [:thyroid_disease_history :types] form)
        diabetes-control-method (Form.useWatch [:diabetes_history :control_method] form)
        pheo-control-status (Form.useWatch [:pheochromocytoma :control_status] form)
        gout-present (Form.useWatch [:gout :present] form) ; For inline conditional input
        hypopituitarism-present (Form.useWatch [:hypopituitarism :present] form) ; For inline conditional input

        thyroid-type-options [{:label "甲亢" :value "hyperthyroidism"}
                              {:label "甲减" :value "hypothyroidism"}
                              {:label "甲状腺术后甲状腺素替代治疗" :value "post_surgery_replacement_therapy"}
                              {:label "桥本" :value "hashimotos"}
                              {:label "其他" :value "other_thyroid_type"}]
        general-treatment-status-options [{:value "治愈" :label "治愈"} {:value "好转" :label "好转"} {:value "仍有症状" :label "仍有症状"} {:value "未治疗" :label "未治疗"} {:value "病情稳定" :label "病情稳定"} {:value "其他" :label "其他"}]
        initial-form-values endo-data ; No date parsing needed for this card
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :endocrine_system (js->clj values :keywordize-keys true)]))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "甲状腺疾病病史"
                      :radio-name [:thyroid_disease_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "类型" :name [:thyroid_disease_history :types]}
                                  [:> Checkbox.Group {:options thyroid-type-options}]]
                                 (when (some #{"other_thyroid_type"} thyroid-types)
                                   [:> Form.Item {:label "其他类型详情" :name [:thyroid_disease_history :type_other_details]}
                                    [:> Input {:placeholder "请描述其他甲状腺疾病类型"}]])
                                 [:> Form.Item {:label "甲状腺功能检查" :name [:thyroid_disease_history :function_test_results]}
                                  [:> Input.TextArea {:placeholder "请描述甲状腺功能检查结果" :rows 2}]]
                                 [:> Form.Item {:label "治疗情况" :name [:thyroid_disease_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options general-treatment-status-options}]]
                                 [:> Form.Item {:label "甲状腺是否肿大压迫气管，是否存在困难气道？" :name [:thyroid_disease_history :airway_compression]}
                                  [:> Radio.Group {}
                                   [:> Radio {:value "无"} "无"]
                                   [:> Radio {:value "有"} "有"]]]
                                 [:> Form.Item {:label "是否合并甲状腺心脏病" :name [:thyroid_disease_history :thyroid_heart_disease]}
                                  [:> Radio.Group {}
                                   [:> Radio {:value "无"} "无"]
                                   [:> Radio {:value "有"} "有"]]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "糖尿病病史"
                      :radio-name [:diabetes_history :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children (let [diabetes-type-options [{:value "1型糖尿病" :label "1 型糖尿病"} {:value "2型糖尿病" :label "2 型糖尿病"}]
                                      diabetes-control-options [{:value "饮食控制" :label "饮食控制"}
                                                                {:value "药物控制" :label "药物控制"}
                                                                {:value "胰岛素控制" :label "胰岛素控制"}
                                                                {:value "未控制" :label "未控制"}]]
                                  [:<>
                                   [:> Form.Item {:label "类型" :name [:diabetes_history :type]}
                                    [:> Select {:placeholder "选择类型" :style {:width "100%"} :allowClear true
                                                :options diabetes-type-options}]]
                                   [:> Form.Item {:label "控制方式" :name [:diabetes_history :control_method]}
                                    [:> Select {:placeholder "选择控制方式" :style {:width "100%"} :allowClear true
                                                :options diabetes-control-options}]]
                                   (when (= diabetes-control-method "药物控制")
                                     [:> Form.Item {:label "药物详情" :name [:diabetes_history :medication_details]}
                                      [:> Input {:placeholder "请输入药物控制详情"}]])
                                   [:> Form.Item {:label "血糖（Glu）" :name [:diabetes_history :blood_glucose_level]}
                                    [:> InputNumber {:placeholder "mmol/L" :style {:width "100%"}
                                                     :addonAfter "mmol/L"}]]
                                   [:> Form.Item {:label "糖化血红蛋白（HbA1c）" :name [:diabetes_history :hba1c_level]}
                                    [:> InputNumber {:placeholder "%" :style {:width "100%"}
                                                     :addonAfter "%"}]]])}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "嗜铬细胞瘤"
                      :radio-name [:pheochromocytoma :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"}]
                      :conditional-value "有"
                      :children (let [pheo-control-options [{:value "药物控制<2周" :label "药物控制 <2 周"}
                                                            {:value "药物控制>2周" :label "药物控制 >2 周"}
                                                            {:value "无症状" :label "无症状"}
                                                            {:value "当前存在症状" :label "当前存在下列症状"}]
                                      pheo-symptom-options [{:label "高血压" :value "hypertension"}
                                                            {:label "心悸" :value "palpitations"}
                                                            {:label "头痛" :value "headache"}
                                                            {:label "多汗" :value "hyperhidrosis"}]]
                                  [:<>
                                   [:> Form.Item {:label "控制情况" :name [:pheochromocytoma :control_status]}
                                    [:> Select {:placeholder "选择控制情况" :style {:width "100%"} :allowClear true
                                                :options pheo-control-options}]]
                                   (when (= pheo-control-status "当前存在症状")
                                     [:> Form.Item {:label "症状" :name [:pheochromocytoma :symptoms]}
                                      [:> Checkbox.Group {:options pheo-symptom-options}]])])}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "皮质醇增多症"
                      :radio-name [:hypercortisolism :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:> Form.Item {:label "具体情况" :name [:hypercortisolism :details]}
                                 [:> Select {:placeholder "选择具体情况" :style {:width "100%"} :allowClear true
                                             :options [{:value "肾上腺皮质功能不全" :label "肾上腺皮质功能不全"}
                                                       {:value "皮质醇增多症" :label "皮质醇增多症"}]}]]}]

                    [:> Form.Item {:label "痛风" :name [:gout :present]}
                     [:> Radio.Group {}
                      [:> Radio {:value "无"} "无"]
                      [:> Radio {:value "有"} "有"]
                      [:> Radio {:value "不祥"} "不祥"]]
                     (when (= gout-present "有") ; Inline conditional input, not using form-item-radio-conditional
                       [:> Form.Item {:name [:gout :details]}
                        [:> Input {:placeholder "请描述痛风详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}}]])]

                    [:> Form.Item {:label "垂体功能减退症" :name [:hypopituitarism :present]}
                     [:> Radio.Group {}
                      [:> Radio {:value "无"} "无"]
                      [:> Radio {:value "有"} "有"]
                      [:> Radio {:value "不祥"} "不祥"]]
                     (when (= hypopituitarism-present "有") ; Inline conditional input
                       [:> Form.Item {:name [:hypopituitarism :details]}
                        [:> Input {:placeholder "请描述垂体功能减退症详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}}]])]

                    [:> Form.Item {:label "其他内分泌系统相关情况" :name [:other_endocrine_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他内分泌系统相关情况请在此注明" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
      :title "内分泌系统"
      :header-color "#f9f0ff"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-endocrine")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))


(defn liver-kidney-system-card "肝肾病史" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        lk-data @(rf/subscribe [::subs/liver-kidney-system-data])
        [form] (Form.useForm)
        initial-form-values lk-data

        liver-disease-type-options [{:label "无" :value "none"}
                                    {:label "药物性肝炎" :value "drug_induced_hepatitis"}
                                    {:label "自身免疫性肝病" :value "autoimmune_liver_disease"}
                                    ;; ... other options
                                    {:label "其他" :value "other_liver_disease"}]
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :liver_kidney_system (js->clj values :keywordize-keys true)]))
        form-items [:<>
                    [:h4 "肝功能"]
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "状态"
                      :radio-name [:liver_function :status]
                      :radio-options [{:label "正常" :value "正常"} {:label "异常" :value "异常"}]
                      :conditional-value "异常"
                      :children [:<>
                                 [:> Row {:gutter 16}
                                  [:> Col {:span 12}
                                   [:> Form.Item {:label "谷丙转氨酶 (ALT)" :name [:liver_function :alt]}
                                    [:> InputNumber {:placeholder "U/L (Ref: 0-40)" :style {:width "100%"} :addonAfter "U/L"}]]]
                                  ;; ... other liver function inputs
                                  [:> Col {:span 12}
                                   [:> Form.Item {:label "血清白蛋白 (Albumin)" :name [:liver_function :albumin]}
                                    [:> InputNumber {:placeholder "g/L (Ref: 35-55)" :style {:width "100%"} :addonAfter "g/L"}]]]]]}]

                    [:h4 {:style {:marginTop "16px"}} "肝脏疾病病史"]
                    [:> Form.Item {:label "类型" :name [:liver_disease_history :types]}
                     [:> Checkbox.Group {:options liver-disease-type-options}]]
                    ;; ... (rest of liver-kidney card)
                    [:h4 {:style {:marginTop "16px"}} "其他情况"]
                    [:> Form.Item {:label "其他肝肾系统相关情况" :name [:other_liver_kidney_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他肝肾系统相关情况请在此注明" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> ProjectOutlined {:style {:marginRight "8px"}}]
      :title "肝肾病史"
      :header-color "#fff7e6"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-liver-kidney")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))

(defn digestive-system-card  "消化系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        ds-data @(rf/subscribe [::subs/digestive-system-data])
        [form] (Form.useForm)
        ;; Watcher for nested condition
        chronic-digestive-symptoms (Form.useWatch [:chronic_digestive_history :symptoms] form)

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        treatment-status-options [
                                  {:label "治愈" :value "治愈"} {:label "好转" :value "好转"}
                                  {:label "仍有症状" :value "仍有症状"} {:label "未治疗" :value "未治疗"}
                                  ]

        acute-gastroenteritis-symptom-options [{:label "恶心" :value "nausea"}
                                               {:label "呕吐" :value "vomiting"}
                                               {:label "腹泻" :value "diarrhea"}]
        esophageal-gastric-duodenal-symptom-options [
                                                     {:label "慢性胃炎" :value "chronic_gastritis"}
                                                     {:label "胃部肿瘤病史" :value "history_of_gastric_tumor"}
                                                     {:label "反流性食管炎" :value "reflux_esophagitis"}
                                                     {:label "慢性胃炎合并幽门螺旋杆菌感染" :value "chronic_gastritis_h_pylori"}
                                                     {:label "溃疡" :value "ulcer"}
                                                     {:label "食道肿瘤" :value "esophageal_tumor"}
                                                     {:label "息肉" :value "polyp"}
                                                     {:label "穿孔" :value "perforation"}
                                                     {:label "应激性溃疡" :value "stress_ulcer"}
                                                     {:label "食管静脉曲张" :value "esophageal_varices"}
                                                     {:label "胃底静脉曲张" :value "fundic_varices"}]
        chronic-digestive-symptom-options [
                                           {:label "胰腺炎" :value "pancreatitis"}
                                           {:label "慢性肠炎" :value "chronic_enteritis"}
                                           {:label "溃疡性结肠炎" :value "ulcerative_colitis"}
                                           {:label "克罗恩病" :value "crohns_disease"}
                                           {:label "肠易激综合征" :value "irritable_bowel_syndrome"}
                                           {:label "肠梗阻" :value "intestinal_obstruction"}
                                           {:label "肠道肿瘤" :value "intestinal_tumor"}
                                           {:label "胆囊炎" :value "cholecystitis"}
                                           {:label "胆囊结石" :value "gallstones"}
                                           {:label "胰腺肿瘤" :value "pancreatic_tumor"}
                                           {:label "其他" :value "other_chronic_digestive"}]
        initial-form-values ds-data
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :digestive_system (js->clj values :keywordize-keys true)]))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "急性胃肠炎病史"
                      :radio-name [:acute_gastroenteritis_history :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "症状" :name [:acute_gastroenteritis_history :symptoms]}
                                  [:> Checkbox.Group {:options acute-gastroenteritis-symptom-options}]]
                                 [:> Form.Item {:label "治疗情况" :name [:acute_gastroenteritis_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-status-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "食管，胃十二指肠疾病病史"
                      :radio-name [:esophageal_gastric_duodenal_history :has]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "相关疾病" :name [:esophageal_gastric_duodenal_history :symptoms]}
                                  [:> Checkbox.Group {:options esophageal-gastric-duodenal-symptom-options
                                                      :style {:display "flex" :flexDirection "column"}}]]
                                 [:> Form.Item {:label "治疗情况" :name [:esophageal_gastric_duodenal_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-status-options}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "慢性消化疾病病史"
                      :radio-name [:chronic_digestive_history :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "相关疾病" :name [:chronic_digestive_history :symptoms]}
                                  [:> Checkbox.Group {:options chronic-digestive-symptom-options
                                                      :style {:display "flex" :flexDirection "column"}}]]
                                 (when (some #{"other_chronic_digestive"} chronic-digestive-symptoms)
                                   [:> Form.Item {:label "其他疾病详情" :name [:chronic_digestive_history :symptoms_other_details]}
                                    [:> Input {:placeholder "请描述其他慢性消化疾病"}]])
                                 [:> Form.Item {:label "治疗情况" :name [:chronic_digestive_history :treatment_status]}
                                  [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                                              :options treatment-status-options}]]]}]

                    ;; 4. 其他情况 (Other Conditions)
                    [:> Form.Item {:label "其他消化系统相关情况" :name [:other_conditions]}
                     [:> Input.TextArea {:placeholder "请描述其他消化系统相关情况"
                                         :rows 4}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> CoffeeOutlined {:style {:marginRight "8px"}}]
      :title "消化系统"
      :header-color "#eff8ff" ; Adjusted color to be slightly different from respiratory for distinction
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-digestive-system")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))

(defn hematologic-system-card "血液系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        hs-data @(rf/subscribe [::subs/hematologic-system-data])
        [form] (Form.useForm)
        ;; Removed Form.useWatch calls for radio groups now handled by form-item-radio-conditional

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        initial-form-values hs-data
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :hematologic_system (js->clj values :keywordize-keys true)]))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "贫血"
                      :radio-name [:anemia :has]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "Hb (g/L)" :name [:anemia :hb]}
                                  [:> InputNumber {:placeholder "请输入Hb值" :style {:width "100%"}}]]
                                 [:> Form.Item {:label "贫血原因及目前治疗方式" :name [:anemia :cause_treatment]}
                                  [:> Input.TextArea {:placeholder "描述贫血原因及目前治疗方式" :rows 3}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "凝血功能障碍"
                      :radio-name [:coagulation_dysfunction :has]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "PT (s)" :help "PT延长超过3秒，即有临床意义" :name [:coagulation_dysfunction :pt]}
                                  [:> InputNumber {:placeholder "秒" :style {:width "100%"}}]]
                                 [:> Form.Item {:label "APTT (s)" :help "APTT延长超过10秒，即有临床意义" :name [:coagulation_dysfunction :aptt]}
                                  [:> InputNumber {:placeholder "秒" :style {:width "100%"}}]]
                                 [:> Form.Item {:label "INR" :name [:coagulation_dysfunction :inr]}
                                  [:> InputNumber {:placeholder "值" :style {:width "100%"}}]]
                                 [:> Form.Item {:label "血小板计数 (×10^9/L)" :name [:coagulation_dysfunction :platelet_count]}
                                  [:> InputNumber {:placeholder "血小板数值" :style {:width "100%"}}]]
                                 [:> Form.Item {:label "D-二聚体 (mg/L)" :name [:coagulation_dysfunction :d_dimer]}
                                  [:> InputNumber {:placeholder "D-dimer值" :style {:width "100%"}}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "血栓史"
                      :radio-name [:thrombosis_history :has]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [[:> Form.Item {:label "详情" :name [:thrombosis_history :details]}
                                  [:> Input {:placeholder "描述血栓史详情"}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "下肢深静脉血栓"
                      :radio-name [:lower_limb_dvt :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:> Form.Item {:label "详情" :name [:lower_limb_dvt :details]}
                                 [:> Input {:placeholder "描述下肢深静脉血栓详情"}]]}]

                    [:> Form.Item {:label "血管超声" :name [:vascular_ultrasound_results]}
                     [:> Input.TextArea {:placeholder "请描述血管超声结果" :rows 3}]]]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> ExperimentOutlined {:style {:marginRight "8px"}}]
      :title "血液系统"
      :header-color "#fff0f6"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-hematologic-system")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))

(defn immune-system-card        "免疫系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        is-data @(rf/subscribe [::subs/immune-system-data])
        [form] (Form.useForm)
        ;; Watchers for nested conditions
        immune-dysfunction-type (Form.useWatch [:immune_dysfunction :type] form)
        autoimmune-symptoms (Form.useWatch [:autoimmune_disease :symptoms] form)

        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        immune-dysfunction-type-options [{:label "获得性免疫缺陷" :value "acquired_immunodeficiency"}
                                         {:label "先天性免疫缺陷" :value "congenital_immunodeficiency"}
                                         {:label "其他" :value "other_immune_dysfunction"}]
        autoimmune-disease-symptom-options [{:label "系统性红斑狼疮" :value "systemic_lupus_erythematosus"}
                                            {:label "类风湿性关节炎" :value "rheumatoid_arthritis"}
                                            {:label "强直性脊柱炎" :value "ankylosing_spondylitis"}
                                            {:label "过敏性紫癜" :value "allergic_purpura"}
                                            {:label "其他" :value "other_autoimmune_symptom"}]
        initial-form-values is-data
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :immune_system (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form ; Use the form instance from the outer let
                                    all-values-clj (js->clj all-values :keywordize-keys true)]
                                (when (contains? changed-values [:immune_dysfunction :has])
                                  (when (not= (get-in all-values-clj [:immune_dysfunction :has]) "有")
                                    (.setFieldsValue form-instance (clj->js {:immune_dysfunction {:type nil :type_other_details nil}}))))
                                (when (contains? changed-values [:immune_dysfunction :type])
                                  (when (not= (get-in all-values-clj [:immune_dysfunction :type]) "other_immune_dysfunction")
                                    (.setFieldsValue form-instance (clj->js {:immune_dysfunction {:type_other_details nil}}))))
                                (when (contains? changed-values [:autoimmune_disease :has])
                                  (when (not= (get-in all-values-clj [:autoimmune_disease :has]) "有")
                                    (.setFieldsValue form-instance (clj->js {:autoimmune_disease {:symptoms nil :symptoms_other_details nil}}))))
                                (when (contains? changed-values [:autoimmune_disease :symptoms])
                                  (when (not (some #{"other_autoimmune_symptom"} (get-in all-values-clj [:autoimmune_disease :symptoms])))
                                    (.setFieldsValue form-instance (clj->js {:autoimmune_disease {:symptoms_other_details nil}}))))
                                ))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "免疫功能障碍"
                      :radio-name [:immune_dysfunction :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "类型" :name [:immune_dysfunction :type]}
                                  [:> Radio.Group {:options immune-dysfunction-type-options}]]
                                 (when (= immune-dysfunction-type "other_immune_dysfunction")
                                   [:> Form.Item {:label "其他类型详情" :name [:immune_dysfunction :type_other_details]}
                                    [:> Input {:placeholder "请描述其他免疫功能障碍类型"}]])]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "自身免疫性疾病"
                      :radio-name [:autoimmune_disease :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "症状" :name [:autoimmune_disease :symptoms]}
                                  [:> Checkbox.Group {:options autoimmune-disease-symptom-options
                                                      :style {:display "flex" :flexDirection "column"}}]]
                                 (when (some #{"other_autoimmune_symptom"} autoimmune-symptoms)
                                   [:> Form.Item {:label "其他症状详情" :name [:autoimmune_disease :symptoms_other_details]}
                                    [:> Input {:placeholder "请描述其他自身免疫性疾病症状"}]])]}]

                    ;; 3. 其他情况 (Other Conditions)
                    [:> Form.Item {:label "其他免疫系统相关情况" :name [:other_immune_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他免疫系统相关情况请在此注明" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
      :title "免疫系统"
      :header-color "#f6ffed"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-immune-system")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))

(defn special-medication-history-card "特殊用药史" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        smh-data @(rf/subscribe [::subs/special-medication-history-data])
        [form] (Form.useForm)
        initial-form-values smh-data

        watch-anticoagulant-present (Form.useWatch [:anticoagulant_antiplatelet :present] form)
        watch-glucocorticoids-present (Form.useWatch [:glucocorticoids :present] form)
        watch-cancer-treatment-present (Form.useWatch [:cancer_treatment :present] form)
        watch-drug-abuse-present (Form.useWatch [:drug_abuse_dependence :present] form)
        watch-neuroleptic-present (Form.useWatch [:neuroleptic_drugs :present] form)
        watch-glp1-agonists-present (Form.useWatch [:glp1_agonists :present] form)

        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :special_medication_history (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)
                                    med-keys [:anticoagulant_antiplatelet
                                              :glucocorticoids
                                              :cancer_treatment
                                              :drug_abuse_dependence
                                              :neuroleptic_drugs
                                              :glp1_agonists]]
                                (doseq [med-key med-keys]
                                  (when (and (contains? changed-values [med-key :present])
                                             (not= (get-in all-values-clj [med-key :present]) "有"))
                                    (.setFieldsValue form-instance (clj->js {med-key {:details nil}}))))))
        render-medication-item (fn [field-key label-text placeholder-text show-details?]
                                 [:div {:key (name field-key)}
                                  [:> Form.Item {:label label-text :name [field-key :present]}
                                   [:> Radio.Group {}
                                    [:> Radio {:value "无"} "无"]
                                    [:> Radio {:value "有"} "有"]]]
                                  (when show-details?
                                    [:> Form.Item {:label "详情" :name [field-key :details] :style {:marginLeft "20px"}}
                                     [:> Input.TextArea {:placeholder (or placeholder-text "请描述详情") :rows 2}]])])
        form-items [:<>
                    (render-medication-item :anticoagulant_antiplatelet "抗凝 / 抗血小板药物" "请描述药物名称、剂量、频率、末次用药时间" (= watch-anticoagulant-present "有"))
                    (render-medication-item :glucocorticoids "糖皮质激素" "请描述药物名称、剂量、频率、用药时长" (= watch-glucocorticoids-present "有"))
                    (render-medication-item :cancer_treatment "肿瘤治疗" "请描述治疗方案、药物、周期、末次治疗时间" (= watch-cancer-treatment-present "有"))
                    (render-medication-item :drug_abuse_dependence "药物滥用依赖史" "请描述药物种类、频率、时长" (= watch-drug-abuse-present "有"))
                    (render-medication-item :neuroleptic_drugs "神经安定类药物" "请描述药物名称、剂量、频率" (= watch-neuroleptic-present "有"))
                    (render-medication-item :glp1_agonists "GLP-1受体激动剂" "例如：利拉鲁肽、司美格鲁肽等。请描述药物名称、剂量、频率、末次用药时间" (= watch-glp1-agonists-present "有"))

                    [:> Form.Item {:label "其他药物使用" :name [:other_drug_use]}
                     [:> Input.TextArea {:placeholder "请描述其他特殊药物使用情况" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
      :title "特殊用药史"
      :header-color "#fffbe6"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-special-medication-history")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))

(defn special-disease-history-card "特殊疾病病史" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        sdh-data @(rf/subscribe [::subs/special-disease-history-data])
        [form] (Form.useForm)
        marfan-lesions (Form.useWatch [:marfan_syndrome :related_lesions] form) ; Watched for conditional rendering of details
        initial-form-values sdh-data ; No date parsing needed

        marfan-related-lesions-options [{:label "眼部病变（晶状体脱位）" :value "eye_lesion_lens_dislocation"}
                                        {:label "心血管病变（主动脉瘤）" :value "cardiovascular_aortic_aneurysm"}
                                        {:label "心血管病变（主动脉夹层）" :value "cardiovascular_aortic_dissection"}
                                        {:label "心血管病变（心脏二尖瓣病变）" :value "cardiovascular_mitral_valve_disease"}
                                        {:label "心血管病变（其他）" :value "cardiovascular_other"}
                                        {:label "骨骼病变（脊柱侧弯）" :value "skeletal_scoliosis"}
                                        {:label "骨骼病变（胸廓畸形）" :value "skeletal_chest_deformity"}
                                        {:label "其他" :value "skeletal_other"}]
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :special_disease_history (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)]
                                ;; Clear Marfan details if Marfan syndrome is not present
                                (when (and (contains? changed-values (clj->js {:marfan_syndrome {:present nil}}))
                                           (not= (get-in all-values-clj [:marfan_syndrome :present]) "有"))
                                  (.setFieldsValue form-instance (clj->js {:marfan_syndrome {:related_lesions []
                                                                                            :cardiovascular_other_details nil
                                                                                            :skeletal_other_details nil}})))
                                ;; Clear cardiovascular_other_details if "cardiovascular_other" is not in related_lesions
                                (when (contains? changed-values (clj->js {:marfan_syndrome {:related_lesions nil}}))
                                  (when (not (some #{"cardiovascular_other"} (get-in all-values-clj [:marfan_syndrome :related_lesions])))
                                    (.setFieldsValue form-instance (clj->js {:marfan_syndrome {:cardiovascular_other_details nil}}))))
                                ;; Clear skeletal_other_details if "skeletal_other" is not in related_lesions
                                (when (contains? changed-values (clj->js {:marfan_syndrome {:related_lesions nil}}))
                                  (when (not (some #{"skeletal_other"} (get-in all-values-clj [:marfan_syndrome :related_lesions])))
                                    (.setFieldsValue form-instance (clj->js {:marfan_syndrome {:skeletal_other_details nil}}))))
                                ))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "马方综合征"
                      :radio-name [:marfan_syndrome :present]
                      :radio-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "相关病变" :name [:marfan_syndrome :related_lesions]}
                                  [:> Checkbox.Group {:options marfan-related-lesions-options
                                                      :style {:display "flex" :flexDirection "column"}}]]
                                 (when (some #{"cardiovascular_other"} marfan-lesions)
                                   [:> Form.Item {:label "其他心血管病变详情" :name [:marfan_syndrome :cardiovascular_other_details]}
                                    [:> Input {:placeholder "请描述其他心血管病变"}]])
                                 (when (some #{"skeletal_other"} marfan-lesions)
                                   [:> Form.Item {:label "其他骨骼畸形详情" :name [:marfan_syndrome :skeletal_other_details]}
                                    [:> Input {:placeholder "请描述其他骨骼畸形"}]])]}]

                    [:> Form.Item {:label "其他特殊疾病" :name [:other_special_diseases]}
                     [:> Input.TextArea {:placeholder "请描述其他特殊疾病情况" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> WarningOutlined {:style {:marginRight "8px"}}]
      :title "特殊疾病病史"
      :header-color "#fff1f0"
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-special-disease-history")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))


(defn nutritional-assessment-card "营养评估" []
  (let [na-data @(rf/subscribe [::subs/nutritional-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        [form] (Form.useForm)
        initial-form-values na-data

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :nutritional_assessment (js->clj values :keywordize-keys true)]))
        form-items [:<>
                    [:h4 "1. 营养评分"]
                    [:> Form.Item {:label "是否 BMI < 20.5" :name [:nutritional_score :bmi_lt_20_5]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "患者在过去 1-3 个月有体重下降吗？" :name [:nutritional_score :weight_loss_last_3_months]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "患者在过去的 1 周内有摄食减少吗？" :name [:nutritional_score :reduced_intake_last_week]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "患者有严重疾病吗 (如 ICU 治疗)？" :name [:nutritional_score :severe_illness]}
                     [:> Radio.Group {:options yes-no-options}]]

                    [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px"}}
                     [:h5 {:style {:marginBottom "4px"}} "评分说明:"]
                     [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]

                    [:h4 {:style {:marginTop "16px"}} "2. FRAIL（针对年龄大于 60 岁病人）"]
                    [:> Form.Item {:label "疲乏 (Fatigue): 是否感到疲乏？" :name [:frail_score :fatigue]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "阻力增加 / 耐力减退 (Resistance): 是否难以独立爬一层楼梯？" :name [:frail_score :resistance]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "自由活动下降 (Ambulation): 是否难以独立行走 100 米？" :name [:frail_score :ambulation]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "疾病状况 (Illness): 是否患有 5 种及以上慢性疾病？" :name [:frail_score :illness_gt_5]}
                     [:> Radio.Group {:options yes-no-options}]]
                    [:> Form.Item {:label "体重下降 (Loss of weight): 1 年或更短时间内体重下降是否超过 5%？" :name [:frail_score :loss_of_weight_gt_5_percent]}
                     [:> Radio.Group {:options yes-no-options}]]

                    [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px"}}
                     [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
                     [:p {:style {:fontSize "12px" :color "gray"}}
                      "0 分：健康；" [:br]
                      "1-2 分：衰弱前期；" [:br]
                      "≥3 分：衰弱。"]]]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> AppleOutlined {:style {:marginRight "8px"}}]
      :title "营养评估"
      :header-color "#f0fff0" ; Light green background
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-nutritional-assessment")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler nil
      :children form-items}]))


(defn pregnancy-assessment-card "妊娠" []
  (let [pa-data @(rf/subscribe [::subs/pregnancy-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        [form] (Form.useForm)
        comorbid-conditions-watch (Form.useWatch [:comorbid_obstetric_conditions] form) ; Watcher for nested condition
        initial-form-values pa-data

        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        gestational-week-options [{:label "0-12 周" :value "0-12_weeks"}
                                  {:label "13-28 周" :value "13-28_weeks"}
                                  {:label ">28 周" :value ">28_weeks"}]
        comorbid-obstetric-options [{:label "单胎" :value "singleton_pregnancy"}
                                    {:label "多胎" :value "multiple_pregnancy"}
                                    {:label "妊娠期糖尿病" :value "gestational_diabetes"}
                                    {:label "妊娠期高血压" :value "gestational_hypertension"}
                                    {:label "周围型前置胎盘" :value "marginal_placenta_previa"}
                                    {:label "中央型前置胎盘" :value "complete_placenta_previa"}
                                    {:label "胎膜早破" :value "premature_rupture_of_membranes"}
                                    {:label "胎盘早剥" :value "placental_abruption"}
                                    {:label "胎盘植入" :value "placenta_accreta"}
                                    {:label "先兆流产" :value "threatened_abortion"}
                                    {:label "子痫前期" :value "preeclampsia"}
                                    {:label "子痫" :value "eclampsia"}
                                    {:label "其他情况" :value "other_obstetric_conditions"}]
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :pregnancy_assessment (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)]
                                ;; Clear pregnancy sub-fields if not pregnant
                                (when (contains? changed-values (clj->js {:is_pregnant nil}))
                                  (when (not= (get-in all-values-clj [:is_pregnant]) "有")
                                    (.setFieldsValue form-instance (clj->js {:gestational_week nil
                                                                              :obstetric_history nil
                                                                              :comorbid_obstetric_conditions []
                                                                              :comorbid_obstetric_conditions_other_details nil}))))
                                ;; Clear other details if "other_obstetric_conditions" is not selected
                                (when (contains? changed-values (clj->js {:comorbid_obstetric_conditions nil}))
                                  (when (not (some #{"other_obstetric_conditions"} (get-in all-values-clj [:comorbid_obstetric_conditions])))
                                    (.setFieldsValue form-instance (clj->js {:comorbid_obstetric_conditions_other_details nil}))))
                                ))
        form-items [:<>
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "是否妊娠"
                      :radio-name [:is_pregnant]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "孕周" :name [:gestational_week]}
                                  [:> Select {:placeholder "选择孕周" :style {:width "100%"} :allowClear true
                                              :options gestational-week-options}]]
                                 [:> Form.Item {:label "孕产史" :name [:obstetric_history]}
                                  [:> Input.TextArea {:placeholder "例如：G2P1A1L1 或 足月1、早产0、流产1、存活1" :rows 2}]]
                                 [:> Form.Item {:label "合并产科情况" :name [:comorbid_obstetric_conditions]}
                                  [:> Checkbox.Group {:options comorbid-obstetric-options
                                                      :style {:display "flex" :flexDirection "column"}}]]
                                 (when (some #{"other_obstetric_conditions"} comorbid-conditions-watch)
                                   [:> Form.Item {:label "其他合并产科情况详情" :name [:comorbid_obstetric_conditions_other_details] :style {:marginLeft "20px"}}
                                    [:> Input {:placeholder "请描述其他合并产科情况"}]])]}]

                    [:> Form.Item {:label "其他妊娠相关情况" :name [:other_pregnancy_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他妊娠相关情况请在此注明" :rows 3}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> WomanOutlined {:style {:marginRight "8px"}}]
      :title "妊娠"
      :header-color "#fff0f6" ; Light pink background
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-pregnancy-assessment")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))


(defn surgical-anesthesia-history-card "手术麻醉史" []
  (let [sah-data @(rf/subscribe [::subs/surgical-anesthesia-history-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        [form] (Form.useForm)
        history-present-watch (Form.useWatch [:history :present] form)
        family-hyperthermia-watch (Form.useWatch [:family_history_malignant_hyperthermia :present] form)
        ;; Watchers for nested conditions, not directly handled by the form-item-radio-conditional itself
        postop-complications-watch (Form.useWatch [:history :postop_complications] form)
        adverse-events-watch (Form.useWatch [:history :adverse_events] form)

        initial-form-values (when sah-data
                              (-> sah-data
                                  (assoc-in [:history :last_anesthesia_date_specific]
                                            (utils/parse-date (get-in sah-data [:history :last_anesthesia_date_specific])))))
        on-finish-fn (fn [values]
                       (let [transformed-values (-> values
                                                    (assoc-in [:history :last_anesthesia_date_specific]
                                                              (when-let [d (get-in values [:history :last_anesthesia_date_specific])]
                                                                (utils/date->iso-string d))))]
                         (rf/dispatch [::events/update-canonical-assessment-section :surgical_anesthesia_history (js->clj transformed-values :keywordize-keys true)])))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)]
                                (when (contains? changed-values (clj->js {:history {:present nil}}))
                                  (when (not= (get-in all-values-clj [:history :present]) "有")
                                    (.setFieldsValue form-instance (clj->js {:history {:last_anesthesia_date_range nil
                                                                                       :last_anesthesia_date_specific nil
                                                                                       :anesthesia_types []
                                                                                       :postop_complications []
                                                                                       :postop_complications_other_details nil
                                                                                       :adverse_events []
                                                                                       :adverse_events_other_details nil
                                                                                       :previous_surgeries nil}}))))
                                (when (contains? changed-values (clj->js {:history {:postop_complications nil}}))
                                  (when (not (some #{"other_postop_complications"} (get-in all-values-clj [:history :postop_complications])))
                                    (.setFieldsValue form-instance (clj->js {:history {:postop_complications_other_details nil}}))))
                                (when (contains? changed-values (clj->js {:history {:adverse_events nil}}))
                                  (when (not (some #{"other_adverse_events"} (get-in all-values-clj [:history :adverse_events])))
                                    (.setFieldsValue form-instance (clj->js {:history {:adverse_events_other_details nil}}))))
                                (when (contains? changed-values (clj->js {:family_history_malignant_hyperthermia {:present nil}}))
                                  (when (not= (get-in all-values-clj [:family_history_malignant_hyperthermia :present]) "有")
                                    (.setFieldsValue form-instance (clj->js {:family_history_malignant_hyperthermia {:relationship nil}}))))))
        last-anesthesia-date-options [{:label ">5 年" :value ">5_years"} {:label "1-5 年" :value "1-5_years"} {:label "<1 年" :value "<1_year"}]
        anesthesia-type-options [{:label "全身麻醉" :value "general_anesthesia"} {:label "椎管内麻醉" :value "spinal_anesthesia"} {:label "神经阻滞" :value "nerve_block"} {:label "局部麻醉" :value "local_anesthesia"}]
        postop-complications-options [{:label "术后恶心呕吐" :value "postop_nausea_vomiting"} {:label "术后疼痛" :value "postop_pain"} {:label "声音嘶哑" :value "hoarseness"} {:label "头晕头疼" :value "dizziness_headache"} {:label "其他" :value "other_postop_complications"}]
        adverse-events-options [{:label "过敏反应" :value "allergic_reaction"} {:label "困难气道" :value "difficult_airway"} {:label "气管切开" :value "tracheostomy"} {:label "术中知晓" :value "intraop_awareness"} {:label "术后认知功能障碍" :value "postop_cognitive_dysfunction"} {:label "恶性高热" :value "malignant_hyperthermia"} {:label "其他" :value "other_adverse_events"}]
        form-items [:<>
                    [:> Form.Item {:label "手术麻醉史" :name [:history :present]}
                     [:> Radio.Group {}
                      [:> Radio {:value "无"} "无"]
                      [:> Radio {:value "有"} "有"]
                      [:> Radio {:value "不祥"} "不祥"]]]

                    (when (= history-present-watch "有")
                      [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
                       [:> Form.Item {:label "上次麻醉日期范围" :name [:history :last_anesthesia_date_range]}
                        [:> Select {:placeholder "选择范围" :style {:width "100%"} :allowClear true
                                    :options last-anesthesia-date-options}]]
                       [:> Form.Item {:label "具体上次麻醉日期 (可选)" :name [:history :last_anesthesia_date_specific]}
                        [:> DatePicker {:style {:width "100%"} :placeholder "选择具体日期"}]]
                       [:> Form.Item {:label "麻醉方式" :name [:history :anesthesia_types]}
                        [:> Checkbox.Group {:options anesthesia-type-options
                                            :style {:display "flex" :flexDirection "column"}}]]
                       [:> Form.Item {:label "术后并发症" :name [:history :postop_complications]}
                        [:> Checkbox.Group {:options postop-complications-options
                                            :style {:display "flex" :flexDirection "column"}}]]
                       (when (some #{"other_postop_complications"} postop-complications-watch)
                         [:> Form.Item {:label "其他术后并发症详情" :name [:history :postop_complications_other_details] :style {:marginLeft "20px"}}
                          [:> Input {:placeholder "请描述其他术后并发症"}]])
                       [:> Form.Item {:label "不良事件" :name [:history :adverse_events]}
                        [:> Checkbox.Group {:options adverse-events-options
                                            :style {:display "flex" :flexDirection "column"}}]]
                       (when (some #{"other_adverse_events"} adverse-events-watch)
                         [:> Form.Item {:label "其他不良事件详情" :name [:history :adverse_events_other_details] :style {:marginLeft "20px"}}
                          [:> Input {:placeholder "请描述其他不良事件"}]])
                       [:> Form.Item {:label "已行手术" :name [:history :previous_surgeries]}
                        [:> Input.TextArea {:placeholder "请列出既往手术名称和日期" :rows 3}]]])

                    [:> Form.Item {:label "有血缘关系的人发生过恶性高热史" :name [:family_history_malignant_hyperthermia :present]}
                     [:> Radio.Group {}
                      [:> Radio {:value "无"} "无"]
                      [:> Radio {:value "有"} "有"]]]
                    (when (= family-hyperthermia-watch "有")
                      [:> Form.Item {:label "关系人" :name [:family_history_malignant_hyperthermia :relationship] :style {:marginLeft "20px"}}
                       [:> Input {:placeholder "请说明与患者关系"}]])

                    [:> Form.Item {:label "其他手术麻醉史相关情况" :name [:other_surgical_anesthesia_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他手术麻醉史相关情况请在此注明" :rows 3}]]]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> HistoryOutlined {:style {:marginRight "8px"}}]
      :title "手术麻醉史"
      :header-color "#e6f7ff" ; Light blue background
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-surgical-anesthesia-history")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))


(defn airway-assessment-card "气道评估" []
  (let [aa-data @(rf/subscribe [::subs/airway-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        [form] (Form.useForm)
        initial-form-values aa-data

        ;; Watchers for conditional rendering and clearing fields
        mouth-opening-degree-watch (Form.useWatch [:detailed_assessment :mouth_opening :degree] form)
        mouth-opening-limit-reasons-watch (Form.useWatch [:detailed_assessment :mouth_opening :limit_reasons] form)
        head-neck-mobility-status-watch (Form.useWatch [:detailed_assessment :head_neck_mobility :status] form)
        teeth-conditions-watch (Form.useWatch [:detailed_assessment :teeth_assessment :conditions] form)
        special-facial-features-watch (Form.useWatch [:detailed_assessment :special_facial_features :features] form)
        snoring-symptoms-watch (Form.useWatch [:detailed_assessment :snoring :symptoms] form)
        airway-diseases-locations-watch (Form.useWatch [:detailed_assessment :airway_related_diseases :locations] form)
        current-symptoms-watch (Form.useWatch [:detailed_assessment :current_airway_symptoms :symptoms] form)
        ;; esophageal-surgery-has-watch is removed as its direct usage is replaced

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        yes-no-suspected-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "疑似" :value "疑似"} {:label "不祥" :value "不祥"}]

        mouth-opening-options [{:label "≥3横指" :value "gte_3_fingers"}
                               {:label "<3横指" :value "lt_3_fingers"}
                               {:label "无法张口" :value "cannot_open"}]
        mouth-opening-limit-reason-options [{:label "颞颌关节功能障碍" :value "tmj_dysfunction"}
                                            {:label "感染" :value "infection"}
                                            {:label "肿瘤" :value "tumor"}
                                            {:label "外伤" :value "trauma"}
                                            {:label "其他" :value "other_mouth_opening_limit"}]
        thyromental-distance-class-options [{:label ">6.5cm" :value "gt_6_5_cm"}
                                            {:label "6.0-6.5cm" :value "6_0_to_6_5_cm"}
                                            {:label "<6.0cm" :value "lt_6_0_cm"}]
        head-neck-mobility-options [{:label "正常" :value "normal"}
                                    {:label "轻度受限" :value "mildly_limited"}
                                    {:label "中度受限" :value "moderately_limited"}
                                    {:label "重度受限" :value "severely_limited"}
                                    {:label "颈椎融合/固定" :value "cervical_fusion_fixation"}]
        mallampati-classification-options [{:label "Ⅰ级" :value "grade_1"}
                                           {:label "Ⅱ级" :value "grade_2"}
                                           {:label "Ⅲ级" :value "grade_3"}
                                           {:label "Ⅳ级" :value "grade_4"}]
        upper-lip-bite-test-options [{:label "Ⅰ级（可咬及上唇红缘）" :value "grade_1_ulbt"}
                                     {:label "Ⅱ级（不能咬及上唇红缘，但可咬及部分上唇）" :value "grade_2_ulbt"}
                                     {:label "Ⅲ级（不能咬及上唇）" :value "grade_3_ulbt"}]
        teeth-assessment-options [{:label "正常" :value "normal_teeth"}
                                  {:label "松动" :value "loose_teeth"}
                                  {:label "缺齿" :value "missing_teeth"}
                                  {:label "义齿（全口/部分）" :value "dentures_full_partial"}
                                  {:label "龅牙" :value "buck_teeth"}
                                  {:label "其他" :value "other_teeth_assessment"}]
        special-facial-features-options [{:label "小颌畸形" :value "micrognathia"}
                                         {:label "腭裂/唇裂" :value "cleft_palate_lip"}
                                         {:label "面部肿瘤" :value "facial_tumor"}
                                         {:label "面部外伤" :value "facial_trauma"}
                                         {:label "其他" :value "other_facial_features"}]
        snoring-symptoms-options [{:label "睡眠呼吸暂停" :value "sleep_apnea"}
                                  {:label "白天嗜睡" :value "daytime_sleepiness"}
                                  {:label "其他" :value "other_snoring_symptoms"}]
        airway-disease-location-options [{:label "上呼吸道" :value "upper_airway"}
                                         {:label "下呼吸道" :value "lower_airway"}
                                         {:label "其他" :value "other_airway_location"}]
        current-airway-symptoms-options [{:label "声音嘶哑" :value "hoarseness"}
                                         {:label "呼吸困难" :value "dyspnea"}
                                         {:label "喘鸣" :value "stridor"}
                                         {:label "吞咽困难" :value "dysphagia"}
                                         {:label "咳嗽/咳痰" :value "cough_sputum"}
                                         {:label "其他" :value "other_current_symptoms"}]
        laryngeal-obstruction-grade-options [{:label "Ⅰ度（静时无症状，活动后轻度呼吸困难）" :value "grade_1_laryngeal"}
                                             {:label "Ⅱ度（静时明显呼吸困难，喉喘鸣，烦躁不安）" :value "grade_2_laryngeal"}
                                             {:label "Ⅲ度（呼吸极度困难，吸气三凹征，发绀，极度烦躁）" :value "grade_3_laryngeal"}
                                             {:label "Ⅳ度（窒息，意识丧失或抽搐）" :value "grade_4_laryngeal"}]
        esophageal-reflux-options [{:label "无" :value "no_reflux"} {:label "有" :value "has_reflux"} {:label "不祥" :value "unknown_reflux"}]
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :airway_assessment (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)
                                    cv (fn [path] (get-in changed-values path)) ; helper to get changed value
                                    av (fn [path] (get-in all-values-clj path)) ; helper to get current value from all_values_clj
                                    set-val (fn [path-val-map] (.setFieldsValue form-instance (clj->js path-val-map)))]

                                (when (cv [:detailed_assessment :mouth_opening :degree])
                                  (when (or (= (av [:detailed_assessment :mouth_opening :degree]) "gte_3_fingers") (nil? (av [:detailed_assessment :mouth_opening :degree])))
                                    (set-val {:detailed_assessment {:mouth_opening {:limit_reasons [] :limit_reasons_other nil}}})))
                                (when (cv [:detailed_assessment :mouth_opening :limit_reasons])
                                  (when (not (some #{"other_mouth_opening_limit"} (av [:detailed_assessment :mouth_opening :limit_reasons])))
                                    (set-val {:detailed_assessment {:mouth_opening {:limit_reasons_other nil}}})))
                                (when (cv [:detailed_assessment :head_neck_mobility :status])
                                  (when (not= (av [:detailed_assessment :head_neck_mobility :status]) "cervical_fusion_fixation")
                                    (set-val {:detailed_assessment {:head_neck_mobility {:details nil}}})))
                                (when (cv [:detailed_assessment :teeth_assessment :conditions])
                                  (when (not (some #{"other_teeth_assessment"} (av [:detailed_assessment :teeth_assessment :conditions])))
                                    (set-val {:detailed_assessment {:teeth_assessment {:other_details nil}}})))
                                (when (cv [:detailed_assessment :special_facial_features :features])
                                  (when (not (some #{"other_facial_features"} (av [:detailed_assessment :special_facial_features :features])))
                                    (set-val {:detailed_assessment {:special_facial_features {:other_details nil}}})))
                                (when (cv [:detailed_assessment :snoring :has])
                                  (when (not= (av [:detailed_assessment :snoring :has]) "有")
                                    (set-val {:detailed_assessment {:snoring {:symptoms [] :symptoms_other_details nil}}})))
                                (when (cv [:detailed_assessment :snoring :symptoms])
                                  (when (not (some #{"other_snoring_symptoms"} (av [:detailed_assessment :snoring :symptoms])))
                                    (set-val {:detailed_assessment {:snoring {:symptoms_other_details nil}}})))
                                (when (cv [:detailed_assessment :airway_related_diseases :has])
                                  (when (not= (av [:detailed_assessment :airway_related_diseases :has]) "有")
                                    (set-val {:detailed_assessment {:airway_related_diseases {:locations [] :upper_airway_details nil :lower_airway_details nil :other_location_details nil}}})))
                                (when (cv [:detailed_assessment :airway_related_diseases :locations])
                                  (when (not (some #{"upper_airway"} (av [:detailed_assessment :airway_related_diseases :locations]))) (set-val {:detailed_assessment {:airway_related_diseases {:upper_airway_details nil}}}))
                                  (when (not (some #{"lower_airway"} (av [:detailed_assessment :airway_related_diseases :locations]))) (set-val {:detailed_assessment {:airway_related_diseases {:lower_airway_details nil}}}))
                                  (when (not (some #{"other_airway_location"} (av [:detailed_assessment :airway_related_diseases :locations]))) (set-val {:detailed_assessment {:airway_related_diseases {:other_location_details nil}}})))
                                (when (cv [:detailed_assessment :mediastinal_history :has])
                                  (when (not= (av [:detailed_assessment :mediastinal_history :has]) "有")
                                    (set-val {:detailed_assessment {:mediastinal_history {:details nil}}})))
                                (when (cv [:detailed_assessment :current_airway_symptoms :has])
                                  (when (not= (av [:detailed_assessment :current_airway_symptoms :has]) "有")
                                    (set-val {:detailed_assessment {:current_airway_symptoms {:symptoms [] :symptoms_other_details nil :laryngeal_obstruction_grade nil}}})))
                                (when (cv [:detailed_assessment :current_airway_symptoms :symptoms])
                                  (when (not (some #{"other_current_symptoms"} (av [:detailed_assessment :current_airway_symptoms :symptoms]))) (set-val {:detailed_assessment {:current_airway_symptoms {:symptoms_other_details nil}}}))
                                  (when (not (some #{"stridor"} (av [:detailed_assessment :current_airway_symptoms :symptoms]))) (set-val {:detailed_assessment {:current_airway_symptoms {:laryngeal_obstruction_grade nil}}})))
                                (when (cv [:detailed_assessment :esophageal_surgery_history :has])
                                  (when (not= (av [:detailed_assessment :esophageal_surgery_history :has]) "有")
                                    (set-val {:detailed_assessment {:esophageal_surgery_history {:reflux_status nil}}})))
                                ))
        form-items [:<>
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

                    [:h4 {:style {:marginTop "16px" :borderTop "1px solid #eee" :paddingTop "10px"}} "详细评估项"]

                    [:> Form.Item {:label "既往困难通气史" :name [:detailed_assessment :difficult_ventilation_history]}
                     [:> Radio.Group {:options yes-no-suspected-unknown-options}]]
                    [:> Form.Item {:label "既往困难插管史" :name [:detailed_assessment :difficult_intubation_history]}
                     [:> Radio.Group {:options yes-no-suspected-unknown-options}]]

                    [:> Form.Item {:label "张口度" :name [:detailed_assessment :mouth_opening :degree]}
                     [:> Select {:placeholder "选择张口度" :style {:width "100%"} :allowClear true
                                 :options mouth-opening-options}]]
                    (when (and mouth-opening-degree-watch (not= mouth-opening-degree-watch "gte_3_fingers"))
                      [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
                       [:> Form.Item {:label "受限原因" :name [:detailed_assessment :mouth_opening :limit_reasons]}
                        [:> Checkbox.Group {:options mouth-opening-limit-reason-options}]]
                       (when (some #{"other_mouth_opening_limit"} mouth-opening-limit-reasons-watch)
                         [:> Form.Item {:label "其他原因详情" :name [:detailed_assessment :mouth_opening :limit_reasons_other]}
                          [:> Input {:placeholder "描述其他原因"}]])])

                    [:> Form.Item {:label "甲颏距离 (cm)" :name [:detailed_assessment :thyromental_distance_cm]}
                     [:> InputNumber {:placeholder "输入距离(cm)" :style {:width "100%"} :min 0}]]
                    [:> Form.Item {:label "甲颏距离 (分级)" :name [:detailed_assessment :thyromental_distance_class]}
                     [:> Select {:placeholder "选择分级" :style {:width "100%"} :allowClear true
                                 :options thyromental-distance-class-options}]]

                    [:> Form.Item {:label "头颈活动度" :name [:detailed_assessment :head_neck_mobility :status]}
                     [:> Select {:placeholder "选择活动度" :style {:width "100%"} :allowClear true
                                 :options head-neck-mobility-options}]]
                    (when (= head-neck-mobility-status-watch "cervical_fusion_fixation")
                      [:> Form.Item {:label "颈椎融合/固定详情" :name [:detailed_assessment :head_neck_mobility :details] :style {:marginLeft "20px"}}
                       [:> Input {:placeholder "描述详情"}]])

                    [:> Form.Item {:label "Mallampati分级" :name [:detailed_assessment :mallampati_classification]}
                     [:> Select {:placeholder "选择Mallampati分级" :style {:width "100%"} :allowClear true
                                 :options mallampati-classification-options}]]
                    [:> Form.Item {:label "上唇咬合试验 (ULBT)" :name [:detailed_assessment :upper_lip_bite_test]}
                     [:> Select {:placeholder "选择ULBT分级" :style {:width "100%"} :allowClear true
                                 :options upper-lip-bite-test-options}]]

                    [:> Form.Item {:label "牙齿评估" :name [:detailed_assessment :teeth_assessment :conditions]}
                     [:> Checkbox.Group {:options teeth-assessment-options
                                         :style {:display "flex" :flexDirection "column"}}]]
                    (when (some #{"other_teeth_assessment"} teeth-conditions-watch)
                      [:> Form.Item {:label "其他牙齿评估详情" :name [:detailed_assessment :teeth_assessment :other_details] :style {:marginLeft "20px"}}
                       [:> Input {:placeholder "描述其他牙齿情况"}]])

                    [:> Form.Item {:label "特殊面部特征 (可多选)" :name [:detailed_assessment :special_facial_features :features]}
                     [:> Checkbox.Group {:options special-facial-features-options
                                         :style {:display "flex" :flexDirection "column"}}]]
                    (when (some #{"other_facial_features"} special-facial-features-watch)
                      [:> Form.Item {:label "其他特殊面部特征详情" :name [:detailed_assessment :special_facial_features :other_details] :style {:marginLeft "20px"}}
                       [:> Input {:placeholder "描述其他特征"}]])

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "鼾症"
                      :radio-name [:detailed_assessment :snoring :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "症状 (可多选)" :name [:detailed_assessment :snoring :symptoms]}
                                  [:> Checkbox.Group {:options snoring-symptoms-options}]]
                                 (when (some #{"other_snoring_symptoms"} snoring-symptoms-watch)
                                   [:> Form.Item {:label "其他鼾症症状详情" :name [:detailed_assessment :snoring :symptoms_other_details]}
                                    [:> Input {:placeholder "描述其他症状"}]])]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "气道相关疾病"
                      :radio-name [:detailed_assessment :airway_related_diseases :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "部位 (可多选)" :name [:detailed_assessment :airway_related_diseases :locations]}
                                  [:> Checkbox.Group {:options airway-disease-location-options}]]
                                 (when (some #{"upper_airway"} airway-diseases-locations-watch)
                                   [:> Form.Item {:label "上呼吸道详情" :name [:detailed_assessment :airway_related_diseases :upper_airway_details]} [:> Input.TextArea {:placeholder "描述上呼吸道疾病详情" :rows 2}]])
                                 (when (some #{"lower_airway"} airway-diseases-locations-watch)
                                   [:> Form.Item {:label "下呼吸道详情" :name [:detailed_assessment :airway_related_diseases :lower_airway_details]} [:> Input.TextArea {:placeholder "描述下呼吸道疾病详情" :rows 2}]])
                                 (when (some #{"other_airway_location"} airway-diseases-locations-watch)
                                   [:> Form.Item {:label "其他部位详情" :name [:detailed_assessment :airway_related_diseases :other_location_details]} [:> Input.TextArea {:placeholder "描述其他部位疾病详情" :rows 2}]])]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "纵隔病史 (如肿瘤、放疗等)"
                      :radio-name [:detailed_assessment :mediastinal_history :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [[:> Form.Item {:label "详情" :name [:detailed_assessment :mediastinal_history :details]}
                                  [:> Input.TextArea {:placeholder "描述纵隔病史详情" :rows 2}]]]}]

                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "现存气道症状"
                      :radio-name [:detailed_assessment :current_airway_symptoms :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:<>
                                 [:> Form.Item {:label "症状 (可多选)" :name [:detailed_assessment :current_airway_symptoms :symptoms]}
                                  [:> Checkbox.Group {:options current-airway-symptoms-options}]]
                                 (when (some #{"other_current_symptoms"} current-symptoms-watch)
                                   [:> Form.Item {:label "其他症状详情" :name [:detailed_assessment :current_airway_symptoms :symptoms_other_details]}
                                    [:> Input {:placeholder "描述其他症状"}]])
                                 (when (some #{"stridor"} current-symptoms-watch)
                                   [:> Form.Item {:label "喉梗阻分级 (若有喘鸣)" :name [:detailed_assessment :current_airway_symptoms :laryngeal_obstruction_grade]}
                                    [:> Select {:placeholder "选择喉梗阻分级" :style {:width "100%"} :allowClear true
                                                :options laryngeal-obstruction-grade-options}]])]}]
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "食管手术史"
                      :radio-name [:detailed_assessment :esophageal_surgery_history :has]
                      :radio-options yes-no-unknown-options
                      :conditional-value "有"
                      :children [:> Form.Item {:label "是否存在返流" :name [:detailed_assessment :esophageal_surgery_history :reflux_status]}
                                 [:> Radio.Group {:options esophageal-reflux-options}]]}]

                    [:> Form.Item {:label "其他气道相关情况" :name [:other_airway_conditions]}
                     [:> Input.TextArea {:placeholder "如有其他气道相关情况请在此注明" :rows 3}]]]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
      :title "气道评估"
      :header-color "#fff7e6" ; Light orange/yellowish background
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-airway-assessment")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))


(defn spinal-anesthesia-assessment-card "椎管内麻醉相关评估" []
  (let [saa-data @(rf/subscribe [::subs/spinal-anesthesia-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        [form] (Form.useForm)
        initial-form-values saa-data
        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        ;; Removed ldh-present-watch and anticoagulants-present-watch
        on-finish-fn (fn [values]
                       (rf/dispatch [::events/update-canonical-assessment-section :spinal_anesthesia_assessment (js->clj values :keywordize-keys true)]))
        on-values-change-fn (fn [changed-values all-values]
                              (let [form-instance form
                                    all-values-clj (js->clj all-values :keywordize-keys true)]
                                (when (contains? changed-values (clj->js {:lumbar_disc_herniation {:present nil}}))
                                  (when (not= (get-in all-values-clj [:lumbar_disc_herniation :present]) "有")
                                    (.setFieldsValue form-instance (clj->js {:lumbar_disc_herniation {:lower_limb_numbness_symptoms nil}}))))
                                (when (contains? changed-values (clj->js {:cardiovascular_system {:anticoagulants_present nil}}))
                                  (when (not= (get-in all-values-clj [:cardiovascular_system :anticoagulants_present]) "有")
                                    (.setFieldsValue form-instance (clj->js {:cardiovascular_system {:anticoagulants_details nil}}))))
                                ))
        render-radio-group (fn [main-key item-key label-text]
                             [:> Form.Item {:label label-text :name [main-key item-key] :key (str (name main-key) "-" (name item-key))}
                              [:> Radio.Group {:options yes-no-options}]])
        render-subsection-title (fn [title]
                                  [:h4 {:style {:marginTop "16px" :marginBottom "8px" :borderBottom "1px solid #f0f0f0" :paddingBottom "4px"}} title])
        form-items [:<>
                    (render-subsection-title "中枢神经系统")
                    (render-radio-group :central_nervous_system :brain_tumor "脑肿瘤")
                    (render-radio-group :central_nervous_system :cerebral_hemorrhage "脑出血")
                    (render-radio-group :central_nervous_system :severe_head_trauma "严重颅脑外伤")
                    (render-radio-group :central_nervous_system :epilepsy "癫痫")

                    (render-subsection-title "外周神经系统")
                    (render-radio-group :peripheral_nervous_system :multiple_sclerosis "多发性硬化")
                    (render-radio-group :peripheral_nervous_system :spinal_cord_injury "脊髓损伤")
                    (render-radio-group :peripheral_nervous_system :scoliosis "脊柱侧弯")
                    (render-radio-group :peripheral_nervous_system :spinal_deformity "脊柱畸形")
                    (render-radio-group :peripheral_nervous_system :intraspinal_tumor "椎管内肿瘤")
                    (render-radio-group :peripheral_nervous_system :ankylosing_spondylitis "强制性脊柱炎")
                    (render-radio-group :peripheral_nervous_system :lumbar_surgery_history "腰椎手术史")

                    (render-subsection-title "腰椎间盘突出")
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "腰椎间盘突出"
                      :radio-name [:lumbar_disc_herniation :present]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [:> Form.Item {:label "下肢麻木症状" :name [:lumbar_disc_herniation :lower_limb_numbness_symptoms] :key "ldh-symptoms"}
                                 [:> Radio.Group {:options yes-no-options}]]}]

                    (render-subsection-title "心血管系统")
                    (render-radio-group :cardiovascular_system :aortic_stenosis "主动脉瓣狭窄")
                    (render-radio-group :cardiovascular_system :hypertrophic_obstructive_cardiomyopathy "肥厚型梗阻型心肌病")
                    [:f> afc/form-item-radio-conditional
                     {:form-instance form
                      :label "抗凝 / 抗血小板药物"
                      :radio-name [:cardiovascular_system :anticoagulants_present]
                      :radio-options yes-no-options
                      :conditional-value "有"
                      :children [:> Form.Item {:label "详情" :name [:cardiovascular_system :anticoagulants_details] :key "cvs-anticoag-details"}
                                 [:> Input.TextArea {:placeholder "请描述药物名称、剂量、频率、末次用药时间" :rows 2}]]}]

                    (render-subsection-title "穿刺点检查")
                    (render-radio-group :puncture_site_inspection :difficult_puncture_history "既往穿刺困难史")
                    (render-radio-group :puncture_site_inspection :local_infection "局部感染")
                    (render-radio-group :puncture_site_inspection :deformity "畸形")

                    (render-subsection-title "局麻药过敏")
                    [:> Form.Item {:label "局麻药过敏史" :name :local_anesthetic_allergy :key "la-allergy"}
                     [:> Radio.Group {:options yes-no-options}]]
                    ]]
    [afc/patient-assessment-card-wrapper
     {:icon [:> GatewayOutlined {:style {:marginRight "8px"}}]
      :title "椎管内麻醉相关评估"
      :header-color "#f0f5ff" ; Light blue background
      :patient-id patient-id
      :form-instance form
      :form-key (str patient-id "-spinal-anesthesia")
      :initial-data initial-form-values
      :on-finish-handler on-finish-fn
      :on-values-change-handler on-values-change-fn
      :children form-items}]))
