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
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [hc.hospital.ui-helpers :refer [custom-styled-card]]
   [hc.hospital.utils :as utils]
   [re-frame.core :as rf]))


(defn circulatory-system-card "循环系统" []
  (let [patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        circulatory-data @(rf/subscribe [::subs/circulatory-system-data])
        get-val (fn [path] (get-in circulatory-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:circulatory_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        dispatch-checkbox-event (fn [path event] (dispatch-update path (-> event .-target .-checked)))

        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "不祥" :value "不祥"}]
        treatment-status-options [
                                  {:label "治愈" :value "治愈"} {:label "好转" :value "好转"}
                                  {:label "仍有症状" :value "仍有症状"} {:label "未治疗" :value "未治疗"}
                                  ]
        cardiac-function-options [
                                  {:value "Ⅰ 级" :label "Ⅰ 级：心功能正常，体力活动不受限制。一般体力活动不引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅱ 级" :label "Ⅱ 级：心功能较差，体力活动轻度受限制。休息时无症状，一般体力活动引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅲ 级" :label "Ⅲ 级：心功能不全，体力活动明显受限制。休息时无症状，但小于一般体力活动即可引起过度疲劳、心悸、气喘或心绞痛"}
                                  {:value "Ⅳ 级" :label "Ⅳ 级：心功能衰竭，不能从事任何体力劳动。休息状态下也出现心衰症状，体力活动后加重"}
                                  ]
        exercise-capacity-options [
                                   {:value "运动能力正常" :label "运动能力正常。可耐受慢跑、跳绳等较高强度的身体训练 >6MET"}
                                   {:value "运动能力轻度下降" :label "运动能力轻度下降。可胜任日常家务工作或骑自行车 3-6MET"}
                                   {:value "运动能力明显下降" :label "运动能力明显下降。仅能从事文书工作或缓慢步行 <3MET"}
                                   ]]
    [custom-styled-card
     [:> HeartOutlined {:style {:marginRight "8px"}}]
     "循环系统"
     "#e6f7ff" ; Unique header background color
     (if patient-id
       [:> Form {:layout "vertical"
                 :key (str patient-id "-circulatory-detailed")}

        ;; 心电图 (ECG)
        [:> Form.Item {:label "心电图"}
         [:> Input.TextArea {:placeholder "请描述ECG结果"
                             :rows 3
                             :value (get-val [:ecg_description])
                             :onChange #(dispatch-update-event [:ecg_description] %)}]]

        ;; Main "心脏疾病病史" (History of Cardiac Diseases) Section
        [:> Form.Item {:label "心脏疾病病史"}
         [:> Radio.Group {:value (get-val [:cardiac_disease_history :has])
                          :onChange #(dispatch-update-event [:cardiac_disease_history :has] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]

        (when (= (get-val [:cardiac_disease_history :has]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}

           ;; Coronary Artery Disease (冠心病)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "冠心病"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:options yes-no-options
                              :value (get-val [:cardiac_disease_history :coronary_artery_disease :has])
                              :onChange #(dispatch-update-event [:cardiac_disease_history :coronary_artery_disease :has] %)}]]
            (when (= (get-val [:cardiac_disease_history :coronary_artery_disease :has]) "有")
              [:<>
               [:> Form.Item {:label "症状"}
                [:> Select {:placeholder "选择症状" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :coronary_artery_disease :symptoms])
                            :options [{:value "无症状" :label "无症状"}
                                      {:value "稳定性心绞痛" :label "稳定性心绞痛"}
                                      {:value "不稳定性心绞痛" :label "不稳定性心绞痛"}
                                      {:value "心梗" :label "心梗"}]
                            :onChange #(dispatch-update [:cardiac_disease_history :coronary_artery_disease :symptoms] %)}]]
               [:> Form.Item {:label "心脏支架"}
                [:> Radio.Group {:options yes-no-options
                                 :value (get-val [:cardiac_disease_history :coronary_artery_disease :stent])
                                 :onChange #(dispatch-update-event [:cardiac_disease_history :coronary_artery_disease :stent] %)}]]
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :coronary_artery_disease :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :coronary_artery_disease :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :coronary_artery_disease :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :coronary_artery_disease :medication] %)}]]])]

           ;; Arrhythmia (心律失常)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心律失常"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:value (get-val [:cardiac_disease_history :arrhythmia :has])
                              :onChange (fn [ev]
                                          (let [val (-> ev .-target .-value)]
                                            (dispatch-update [:cardiac_disease_history :arrhythmia :has] val)
                                            (when (not= val "有")
                                              (dispatch-update [:cardiac_disease_history :arrhythmia :has_details] nil))))}
              (for [opt yes-no-unknown-options]
                ^{:key (:value opt)} [:> Radio {:value (:value opt)} (:label opt)])]]
            (when (= (get-val [:cardiac_disease_history :arrhythmia :has]) "有")
              [:> Input {:placeholder "心律失常类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"} ; Adjusted width
                         :value (get-val [:cardiac_disease_history :arrhythmia :has_details])
                         :onChange #(dispatch-update-event [:cardiac_disease_history :arrhythmia :has_details] %)}])
            (when (or (= (get-val [:cardiac_disease_history :arrhythmia :has]) "有") (= (get-val [:cardiac_disease_history :arrhythmia :has]) "不祥"))
              [:<>
               [:> Form.Item {:label "类型"}
                [:> Select {:placeholder "选择类型" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :arrhythmia :type])
                            :options [{:value "低危型" :label "低危型"}
                                      {:value "中危型" :label "中危型"}
                                      {:value "高危型" :label "高危型"}]
                            :onChange #(dispatch-update [:cardiac_disease_history :arrhythmia :type] %)}]]
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :arrhythmia :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :arrhythmia :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :arrhythmia :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :arrhythmia :medication] %)}]]])]

           ;; Cardiomyopathy (心肌病)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心肌病"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:value (get-val [:cardiac_disease_history :cardiomyopathy :has])
                              :onChange (fn [ev]
                                          (let [val (-> ev .-target .-value)]
                                            (dispatch-update [:cardiac_disease_history :cardiomyopathy :has] val)
                                            (when (not= val "有")
                                              (dispatch-update [:cardiac_disease_history :cardiomyopathy :has_details] nil))))}
              (for [opt yes-no-options] ; Assuming yes/no for this one, adjust if "不祥" needed
                ^{:key (:value opt)} [:> Radio {:value (:value opt)} (:label opt)])]]
            (when (= (get-val [:cardiac_disease_history :cardiomyopathy :has]) "有")
              [:> Input {:placeholder "心肌病类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}
                         :value (get-val [:cardiac_disease_history :cardiomyopathy :has_details])
                         :onChange #(dispatch-update-event [:cardiac_disease_history :cardiomyopathy :has_details] %)}])
            (when (= (get-val [:cardiac_disease_history :cardiomyopathy :has]) "有")
              [:<>
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :cardiomyopathy :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :cardiomyopathy :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :cardiomyopathy :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :cardiomyopathy :medication] %)}]]])]

           ;; Valvular Heart Disease (心脏瓣膜病变)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "心脏瓣膜病变"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:value (get-val [:cardiac_disease_history :valvular_heart_disease :has])
                              :onChange (fn [ev]
                                          (let [val (-> ev .-target .-value)]
                                            (dispatch-update [:cardiac_disease_history :valvular_heart_disease :has] val)
                                            (when (not= val "有")
                                              (dispatch-update [:cardiac_disease_history :valvular_heart_disease :has_details] nil))))}
              (for [opt yes-no-options]
                ^{:key (:value opt)} [:> Radio {:value (:value opt)} (:label opt)])]]
            (when (= (get-val [:cardiac_disease_history :valvular_heart_disease :has]) "有")
              [:> Input {:placeholder "心脏瓣膜病变类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}
                         :value (get-val [:cardiac_disease_history :valvular_heart_disease :has_details])
                         :onChange #(dispatch-update-event [:cardiac_disease_history :valvular_heart_disease :has_details] %)}])
            (when (= (get-val [:cardiac_disease_history :valvular_heart_disease :has]) "有")
              [:<>
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :valvular_heart_disease :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :valvular_heart_disease :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :valvular_heart_disease :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :valvular_heart_disease :medication] %)}]]])]

           ;; Congenital Heart Disease (先天性心脏病)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "先天性心脏病"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:value (get-val [:cardiac_disease_history :congenital_heart_disease :has])
                              :onChange (fn [ev]
                                          (let [val (-> ev .-target .-value)]
                                            (dispatch-update [:cardiac_disease_history :congenital_heart_disease :has] val)
                                            (when (not= val "有")
                                              (dispatch-update [:cardiac_disease_history :congenital_heart_disease :has_details] nil))))}
              (for [opt yes-no-options]
                ^{:key (:value opt)} [:> Radio {:value (:value opt)} (:label opt)])]]
            (when (= (get-val [:cardiac_disease_history :congenital_heart_disease :has]) "有")
              [:> Input {:placeholder "先天性心脏病类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}
                         :value (get-val [:cardiac_disease_history :congenital_heart_disease :has_details])
                         :onChange #(dispatch-update-event [:cardiac_disease_history :congenital_heart_disease :has_details] %)}])
            (when (= (get-val [:cardiac_disease_history :congenital_heart_disease :has]) "有")
              [:<>
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :congenital_heart_disease :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :congenital_heart_disease :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :congenital_heart_disease :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :congenital_heart_disease :medication] %)}]]])]

           ;; Congestive Heart Failure (充血性心力衰竭病史)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "充血性心力衰竭病史"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:options yes-no-options
                              :value (get-val [:cardiac_disease_history :congestive_heart_failure :has])
                              :onChange #(dispatch-update-event [:cardiac_disease_history :congestive_heart_failure :has] %)}]]
            (when (= (get-val [:cardiac_disease_history :congestive_heart_failure :has]) "有")
              [:<>
               [:> Form.Item {:label "上次发作日期"}
                [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                                :value (utils/iso-string->dayjs (get-val [:cardiac_disease_history :congestive_heart_failure :last_episode_date]))
                                :onChange #(dispatch-update [:cardiac_disease_history :congestive_heart_failure :last_episode_date] (if % (utils/date->iso-string %) nil))}]]
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :congestive_heart_failure :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :congestive_heart_failure :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :congestive_heart_failure :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :congestive_heart_failure :medication] %)}]]])]

           ;; Pulmonary Hypertension (肺动脉高压)
           [:div {:style {:marginTop "10px" :paddingTop "10px" :borderTop "1px dashed #ccc"}}
            [:h4 {:style {:fontSize "15px" :marginBottom "10px"}} "肺动脉高压"]
            [:> Form.Item {:label "有无"}
             [:> Radio.Group {:value (get-val [:cardiac_disease_history :pulmonary_hypertension :has])
                              :onChange (fn [ev]
                                          (let [val (-> ev .-target .-value)]
                                            (dispatch-update [:cardiac_disease_history :pulmonary_hypertension :has] val)
                                            (when (not= val "有")
                                              (dispatch-update [:cardiac_disease_history :pulmonary_hypertension :has_details] nil))))}
              (for [opt yes-no-options]
                ^{:key (:value opt)} [:> Radio {:value (:value opt)} (:label opt)])]]
            (when (= (get-val [:cardiac_disease_history :pulmonary_hypertension :has]) "有")
              [:> Input {:placeholder "肺动脉高压类型 (若选择'有')" :style {:marginTop "8px" :width "calc(100% - 0px)"}
                         :value (get-val [:cardiac_disease_history :pulmonary_hypertension :has_details])
                         :onChange #(dispatch-update-event [:cardiac_disease_history :pulmonary_hypertension :has_details] %)}])
            (when (= (get-val [:cardiac_disease_history :pulmonary_hypertension :has]) "有")
              [:<>
               [:> Form.Item {:label "治疗情况"}
                [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:cardiac_disease_history :pulmonary_hypertension :treatment_status])
                            :options treatment-status-options
                            :onChange #(dispatch-update [:cardiac_disease_history :pulmonary_hypertension :treatment_status] %)}]]
               [:> Form.Item {:label "治疗用药"}
                [:> Input.TextArea {:placeholder "描述治疗用药" :rows 2
                                    :value (get-val [:cardiac_disease_history :pulmonary_hypertension :medication])
                                    :onChange #(dispatch-update-event [:cardiac_disease_history :pulmonary_hypertension :medication] %)}]]])]
           ]) ; End of (when (= (get-val [:cardiac_disease_history :has]) "有") ...)

        ;; Pacemaker Implantation Section
        [:> Form.Item {:label "心脏起搏器植入史"}
         [:> Radio.Group {:value (get-val [:pacemaker_history :has])
                          :onChange #(let [val (-> % .-target .-value)]
                                       (dispatch-update [:pacemaker_history :has] val)
                                       (when (not= val "有") ; Clear sub-fields if "有" is not selected
                                         (dispatch-update [:pacemaker_history :type] nil)
                                         (dispatch-update [:pacemaker_history :working_status] nil)))}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]

        (when (= (get-val [:pacemaker_history :has]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "起搏器类型"}
            [:> Radio.Group {:value (get-val [:pacemaker_history :type])
                             :onChange #(let [val (-> % .-target .-value)]
                                          (dispatch-update [:pacemaker_history :type] val)
                                          (when (not= val "永久起搏器") ; Clear working_status if not permanent
                                            (dispatch-update [:pacemaker_history :working_status] nil)))}
             [:> Radio {:value "临时起搏器"} "临时起搏器"]
             [:> Radio {:value "永久起搏器"} "永久起搏器"]]]
           (when (= (get-val [:pacemaker_history :type]) "永久起搏器")
             [:> Form.Item {:label "工作状态"}
              [:> Input {:placeholder "描述永久起搏器工作状态"
                         :value (get-val [:pacemaker_history :working_status])
                         :onChange #(dispatch-update-event [:pacemaker_history :working_status] %)}]])])

        ;; Cardiac Ultrasound Findings Section
        [:> Form.Item {:label "心脏彩超检查"}
         [:> Input.TextArea {:placeholder "请描述心脏彩超检查内容"
                             :rows 4
                             :value (get-val [:cardiac_ultrasound_findings :details])
                             :onChange #(dispatch-update-event [:cardiac_ultrasound_findings :details] %)}]]

        ;; Coronary CTA/Angiography Results Section
        [:> Form.Item {:label "冠脉CTA/冠脉造影结果"}
         [:> Input.TextArea {:placeholder "请描述冠脉CTA/冠脉造影结果内容"
                             :rows 4
                             :value (get-val [:coronary_cta_angiography_results :details])
                             :onChange #(dispatch-update-event [:coronary_cta_angiography_results :details] %)}]]

        ;; Cardiac Function Assessment Section
        [:> Form.Item {:label "心脏功能评估 (NYHA分级)"}
         [:> Radio.Group {:value (get-val [:cardiac_function_assessment :class])
                          :onChange #(dispatch-update-event [:cardiac_function_assessment :class] %)}
          (for [opt cardiac-function-options]
            ^{:key (:value opt)} [:> Radio {:value (:value opt) :style {:display "block" :height "auto" :lineHeight "22px" :whiteSpace "normal" :marginBottom "8px"}}
                                  (:label opt)])]]

        ;; Exercise Capacity Assessment Section
        [:> Form.Item {:label "运动能力评估"}
         [:> Radio.Group {:value (get-val [:exercise_capacity_assessment :level])
                          :onChange #(dispatch-update-event [:exercise_capacity_assessment :level] %)}
          (for [opt exercise-capacity-options]
            ^{:key (:value opt)} [:> Radio {:value (:value opt) :style {:display "block" :height "auto" :lineHeight "22px" :whiteSpace "normal" :marginBottom "8px"}}
                                  (:label opt)])]]

        ;; Other Conditions Section
        [:> Form.Item {:label "其他循环系统相关情况"}
         [:> Input.TextArea {:placeholder "请描述其他循环系统相关情况"
                             :rows 4
                             :value (get-val [:other_cardiac_info :details])
                             :onChange #(dispatch-update-event [:other_cardiac_info :details] %)}]]
        ] ; End of main Form content
       [:> Empty {:description "请先选择患者"}])]))


(defn respiratory-system-card "呼吸系统" []
  (let [respiratory-data @(rf/subscribe [::subs/respiratory-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in respiratory-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:respiratory_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]

    (let [cold-symptom-options [{:label "咳嗽" :value "cough"}
                                {:label "流涕" :value "runny_nose"}
                                {:label "发热" :value "fever"}
                                {:label "咽痛" :value "sore_throat"}
                                {:label "其他" :value "other"}]
          treatment-options [{:value "未治疗" :label "未治疗"}
                             {:value "药物治疗" :label "药物治疗"}
                             {:value "已痊愈" :label "已痊愈"}
                             {:value "其他" :label "其他"}]]
      [custom-styled-card
       [:> CloudOutlined {:style {:marginRight "8px"}}]
       "呼吸系统"
       "#e6fffb"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js respiratory-data)
                   :key (str patient-id "-respiratory")}
          ;; Fields... (Content of respiratory-system-card from anesthesia.cljs)
          [:> Form.Item {:label "近两周内感冒病史" :name :cold_history_last_2_weeks}
           [:> Radio.Group {:value (get-val [:cold_history_last_2_weeks :present])
                            :onChange #(dispatch-update-event [:cold_history_last_2_weeks :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:cold_history_last_2_weeks :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "发病日期" :name [:cold_history_last_2_weeks :onset_date]}
              [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                              :value (when-let [date-str (get-val [:cold_history_last_2_weeks :onset_date])] (dayjs date-str))
                              :onChange #(dispatch-update [:cold_history_last_2_weeks :onset_date] (if % (utils/date->iso-string %) nil))}]]
             [:> Form.Item {:label "症状" :name [:cold_history_last_2_weeks :symptoms]}
              [:> Checkbox.Group {:options cold-symptom-options
                                  :value (get-val [:cold_history_last_2_weeks :symptoms])
                                  :onChange #(dispatch-update [:cold_history_last_2_weeks :symptoms] %)}]]
             [:> Form.Item {:label "治疗情况" :name [:cold_history_last_2_weeks :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:cold_history_last_2_weeks :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:cold_history_last_2_weeks :treatment_status] %)}]]])
          ;; ... (rest of the respiratory card content)
          [:> Form.Item {:label "近一个月内支气管炎 / 肺炎病史" :name :bronchitis_pneumonia_last_month}
           [:> Radio.Group {:value (get-val [:bronchitis_pneumonia_last_month :present])
                            :onChange #(dispatch-update-event [:bronchitis_pneumonia_last_month :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:bronchitis_pneumonia_last_month :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "发病日期" :name [:bronchitis_pneumonia_last_month :onset_date]}
              [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                              :value (when-let [date-str (get-val [:bronchitis_pneumonia_last_month :onset_date])] (dayjs date-str))
                              :onChange #(dispatch-update [:bronchitis_pneumonia_last_month :onset_date] (if % (utils/date->iso-string %) nil))}]]
             [:> Form.Item {:label "治疗情况" :name [:bronchitis_pneumonia_last_month :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:bronchitis_pneumonia_last_month :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:bronchitis_pneumonia_last_month :treatment_status] %)}]]])

          [:> Form.Item {:label "哮喘病史" :name :asthma_history}
           [:> Radio.Group {:value (get-val [:asthma_history :present])
                            :onChange #(dispatch-update-event [:asthma_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:asthma_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "上次发作日期" :name [:asthma_history :last_episode_date]}
              [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                              :value (when-let [date-str (get-val [:asthma_history :last_episode_date])] (dayjs date-str))
                              :onChange #(dispatch-update [:asthma_history :last_episode_date] (if % (utils/date->iso-string %) nil))}]]
             [:> Form.Item {:label "治疗情况" :name [:asthma_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:asthma_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:asthma_history :treatment_status] %)}]]
             [:> Form.Item {:label "用药情况" :name [:asthma_history :medication_status]}
              [:> Select {:placeholder "选择用药情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:asthma_history :medication_status])
                          :options [{:value "规律吸入激素" :label "规律吸入激素"}
                                    {:value "按需使用支扩剂" :label "按需使用支扩剂"}
                                    {:value "其他" :label "其他"} {:value "未用药" :label "未用药"}]
                          :onChange #(dispatch-update [:asthma_history :medication_status] %)}]]
             [:> Form.Item {:label "用药详情" :name [:asthma_history :medication_details]}
              [:> Input.TextArea {:placeholder "请描述具体用药情况" :rows 2
                                  :value (get-val [:asthma_history :medication_details])
                                  :onChange #(dispatch-update-event [:asthma_history :medication_details] %)}]]])

          [:> Form.Item {:label "慢性阻塞性肺疾病 (COPD)" :name :copd_history}
           [:> Radio.Group {:value (get-val [:copd_history :present])
                            :onChange #(dispatch-update-event [:copd_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:copd_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "治疗情况" :name [:copd_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:copd_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:copd_history :treatment_status] %)}]]])

          [:> Form.Item {:label "支气管扩张症" :name :bronchiectasis_history}
           [:> Radio.Group {:value (get-val [:bronchiectasis_history :present])
                            :onChange #(dispatch-update-event [:bronchiectasis_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:bronchiectasis_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "治疗情况" :name [:bronchiectasis_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:bronchiectasis_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:bronchiectasis_history :treatment_status] %)}]]])

          [:> Form.Item {:label "肺部结节" :name :pulmonary_nodules_history}
           [:> Radio.Group {:value (get-val [:pulmonary_nodules_history :present])
                            :onChange #(dispatch-update-event [:pulmonary_nodules_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:pulmonary_nodules_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "治疗情况" :name [:pulmonary_nodules_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:pulmonary_nodules_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:pulmonary_nodules_history :treatment_status] %)}]]])

          [:> Form.Item {:label "肺部肿瘤" :name :lung_tumor_history}
           [:> Radio.Group {:value (get-val [:lung_tumor_history :present])
                            :onChange #(dispatch-update-event [:lung_tumor_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:lung_tumor_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "治疗情况" :name [:lung_tumor_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:lung_tumor_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:lung_tumor_history :treatment_status] %)}]]])

          [:> Form.Item {:label "胸片" :name :chest_xray_results}
           [:> Input.TextArea {:placeholder "请描述胸片结果" :rows 2
                               :value (get-val [:chest_xray_results])
                               :onChange #(dispatch-update-event [:chest_xray_results] %)}]]

          [:> Form.Item {:label "胸部 CT" :name :chest_ct_results}
           [:> Input.TextArea {:placeholder "请描述胸部 CT 结果" :rows 2
                               :value (get-val [:chest_ct_results])
                               :onChange #(dispatch-update-event [:chest_ct_results] %)}]]

          [:> Form.Item {:label "肺功能" :name :pulmonary_function_test_results}
           [:> Input.TextArea {:placeholder "请描述肺功能检查结果" :rows 2
                               :value (get-val [:pulmonary_function_test_results])
                               :onChange #(dispatch-update-event [:pulmonary_function_test_results] %)}]]

          [:> Form.Item {:label "血气分析" :name :blood_gas_analysis_results}
           [:> Input.TextArea {:placeholder "请描述血气分析结果" :rows 2
                               :value (get-val [:blood_gas_analysis_results])
                               :onChange #(dispatch-update-event [:blood_gas_analysis_results] %)}]]

          [:> Form.Item {:label "是否有肺结核" :name :tuberculosis_history}
           [:> Radio.Group {:value (get-val [:tuberculosis_history :present])
                            :onChange #(dispatch-update-event [:tuberculosis_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]]]
          (when (= (get-val [:tuberculosis_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "治疗情况" :name [:tuberculosis_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:tuberculosis_history :treatment_status])
                          :options treatment-options
                          :onChange #(dispatch-update [:tuberculosis_history :treatment_status] %)}]]
             [:> Form.Item {:label "传染性" :name [:tuberculosis_history :infectious]}
              [:> Radio.Group {:value (get-val [:tuberculosis_history :infectious])
                               :onChange #(dispatch-update-event [:tuberculosis_history :infectious] %)}
               [:> Radio {:value "无"} "无"]
               [:> Radio {:value "有"} "有"]
               [:> Radio {:value "不详"} "不详"]]]])

          [:> Form.Item {:label "其他呼吸系统相关情况" :name :other_respiratory_conditions}
           [:> Input.TextArea {:placeholder "如有其他呼吸系统相关情况请在此注明" :rows 3
                               :value (get-val [:other_respiratory_conditions])
                               :onChange #(dispatch-update-event [:other_respiratory_conditions] %)}]]
          ]
         [:> Empty {:description "请先选择患者"}])])))

(defn mental-neuromuscular-system-card "精神及神经肌肉系统" []
  (let [mn-data @(rf/subscribe [::subs/mental-neuromuscular-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in mn-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:mental_neuromuscular_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
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
                                          {:value "其他" :label "其他"}]]
    [custom-styled-card
     [:> UserOutlined {:style {:marginRight "8px"}}]
     "精神及神经肌肉系统"
     "#fffbe6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js mn-data)
                 :key (str patient-id "-mental-neuromuscular")}
        ;; Fields... (Content of mental-neuromuscular-system-card)
        [:> Form.Item {:label "精神认知相关疾病史" :name :psycho_cognitive_history_present}
         [:> Radio.Group {:value (get-val [:psycho_cognitive_history :present])
                          :onChange #(dispatch-update-event [:psycho_cognitive_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]
        (when (= (get-val [:psycho_cognitive_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "症状" :name [:psycho_cognitive_history :symptoms]}
            [:> Checkbox.Group {:options psycho-cog-symptom-options
                                :value (get-val [:psycho_cognitive_history :symptoms])
                                :onChange #(dispatch-update [:psycho_cognitive_history :symptoms] %)}]]
           (when (some #{"other_symptoms"} (get-val [:psycho_cognitive_history :symptoms]))
             [:> Form.Item {:label "其他症状详情" :name [:psycho_cognitive_history :symptoms_other_details]}
              [:> Input {:placeholder "请描述其他症状"
                         :value (get-val [:psycho_cognitive_history :symptoms_other_details])
                         :onChange #(dispatch-update-event [:psycho_cognitive_history :symptoms_other_details] %)}]])
           [:> Form.Item {:label "治疗情况" :name [:psycho_cognitive_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:psycho_cognitive_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:psycho_cognitive_history :treatment_status] %)}]]
           [:> Form.Item {:label "治疗用药" :name [:psycho_cognitive_history :medication]}
            [:> Input.TextArea {:placeholder "请描述治疗用药" :rows 2
                                :value (get-val [:psycho_cognitive_history :medication])
                                :onChange #(dispatch-update-event [:psycho_cognitive_history :medication] %)}]]])
        ;; ... (rest of the mental-neuromuscular card content)
        [:> Form.Item {:label "癫痫病史" :name :epilepsy_history_present}
         [:> Radio.Group {:value (get-val [:epilepsy_history :present])
                          :onChange #(dispatch-update-event [:epilepsy_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]
        (when (= (get-val [:epilepsy_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "近期发作日期" :name [:epilepsy_history :last_seizure_date]}
            [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                            :value (when-let [date-str (get-val [:epilepsy_history :last_seizure_date])] (dayjs date-str))
                            :onChange #(dispatch-update [:epilepsy_history :last_seizure_date] (if % (utils/date->iso-string %) nil))}]]
           [:> Form.Item {:label "治疗情况" :name [:epilepsy_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:epilepsy_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:epilepsy_history :treatment_status] %)}]]
           [:> Form.Item {:label "治疗用药" :name [:epilepsy_history :medication]}
            [:> Input.TextArea {:placeholder "请描述治疗用药" :rows 2
                                :value (get-val [:epilepsy_history :medication])
                                :onChange #(dispatch-update-event [:epilepsy_history :medication] %)}]]])

        [:> Form.Item {:label "眩晕病史" :name :vertigo_history_present}
         [:> Radio.Group {:value (get-val [:vertigo_history :present])
                          :onChange #(dispatch-update-event [:vertigo_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        (when (= (get-val [:vertigo_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "近期发作日期" :name [:vertigo_history :last_episode_date]}
            [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                            :value (when-let [date-str (get-val [:vertigo_history :last_episode_date])] (dayjs date-str))
                            :onChange #(dispatch-update [:vertigo_history :last_episode_date] (if % (utils/date->iso-string %) nil))}]]
           [:> Form.Item {:label "治疗情况" :name [:vertigo_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:vertigo_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:vertigo_history :treatment_status] %)}]]])

        [:> Form.Item {:label "短暂性脑缺血发作病史 (TIA)" :name :tia_history_present}
         [:> Radio.Group {:value (get-val [:tia_history :present])
                          :onChange #(dispatch-update-event [:tia_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]
        (when (= (get-val [:tia_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "近期发作情况" :name [:tia_history :recent_onset_status]}
            [:> Select {:placeholder "选择近期发作情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:tia_history :recent_onset_status])
                        :options [{:value "近 3 月内无发作" :label "近 3 月内无发作"}
                                  {:value "近 3 月内有发作" :label "近 3 月内有发作"}]
                        :onChange #(dispatch-update [:tia_history :recent_onset_status] %)}]]
           [:> Form.Item {:label "治疗情况" :name [:tia_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:tia_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:tia_history :treatment_status] %)}]]])

        [:> Form.Item {:label "脑梗病史" :name :cerebral_infarction_history_present}
         [:> Radio.Group {:value (get-val [:cerebral_infarction_history :present])
                          :onChange #(dispatch-update-event [:cerebral_infarction_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        (when (= (get-val [:cerebral_infarction_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "近期发作日期" :name [:cerebral_infarction_history :last_episode_date]}
            [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                            :value (when-let [date-str (get-val [:cerebral_infarction_history :last_episode_date])] (dayjs date-str))
                            :onChange #(dispatch-update [:cerebral_infarction_history :last_episode_date] (if % (utils/date->iso-string %) nil))}]]
           [:> Form.Item {:label "治疗情况" :name [:cerebral_infarction_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:cerebral_infarction_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:cerebral_infarction_history :treatment_status] %)}]]
           [:> Form.Item {:label "目前用药" :name [:cerebral_infarction_history :medication]}
            [:> Input.TextArea {:placeholder "请描述目前用药" :rows 2
                                :value (get-val [:cerebral_infarction_history :medication])
                                :onChange #(dispatch-update-event [:cerebral_infarction_history :medication] %)}]]])

        [:> Form.Item {:label "脑出血病史" :name :cerebral_hemorrhage_history_present}
         [:> Radio.Group {:value (get-val [:cerebral_hemorrhage_history :present])
                          :onChange #(dispatch-update-event [:cerebral_hemorrhage_history :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        (when (= (get-val [:cerebral_hemorrhage_history :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "近期发作日期" :name [:cerebral_hemorrhage_history :last_episode_date]}
            [:> DatePicker {:style {:width "100%"} :placeholder "选择日期"
                            :value (when-let [date-str (get-val [:cerebral_hemorrhage_history :last_episode_date])] (dayjs date-str))
                            :onChange #(dispatch-update [:cerebral_hemorrhage_history :last_episode_date] (if % (utils/date->iso-string %) nil))}]]
           [:> Form.Item {:label "治疗情况" :name [:cerebral_hemorrhage_history :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:cerebral_hemorrhage_history :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:cerebral_hemorrhage_history :treatment_status] %)}]]])

        [:> Form.Item {:label "帕金森综合症" :name :parkinsons_syndrome_present}
         [:> Radio.Group {:value (get-val [:parkinsons_syndrome :present])
                          :onChange #(dispatch-update-event [:parkinsons_syndrome :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        (when (= (get-val [:parkinsons_syndrome :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "诊断年限 (年)" :name [:parkinsons_syndrome :diagnosis_duration_years]}
            [:> InputNumber {:placeholder "请输入年数" :style {:width "100%"} :min 0
                             :value (get-val [:parkinsons_syndrome :diagnosis_duration_years])
                             :onChange #(dispatch-update [:parkinsons_syndrome :diagnosis_duration_years] %)}]]
           [:> Form.Item {:label "治疗情况" :name [:parkinsons_syndrome :treatment_status]}
            [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                        :value (get-val [:parkinsons_syndrome :treatment_status])
                        :options general-treatment-status-options
                        :onChange #(dispatch-update [:parkinsons_syndrome :treatment_status] %)}]]
           [:> Form.Item {:label "用药情况" :name [:parkinsons_syndrome :medication]}
            [:> Input.TextArea {:placeholder "请描述用药情况" :rows 2
                                :value (get-val [:parkinsons_syndrome :medication])
                                :onChange #(dispatch-update-event [:parkinsons_syndrome :medication] %)}]]])

        [:> Form.Item {:label "颅脑和颈动脉狭窄" :name :cranial_carotid_stenosis_present}
         [:> Radio.Group {:value (get-val [:cranial_carotid_stenosis :present])
                          :onChange #(dispatch-update-event [:cranial_carotid_stenosis :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]
         (when (= (get-val [:cranial_carotid_stenosis :present]) "有")
           [:> Input {:placeholder "请描述狭窄详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}
                      :value (get-val [:cranial_carotid_stenosis :details])
                      :onChange #(dispatch-update-event [:cranial_carotid_stenosis :details] %)}])]

        [:> Form.Item {:label "其他神经肌肉系统情况" :name :other_neuromuscular_conditions_present}
         [:> Radio.Group {:value (get-val [:other_neuromuscular_conditions :present])
                          :onChange #(dispatch-update-event [:other_neuromuscular_conditions :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        (when (= (get-val [:other_neuromuscular_conditions :present]) "有")
          (let [other-symptom-options [{:label "重症肌无力" :value "myasthenia_gravis"}
                                       {:label "格林巴利综合征" :value "guillain_barre"}
                                       {:label "帕金森病史" :value "parkinsons_disease"}
                                       {:label "脊髓灰质炎后综合征" :value "post_polio_syndrome"}
                                       {:label "多发性硬化症" :value "multiple_sclerosis"}
                                       {:label "肌营养不良" :value "muscular_dystrophy"}
                                       {:label "其他" :value "other_specific_conditions"}]]
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "症状" :name [:other_neuromuscular_conditions :symptoms]}
              [:> Checkbox.Group {:options other-symptom-options
                                  :value (get-val [:other_neuromuscular_conditions :symptoms])
                                  :onChange #(dispatch-update [:other_neuromuscular_conditions :symptoms] %)}]]
             (when (some #{"other_specific_conditions"} (get-val [:other_neuromuscular_conditions :symptoms]))
               [:> Form.Item {:label "其他症状详情" :name [:other_neuromuscular_conditions :symptoms_other_details]}
                [:> Input {:placeholder "请描述其他具体情况"
                           :value (get-val [:other_neuromuscular_conditions :symptoms_other_details])
                           :onChange #(dispatch-update-event [:other_neuromuscular_conditions :symptoms_other_details] %)}]])]))]
       ;; ... (End of mental-neuromuscular card content)
       [:> Empty {:description "请先选择患者"}])]))


(defn endocrine-system-card "内分泌系统" []
  (let [endo-data @(rf/subscribe [::subs/endocrine-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in endo-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:endocrine_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]
    (let [thyroid-type-options [{:label "甲亢" :value "hyperthyroidism"}
                                {:label "甲减" :value "hypothyroidism"}
                                {:label "甲状腺术后甲状腺素替代治疗" :value "post_surgery_replacement_therapy"}
                                {:label "桥本" :value "hashimotos"}
                                {:label "其他" :value "other_thyroid_type"}]
          general-treatment-status-options [{:value "治愈" :label "治愈"} {:value "好转" :label "好转"} {:value "仍有症状" :label "仍有症状"} {:value "未治疗" :label "未治疗"} {:value "病情稳定" :label "病情稳定"} {:value "其他" :label "其他"}]]
      [custom-styled-card
       [:> ExperimentOutlined {:style {:marginRight "8px"}}]
       "内分泌系统"
       "#f9f0ff"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js endo-data)
                   :key (str patient-id "-endocrine")}
          ;; Fields... (Content of endocrine-system-card)
          [:> Form.Item {:label "甲状腺疾病病史" :name :thyroid_disease_history_present}
           [:> Radio.Group {:value (get-val [:thyroid_disease_history :present])
                            :onChange #(dispatch-update-event [:thyroid_disease_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:thyroid_disease_history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "类型" :name [:thyroid_disease_history :types]}
              [:> Checkbox.Group {:options thyroid-type-options
                                  :value (get-val [:thyroid_disease_history :types])
                                  :onChange #(dispatch-update [:thyroid_disease_history :types] %)}]]
             (when (some #{"other_thyroid_type"} (get-val [:thyroid_disease_history :types]))
               [:> Form.Item {:label "其他类型详情" :name [:thyroid_disease_history :type_other_details]}
                [:> Input {:placeholder "请描述其他甲状腺疾病类型"
                           :value (get-val [:thyroid_disease_history :type_other_details])
                           :onChange #(dispatch-update-event [:thyroid_disease_history :type_other_details] %)}]])
             [:> Form.Item {:label "甲状腺功能检查" :name [:thyroid_disease_history :function_test_results]}
              [:> Input.TextArea {:placeholder "请描述甲状腺功能检查结果" :rows 2
                                  :value (get-val [:thyroid_disease_history :function_test_results])
                                  :onChange #(dispatch-update-event [:thyroid_disease_history :function_test_results] %)}]]
             [:> Form.Item {:label "治疗情况" :name [:thyroid_disease_history :treatment_status]}
              [:> Select {:placeholder "选择治疗情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:thyroid_disease_history :treatment_status])
                          :options general-treatment-status-options
                          :onChange #(dispatch-update [:thyroid_disease_history :treatment_status] %)}]]
             [:> Form.Item {:label "甲状腺是否肿大压迫气管，是否存在困难气道？" :name [:thyroid_disease_history :airway_compression]}
              [:> Radio.Group {:value (get-val [:thyroid_disease_history :airway_compression])
                               :onChange #(dispatch-update-event [:thyroid_disease_history :airway_compression] %)}
               [:> Radio {:value "无"} "无"]
               [:> Radio {:value "有"} "有"]]]
             [:> Form.Item {:label "是否合并甲状腺心脏病" :name [:thyroid_disease_history :thyroid_heart_disease]}
              [:> Radio.Group {:value (get-val [:thyroid_disease_history :thyroid_heart_disease])
                               :onChange #(dispatch-update-event [:thyroid_disease_history :thyroid_heart_disease] %)}
               [:> Radio {:value "无"} "无"]
               [:> Radio {:value "有"} "有"]]]])
          ;; ... (rest of endocrine card)
          [:> Form.Item {:label "糖尿病病史" :name :diabetes_history_present}
           [:> Radio.Group {:value (get-val [:diabetes_history :present])
                            :onChange #(dispatch-update-event [:diabetes_history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:diabetes_history :present]) "有")
            (let [diabetes-type-options [{:value "1型糖尿病" :label "1 型糖尿病"} {:value "2型糖尿病" :label "2 型糖尿病"}]
                  diabetes-control-options [{:value "饮食控制" :label "饮食控制"}
                                            {:value "药物控制" :label "药物控制"}
                                            {:value "胰岛素控制" :label "胰岛素控制"}
                                            {:value "未控制" :label "未控制"}]]
              [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
               [:> Form.Item {:label "类型" :name [:diabetes_history :type]}
                [:> Select {:placeholder "选择类型" :style {:width "100%"} :allowClear true
                            :value (get-val [:diabetes_history :type])
                            :options diabetes-type-options
                            :onChange #(dispatch-update [:diabetes_history :type] %)}]]
               [:> Form.Item {:label "控制方式" :name [:diabetes_history :control_method]}
                [:> Select {:placeholder "选择控制方式" :style {:width "100%"} :allowClear true
                            :value (get-val [:diabetes_history :control_method])
                            :options diabetes-control-options
                            :onChange #(dispatch-update [:diabetes_history :control_method] %)}]]
               (when (= (get-val [:diabetes_history :control_method]) "药物控制")
                 [:> Form.Item {:label "药物详情" :name [:diabetes_history :medication_details]}
                  [:> Input {:placeholder "请输入药物控制详情"
                             :value (get-val [:diabetes_history :medication_details])
                             :onChange #(dispatch-update-event [:diabetes_history :medication_details] %)}]])
               [:> Form.Item {:label "血糖（Glu）" :name [:diabetes_history :blood_glucose_level]}
                [:> InputNumber {:placeholder "mmol/L" :style {:width "100%"}
                                 :addonAfter "mmol/L"
                                 :value (get-val [:diabetes_history :blood_glucose_level])
                                 :onChange #(dispatch-update [:diabetes_history :blood_glucose_level] %)}]]
               [:> Form.Item {:label "糖化血红蛋白（HbA1c）" :name [:diabetes_history :hba1c_level]}
                [:> InputNumber {:placeholder "%" :style {:width "100%"}
                                 :addonAfter "%"
                                 :value (get-val [:diabetes_history :hba1c_level])
                                 :onChange #(dispatch-update [:diabetes_history :hba1c_level] %)}]]]))

          [:> Form.Item {:label "嗜铬细胞瘤" :name :pheochromocytoma_present}
           [:> Radio.Group {:value (get-val [:pheochromocytoma :present])
                            :onChange #(dispatch-update-event [:pheochromocytoma :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]]]
          (when (= (get-val [:pheochromocytoma :present]) "有")
            (let [pheo-control-options [{:value "药物控制<2周" :label "药物控制 <2 周"}
                                        {:value "药物控制>2周" :label "药物控制 >2 周"}
                                        {:value "无症状" :label "无症状"}
                                        {:value "当前存在症状" :label "当前存在下列症状"}]
                  pheo-symptom-options [{:label "高血压" :value "hypertension"}
                                        {:label "心悸" :value "palpitations"}
                                        {:label "头痛" :value "headache"}
                                        {:label "多汗" :value "hyperhidrosis"}]]
              [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
               [:> Form.Item {:label "控制情况" :name [:pheochromocytoma :control_status]}
                [:> Select {:placeholder "选择控制情况" :style {:width "100%"} :allowClear true
                            :value (get-val [:pheochromocytoma :control_status])
                            :options pheo-control-options
                            :onChange #(dispatch-update [:pheochromocytoma :control_status] %)}]]
               (when (= (get-val [:pheochromocytoma :control_status]) "当前存在症状")
                 [:> Form.Item {:label "症状" :name [:pheochromocytoma :symptoms]}
                  [:> Checkbox.Group {:options pheo-symptom-options
                                      :value (get-val [:pheochromocytoma :symptoms])
                                      :onChange #(dispatch-update [:pheochromocytoma :symptoms] %)}]])]))

          [:> Form.Item {:label "皮质醇增多症" :name :hypercortisolism_present}
           [:> Radio.Group {:value (get-val [:hypercortisolism :present])
                            :onChange #(dispatch-update-event [:hypercortisolism :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:hypercortisolism :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "具体情况" :name [:hypercortisolism :details]}
              [:> Select {:placeholder "选择具体情况" :style {:width "100%"} :allowClear true
                          :value (get-val [:hypercortisolism :details])
                          :options [{:value "肾上腺皮质功能不全" :label "肾上腺皮质功能不全"}
                                    {:value "皮质醇增多症" :label "皮质醇增多症"}]
                          :onChange #(dispatch-update [:hypercortisolism :details] %)}]]])

          [:> Form.Item {:label "痛风" :name :gout_present}
           [:> Radio.Group {:value (get-val [:gout :present])
                            :onChange #(dispatch-update-event [:gout :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]
           (when (= (get-val [:gout :present]) "有")
             [:> Input {:placeholder "请描述痛风详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}
                        :value (get-val [:gout :details])
                        :onChange #(dispatch-update-event [:gout :details] %)}])]

          [:> Form.Item {:label "垂体功能减退症" :name :hypopituitarism_present}
           [:> Radio.Group {:value (get-val [:hypopituitarism :present])
                            :onChange #(dispatch-update-event [:hypopituitarism :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]
           (when (= (get-val [:hypopituitarism :present]) "有")
             [:> Input {:placeholder "请描述垂体功能减退症详情" :style {:marginLeft "10px" :width "calc(100% - 150px)"}
                        :value (get-val [:hypopituitarism :details])
                        :onChange #(dispatch-update-event [:hypopituitarism :details] %)}])]

          [:> Form.Item {:label "其他内分泌系统相关情况" :name :other_endocrine_conditions}
           [:> Input.TextArea {:placeholder "如有其他内分泌系统相关情况请在此注明" :rows 3
                               :value (get-val [:other_endocrine_conditions])
                               :onChange #(dispatch-update-event [:other_endocrine_conditions] %)}]]
          [:> Empty {:description "请先选择患者"}]])])))


(defn liver-kidney-system-card "肝肾病史" []
  (let [lk-data @(rf/subscribe [::subs/liver-kidney-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in lk-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:liver_kidney_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        liver-disease-type-options [{:label "无" :value "none"}
                                    {:label "药物性肝炎" :value "drug_induced_hepatitis"}
                                    {:label "自身免疫性肝病" :value "autoimmune_liver_disease"}
                                    ;; ... other options
                                    {:label "其他" :value "other_liver_disease"}]]
    [custom-styled-card
     [:> ProjectOutlined {:style {:marginRight "8px"}}]
     "肝肾病史"
     "#fff7e6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js lk-data)
                 :key (str patient-id "-liver-kidney")}
        ;; Fields... (Content of liver-kidney-system-card)
        [:h4 "肝功能"]
        [:> Form.Item {:label "状态" :name [:liver_function :status]}
         [:> Radio.Group {:value (get-val [:liver_function :status])
                          :onChange #(dispatch-update-event [:liver_function :status] %)}
          [:> Radio {:value "正常"} "正常"]
          [:> Radio {:value "异常"} "异常"]]]
        (when (= (get-val [:liver_function :status]) "异常")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Row {:gutter 16}
            [:> Col {:span 12}
             [:> Form.Item {:label "谷丙转氨酶 (ALT)" :name [:liver_function :alt]}
              [:> InputNumber {:placeholder "U/L (Ref: 0-40)" :style {:width "100%"} :addonAfter "U/L"
                               :value (get-val [:liver_function :alt])
                               :onChange #(dispatch-update [:liver_function :alt] %)}]]]
            ;; ... other liver function inputs
            [:> Col {:span 12}
             [:> Form.Item {:label "血清白蛋白 (Albumin)" :name [:liver_function :albumin]}
              [:> InputNumber {:placeholder "g/L (Ref: 35-55)" :style {:width "100%"} :addonAfter "g/L"
                               :value (get-val [:liver_function :albumin])
                               :onChange #(dispatch-update [:liver_function :albumin] %)}]]]]])

        [:h4 {:style {:marginTop "16px"}} "肝脏疾病病史"]
        [:> Form.Item {:label "类型" :name [:liver_disease_history :types]}
         [:> Checkbox.Group {:options liver-disease-type-options
                             :value (get-val [:liver_disease_history :types])
                             :onChange #(dispatch-update [:liver_disease_history :types] %)}]]
        ;; ... (rest of liver-kidney card)
        [:h4 {:style {:marginTop "16px"}} "其他情况"]
        [:> Form.Item {:label "其他肝肾系统相关情况" :name :other_liver_kidney_conditions}
         [:> Input.TextArea {:placeholder "如有其他肝肾系统相关情况请在此注明" :rows 3
                             :value (get-val [:other_liver_kidney_conditions])
                             :onChange #(dispatch-update-event [:other_liver_kidney_conditions] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))

(defn digestive-system-card  "消化系统" []
  (let [ds-data @(rf/subscribe [::subs/digestive-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in ds-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:digestive_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]
    (let [acute-gastroenteritis-symptoms [{:label "恶心" :value "nausea"} {:label "呕吐" :value "vomiting"} {:label "腹泻" :value "diarrhea"}]]
      [custom-styled-card
       [:> CoffeeOutlined {:style {:marginRight "8px"}}]
       "消化系统"
       "#e6fffb"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js ds-data)
                   :key (str patient-id "-digestive")}
          ;; Fields... (Content of digestive-system-card)
          [:> Form.Item {:label "急性胃肠炎病史" :name :acute_gastroenteritis_present}
           [:> Radio.Group {:value (get-val [:acute_gastroenteritis :present])
                            :onChange #(dispatch-update-event [:acute_gastroenteritis :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          ;; ... (rest of digestive card)
          [:> Form.Item {:label "其他消化系统相关情况" :name :other_digestive_conditions}
           [:> Input.TextArea {:placeholder "如有其他消化系统相关情况请在此注明" :rows 3
                               :value (get-val [:other_digestive_conditions])
                               :onChange #(dispatch-update-event [:other_digestive_conditions] %)}]]
          ]
         [:> Empty {:description "请先选择患者"}])])))

(defn hematologic-system-card "血液系统" []
  (let [hs-data @(rf/subscribe [::subs/hematologic-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in hs-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:hematologic_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]
    [custom-styled-card
     [:> ExperimentOutlined {:style {:marginRight "8px"}}]
     "血液系统"
     "#fff0f6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js hs-data)
                 :key (str patient-id "-hematologic")}
        ;; Fields... (Content of hematologic-system-card)
        [:> Form.Item {:label "贫血" :name :anemia_present}
         [:> Radio.Group {:value (get-val [:anemia :present])
                          :onChange #(dispatch-update-event [:anemia :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]]]
        ;; ... (rest of hematologic card)
        [:> Form.Item {:label "血管超声" :name :vascular_ultrasound_results}
         [:> Input.TextArea {:placeholder "请描述血管超声结果" :rows 3
                             :value (get-val [:vascular_ultrasound_results])
                             :onChange #(dispatch-update-event [:vascular_ultrasound_results] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))

(defn immune-system-card        "免疫系统" []
  (let [is-data @(rf/subscribe [::subs/immune-system-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in is-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:immune_system] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]
    (let [immune-dysfunction-type-options [{:label "获得性免疫缺陷" :value "acquired_immunodeficiency"} {:label "先天性免疫缺陷" :value "congenital_immunodeficiency"} {:label "其他" :value "other_immune_dysfunction"}]
          autoimmune-disease-symptom-options [{:label "系统性红斑狼疮" :value "systemic_lupus_erythematosus"} {:label "类风湿性关节炎" :value "rheumatoid_arthritis"} {:label "强直性脊柱炎" :value "ankylosing_spondylitis"} {:label "过敏性紫癜" :value "allergic_purpura"} {:label "其他" :value "other_autoimmune_symptom"}]]
      [custom-styled-card
       [:> SecurityScanOutlined {:style {:marginRight "8px"}}]
       "免疫系统"
       "#f6ffed"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js is-data)
                   :key (str patient-id "-immune")}
          ;; Fields... (Content of immune-system-card)
          [:> Form.Item {:label "免疫功能障碍" :name :immune_dysfunction_present}
           [:> Radio.Group {:value (get-val [:immune_dysfunction :present])
                            :onChange #(dispatch-update-event [:immune_dysfunction :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          ;; ... (rest of immune card)
          [:> Form.Item {:label "其他免疫系统相关情况" :name :other_immune_conditions}
           [:> Input.TextArea {:placeholder "如有其他免疫系统相关情况请在此注明" :rows 3
                               :value (get-val [:other_immune_conditions])
                               :onChange #(dispatch-update-event [:other_immune_conditions] %)}]]
          ]
         [:> Empty {:description "请先选择患者"}])])))

(defn special-medication-history-card "特殊用药史" []
  (let [smh-data @(rf/subscribe [::subs/special-medication-history-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in smh-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:special_medication_history] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))

        render-medication-item (fn [field-key label-text placeholder-text] ; Helper specific to this card
                                 [:div {:key (name field-key)}
                                  [:> Form.Item {:label label-text :name [field-key :present]}
                                   [:> Radio.Group {:value (get-val [field-key :present])
                                                    :onChange #(let [val (-> % .-target .-value)]
                                                                 (dispatch-update [field-key :present] val)
                                                                 (when (= val "无")
                                                                   (dispatch-update [field-key :details] nil)))}
                                    [:> Radio {:value "无"} "无"]
                                    [:> Radio {:value "有"} "有"]]]
                                  (when (= (get-val [field-key :present]) "有")
                                    [:> Form.Item {:label "详情" :name [field-key :details] :style {:marginLeft "20px"}}
                                     [:> Input.TextArea {:placeholder (or placeholder-text "请描述详情") :rows 2
                                                         :value (get-val [field-key :details])
                                                         :onChange #(dispatch-update-event [field-key :details] %)}]])])]
    [custom-styled-card
     [:> MedicineBoxOutlined {:style {:marginRight "8px"}}]
     "特殊用药史"
     "#fffbe6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js smh-data)
                 :key (str patient-id "-special-medication-history")}
        (render-medication-item :anticoagulant_antiplatelet "抗凝 / 抗血小板药物" "请描述药物名称、剂量、频率、末次用药时间")
        (render-medication-item :glucocorticoids "糖皮质激素" "请描述药物名称、剂量、频率、用药时长")
        (render-medication-item :cancer_treatment "肿瘤治疗" "请描述治疗方案、药物、周期、末次治疗时间")
        (render-medication-item :drug_abuse_dependence "药物滥用依赖史" "请描述药物种类、频率、时长")
        (render-medication-item :neuroleptic_drugs "神经安定类药物" "请描述药物名称、剂量、频率")
        (render-medication-item :glp1_agonists "GLP-1受体激动剂" "例如：利拉鲁肽、司美格鲁肽等。请描述药物名称、剂量、频率、末次用药时间")

        [:> Form.Item {:label "其他药物使用" :name :other_drug_use}
         [:> Input.TextArea {:placeholder "请描述其他特殊药物使用情况" :rows 3
                             :value (get-val [:other_drug_use])
                             :onChange #(dispatch-update-event [:other_drug_use] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))

(defn special-disease-history-card   "特殊疾病病史" []
  (let [sdh-data @(rf/subscribe [::subs/special-disease-history-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in sdh-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:special_disease_history] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        marfan-related-lesions-options [{:label "眼部病变（晶状体脱位）" :value "eye_lesion_lens_dislocation"}
                                        {:label "心血管病变（主动脉瘤）" :value "cardiovascular_aortic_aneurysm"}
                                        {:label "心血管病变（主动脉夹层）" :value "cardiovascular_aortic_dissection"}
                                        {:label "心血管病变（心脏二尖瓣病变）" :value "cardiovascular_mitral_valve_disease"}
                                        {:label "心血管病变（其他）" :value "cardiovascular_other"}
                                        {:label "骨骼病变（脊柱侧弯）" :value "skeletal_scoliosis"}
                                        {:label "骨骼病变（胸廓畸形）" :value "skeletal_chest_deformity"}
                                        {:label "其他" :value "skeletal_other"}]]
    [custom-styled-card
     [:> WarningOutlined {:style {:marginRight "8px"}}]
     "特殊疾病病史"
     "#fff1f0"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js sdh-data)
                 :key (str patient-id "-special-disease-history")}
        ;; Fields... (Content of special-disease-history-card)
        [:> Form.Item {:label "马方综合征" :name :marfan_syndrome_present}
         [:> Radio.Group {:value (get-val [:marfan_syndrome :present])
                          :onChange #(dispatch-update-event [:marfan_syndrome :present] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]
        (when (= (get-val [:marfan_syndrome :present]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "相关病变" :name [:marfan_syndrome :related_lesions]}
            [:> Checkbox.Group {:options marfan-related-lesions-options
                                :style {:display "flex" :flexDirection "column"}
                                :value (get-val [:marfan_syndrome :related_lesions])
                                :onChange #(dispatch-update [:marfan_syndrome :related_lesions] %)}]]
           (when (some #{"cardiovascular_other"} (get-val [:marfan_syndrome :related_lesions]))
             [:> Form.Item {:label "其他心血管病变详情" :name [:marfan_syndrome :cardiovascular_other_details]}
              [:> Input {:placeholder "请描述其他心血管病变"
                         :value (get-val [:marfan_syndrome :cardiovascular_other_details])
                         :onChange #(dispatch-update-event [:marfan_syndrome :cardiovascular_other_details] %)}]])
           (when (some #{"skeletal_other"} (get-val [:marfan_syndrome :related_lesions]))
             [:> Form.Item {:label "其他骨骼畸形详情" :name [:marfan_syndrome :skeletal_other_details]}
              [:> Input {:placeholder "请描述其他骨骼畸形"
                         :value (get-val [:marfan_syndrome :skeletal_other_details])
                         :onChange #(dispatch-update-event [:marfan_syndrome :skeletal_other_details] %)}]])])
        [:> Form.Item {:label "其他特殊疾病" :name :other_special_diseases}
         [:> Input.TextArea {:placeholder "请描述其他特殊疾病情况" :rows 3
                             :value (get-val [:other_special_diseases])
                             :onChange #(dispatch-update-event [:other_special_diseases] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))


(defn nutritional-assessment-card        "营养评估" []
  (let [na-data @(rf/subscribe [::subs/nutritional-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in na-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:nutritional_assessment] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]]
    [custom-styled-card
     [:> AppleOutlined {:style {:marginRight "8px"}}]
     "营养评估"
     "#f0fff0"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js na-data)
                 :key (str patient-id "-nutritional-assessment")}
        [:h4 "1. 营养评分"]
        [:> Form.Item {:label "是否 BMI < 20.5" :name [:nutritional_score :bmi_lt_20_5]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:nutritional_score :bmi_lt_20_5]) :onChange #(dispatch-update-event [:nutritional_score :bmi_lt_20_5] %)}]]
        [:> Form.Item {:label "患者在过去 1-3 个月有体重下降吗？" :name [:nutritional_score :weight_loss_last_3_months]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:nutritional_score :weight_loss_last_3_months]) :onChange #(dispatch-update-event [:nutritional_score :weight_loss_last_3_months] %)}]]
        [:> Form.Item {:label "患者在过去的 1 周内有摄食减少吗？" :name [:nutritional_score :reduced_intake_last_week]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:nutritional_score :reduced_intake_last_week]) :onChange #(dispatch-update-event [:nutritional_score :reduced_intake_last_week] %)}]]
        [:> Form.Item {:label "患者有严重疾病吗 (如 ICU 治疗)？" :name [:nutritional_score :severe_illness]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:nutritional_score :severe_illness]) :onChange #(dispatch-update-event [:nutritional_score :severe_illness] %)}]]
        [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px" :marginBottom "16px"}}
         [:h5 {:style {:marginBottom "4px"}} "评分说明:"]
         [:p {:style {:fontSize "12px" :color "gray"}} "每项“是”计1分，总分≥2分提示存在营养风险，建议进一步评估。"]]
        [:h4 {:style {:marginTop "16px"}} "2. FRAIL（针对年龄大于 60 岁病人）"]
        [:> Form.Item {:label "疲乏 (Fatigue): 是否感到疲乏？" :name [:frail_score :fatigue]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:frail_score :fatigue]) :onChange #(dispatch-update-event [:frail_score :fatigue] %)}]]
        [:> Form.Item {:label "阻力增加 / 耐力减退 (Resistance): 是否难以独立爬一层楼梯？" :name [:frail_score :resistance]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:frail_score :resistance]) :onChange #(dispatch-update-event [:frail_score :resistance] %)}]]
        [:> Form.Item {:label "自由活动下降 (Ambulation): 是否难以独立行走 100 米？" :name [:frail_score :ambulation]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:frail_score :ambulation]) :onChange #(dispatch-update-event [:frail_score :ambulation] %)}]]
        [:> Form.Item {:label "疾病状况 (Illness): 是否患有 5 种及以上慢性疾病？" :name [:frail_score :illness_gt_5]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:frail_score :illness_gt_5]) :onChange #(dispatch-update-event [:frail_score :illness_gt_5] %)}]]
        [:> Form.Item {:label "体重下降 (Loss of weight): 1 年或更短时间内体重下降是否超过 5%？" :name [:frail_score :loss_of_weight_gt_5_percent]}
         [:> Radio.Group {:options yes-no-options :value (get-val [:frail_score :loss_of_weight_gt_5_percent]) :onChange #(dispatch-update-event [:frail_score :loss_of_weight_gt_5_percent] %)}]]
        [:div {:style {:padding "8px" :border "1px solid #d9d9d9" :borderRadius "2px"}}
         [:h5 {:style {:marginBottom "4px"}} "FRAIL 结论:"]
         [:p {:style {:fontSize "12px" :color "gray"}}
          "0 分：健康；" [:br]
          "1-2 分：衰弱前期；" [:br]
          "≥3 分：衰弱。"]]
        ]
       [:> Empty {:description "请先选择患者"}])]))


(defn pregnancy-assessment-card        "妊娠" []
  (let [pa-data @(rf/subscribe [::subs/pregnancy-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in pa-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:pregnancy_assessment] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        gestational-week-options [{:label "0-12 周" :value "0-12_weeks"} {:label "13-28 周" :value "13-28_weeks"} {:label ">28 周" :value ">28_weeks"}]
        comorbid-obstetric-options [{:label "单胎" :value "singleton_pregnancy"} {:label "多胎" :value "multiple_pregnancy"} {:label "妊娠期糖尿病" :value "gestational_diabetes"} {:label "妊娠期高血压" :value "gestational_hypertension"} {:label "周围型前置胎盘" :value "marginal_placenta_previa"} {:label "中央型前置胎盘" :value "complete_placenta_previa"} {:label "胎膜早破" :value "premature_rupture_of_membranes"} {:label "胎盘早剥" :value "placental_abruption"} {:label "胎盘植入" :value "placenta_accreta"} {:label "先兆流产" :value "threatened_abortion"} {:label "子痫前期" :value "preeclampsia"} {:label "子痫" :value "eclampsia"} {:label "其他" :value "other_obstetric_conditions"}]]
    [custom-styled-card
     [:> WomanOutlined {:style {:marginRight "8px"}}]
     "妊娠"
     "#fff0f6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js pa-data)
                 :key (str patient-id "-pregnancy-assessment")}
        [:> Form.Item {:label "是否妊娠" :name :is_pregnant}
         [:> Radio.Group {:value (get-val [:is_pregnant])
                          :onChange #(dispatch-update-event [:is_pregnant] %)}
          [:> Radio {:value "无"} "无"]
          [:> Radio {:value "有"} "有"]
          [:> Radio {:value "不祥"} "不祥"]]]
        (when (= (get-val [:is_pregnant]) "有")
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           [:> Form.Item {:label "孕周" :name :gestational_week}
            [:> Select {:placeholder "选择孕周" :style {:width "100%"} :allowClear true
                        :value (get-val [:gestational_week])
                        :options gestational-week-options
                        :onChange #(dispatch-update [:gestational_week] %)}]]
           [:> Form.Item {:label "孕产史" :name :obstetric_history}
            [:> Input.TextArea {:placeholder "例如：G2P1A1L1 或 足月1、早产0、流产1、存活1" :rows 2
                                :value (get-val [:obstetric_history])
                                :onChange #(dispatch-update-event [:obstetric_history] %)}]]
           [:> Form.Item {:label "合并产科情况" :name :comorbid_obstetric_conditions}
            [:> Checkbox.Group {:options comorbid-obstetric-options
                                :style {:display "flex" :flexDirection "column"}
                                :value (get-val [:comorbid_obstetric_conditions])
                                :onChange #(dispatch-update [:comorbid_obstetric_conditions] %)}]]
           (when (some #{"other_obstetric_conditions"} (get-val [:comorbid_obstetric_conditions]))
             [:> Form.Item {:label "其他合并产科情况详情" :name :comorbid_obstetric_conditions_other_details :style {:marginLeft "20px"}}
              [:> Input {:placeholder "请描述其他合并产科情况"
                         :value (get-val [:comorbid_obstetric_conditions_other_details])
                         :onChange #(dispatch-update-event [:comorbid_obstetric_conditions_other_details] %)}]])])
        [:> Form.Item {:label "其他妊娠相关情况" :name :other_pregnancy_conditions}
         [:> Input.TextArea {:placeholder "如有其他妊娠相关情况请在此注明" :rows 3
                             :value (get-val [:other_pregnancy_conditions])
                             :onChange #(dispatch-update-event [:other_pregnancy_conditions] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))


(defn surgical-anesthesia-history-card "手术麻醉史" []
  (let [sah-data @(rf/subscribe [::subs/surgical-anesthesia-history-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in sah-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:surgical_anesthesia_history] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))]
    (let [last-anesthesia-date-options [{:label ">5 年" :value ">5_years"} {:label "1-5 年" :value "1-5_years"} {:label "<1 年" :value "<1_year"}]
          anesthesia-type-options [{:label "全身麻醉" :value "general_anesthesia"} {:label "椎管内麻醉" :value "spinal_anesthesia"} {:label "神经阻滞" :value "nerve_block"} {:label "局部麻醉" :value "local_anesthesia"}]
          postop-complications-options [{:label "术后恶心呕吐" :value "postop_nausea_vomiting"} {:label "术后疼痛" :value "postop_pain"} {:label "声音嘶哑" :value "hoarseness"} {:label "头晕头疼" :value "dizziness_headache"} {:label "其他" :value "other_postop_complications"}]
          adverse-events-options [{:label "过敏反应" :value "allergic_reaction"} {:label "困难气道" :value "difficult_airway"} {:label "气管切开" :value "tracheostomy"} {:label "术中知晓" :value "intraop_awareness"} {:label "术后认知功能障碍" :value "postop_cognitive_dysfunction"} {:label "恶性高热" :value "malignant_hyperthermia"} {:label "其他" :value "other_adverse_events"}]]
      [custom-styled-card
       [:> HistoryOutlined {:style {:marginRight "8px"}}]
       "手术麻醉史"
       "#e6f7ff"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js sah-data)
                   :key (str patient-id "-surgical-anesthesia-history")}
          [:> Form.Item {:label "手术麻醉史" :name [:history :present]}
           [:> Radio.Group {:value (get-val [:history :present])
                            :onChange #(dispatch-update-event [:history :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]
            [:> Radio {:value "不祥"} "不祥"]]]
          (when (= (get-val [:history :present]) "有")
            [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
             [:> Form.Item {:label "上次麻醉日期范围" :name [:history :last_anesthesia_date_range]}
              [:> Select {:placeholder "选择范围" :style {:width "100%"} :allowClear true
                          :value (get-val [:history :last_anesthesia_date_range])
                          :options last-anesthesia-date-options
                          :onChange #(dispatch-update [:history :last_anesthesia_date_range] %)}]]
             [:> Form.Item {:label "具体上次麻醉日期 (可选)" :name [:history :last_anesthesia_date_specific]}
              [:> DatePicker {:style {:width "100%"} :placeholder "选择具体日期"
                              :value (when-let [date-str (get-val [:history :last_anesthesia_date_specific])] (dayjs date-str))
                              :onChange #(dispatch-update [:history :last_anesthesia_date_specific] (if % (utils/date->iso-string %) nil))}]]
             [:> Form.Item {:label "麻醉方式" :name [:history :anesthesia_types]}
              [:> Checkbox.Group {:options anesthesia-type-options
                                  :style {:display "flex" :flexDirection "column"}
                                  :value (get-val [:history :anesthesia_types])
                                  :onChange #(dispatch-update [:history :anesthesia_types] %)}]]
             [:> Form.Item {:label "术后并发症" :name [:history :postop_complications]}
              [:> Checkbox.Group {:options postop-complications-options
                                  :style {:display "flex" :flexDirection "column"}
                                  :value (get-val [:history :postop_complications])
                                  :onChange #(dispatch-update [:history :postop_complications] %)}]]
             (when (some #{"other_postop_complications"} (get-val [:history :postop_complications]))
               [:> Form.Item {:label "其他术后并发症详情" :name [:history :postop_complications_other_details] :style {:marginLeft "20px"}}
                [:> Input {:placeholder "请描述其他术后并发症"
                           :value (get-val [:history :postop_complications_other_details])
                           :onChange #(dispatch-update-event [:history :postop_complications_other_details] %)}]])
             [:> Form.Item {:label "不良事件" :name [:history :adverse_events]}
              [:> Checkbox.Group {:options adverse-events-options
                                  :style {:display "flex" :flexDirection "column"}
                                  :value (get-val [:history :adverse_events])
                                  :onChange #(dispatch-update [:history :adverse_events] %)}]]
             (when (some #{"other_adverse_events"} (get-val [:history :adverse_events]))
               [:> Form.Item {:label "其他不良事件详情" :name [:history :adverse_events_other_details] :style {:marginLeft "20px"}}
                [:> Input {:placeholder "请描述其他不良事件"
                           :value (get-val [:history :adverse_events_other_details])
                           :onChange #(dispatch-update-event [:history :adverse_events_other_details] %)}]])
             [:> Form.Item {:label "已行手术" :name [:history :previous_surgeries]}
              [:> Input.TextArea {:placeholder "请列出既往手术名称和日期" :rows 3
                                  :value (get-val [:history :previous_surgeries])
                                  :onChange #(dispatch-update-event [:history :previous_surgeries] %)}]]])
          [:> Form.Item {:label "有血缘关系的人发生过恶性高热史" :name :family_history_malignant_hyperthermia_present}
           [:> Radio.Group {:value (get-val [:family_history_malignant_hyperthermia :present])
                            :onChange #(dispatch-update-event [:family_history_malignant_hyperthermia :present] %)}
            [:> Radio {:value "无"} "无"]
            [:> Radio {:value "有"} "有"]]]
          (when (= (get-val [:family_history_malignant_hyperthermia :present]) "有")
            [:> Form.Item {:label "关系人" :name [:family_history_malignant_hyperthermia :relationship] :style {:marginLeft "20px"}}
             [:> Input {:placeholder "请说明与患者关系"
                        :value (get-val [:family_history_malignant_hyperthermia :relationship])
                        :onChange #(dispatch-update-event [:family_history_malignant_hyperthermia :relationship] %)}]])
          [:> Form.Item {:label "其他手术麻醉史相关情况" :name :other_surgical_anesthesia_conditions}
           [:> Input.TextArea {:placeholder "如有其他手术麻醉史相关情况请在此注明" :rows 3
                               :value (get-val [:other_surgical_anesthesia_conditions])
                               :onChange #(dispatch-update-event [:other_surgical_anesthesia_conditions] %)}]]
          ]
         [:> Empty {:description "请先选择患者"}])])))


(defn- airway-assessment-card        "气道评估" []
  (let [aa-data @(rf/subscribe [::subs/airway-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in aa-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:airway_assessment] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]
        yes-no-suspected-unknown-options [{:label "无" :value "无"} {:label "有" :value "有"} {:label "疑似" :value "疑似"} {:label "不祥" :value "不祥"}]
        mouth-opening-options [{:label "≥3横指" :value "gte_3_fingers"} {:label "2.5 横指" :value "2_5_fingers"} {:label "2 横指" :value "2_fingers"} {:label "<2 横指" :value "lt_2_fingers"} {:label "无法张口" :value "cannot_open"}]
        render-radio-with-details (fn [field-key-present field-key-details label-text]
                                    [:div {:key (str (name field-key-present) "-group")}
                                     [:> Form.Item {:label label-text :name [field-key-present]}
                                      [:> Radio.Group {:options yes-no-options
                                                       :value (get-val [field-key-present])
                                                       :onChange #(let [val (-> % .-target .-value)]
                                                                    (dispatch-update [field-key-present] val)
                                                                    (when (= val "无") (dispatch-update [field-key-details] nil)))}]]
                                     (when (= (get-val [field-key-present]) "有")
                                       [:> Form.Item {:label "详情" :name [field-key-details] :style {:marginLeft "20px"}}
                                        [:> Input.TextArea {:placeholder "请描述详情" :rows 2
                                                            :value (get-val [field-key-details])
                                                            :onChange #(dispatch-update-event [field-key-details] %)}]])])]
    [custom-styled-card
     [:> NodeIndexOutlined {:style {:marginRight "8px"}}]
     "气道评估"
     "#fff7e6"
     (if patient-id
       [:> Form {:layout "vertical"
                 :initialValues (clj->js aa-data)
                 :key (str patient-id "-airway-assessment")}
        [:p {:style {:fontStyle "italic" :color "gray"}} "参考甲颏距离及Mallampati分级图示 (图示待添加)"]
        [:h4 {:style {:marginTop "16px"}} "详细评估项"]
        [:> Form.Item {:label "既往困难通气史" :name [:detailed_assessment :difficult_ventilation_history]}
         [:> Radio.Group {:options yes-no-suspected-unknown-options :value (get-val [:detailed_assessment :difficult_ventilation_history]) :onChange #(dispatch-update-event [:detailed_assessment :difficult_ventilation_history] %)}]]
        [:> Form.Item {:label "既往困难插管史" :name [:detailed_assessment :difficult_intubation_history]}
         [:> Radio.Group {:options yes-no-suspected-unknown-options :value (get-val [:detailed_assessment :difficult_intubation_history]) :onChange #(dispatch-update-event [:detailed_assessment :difficult_intubation_history] %)}]]
        [:> Form.Item {:label "张口度" :name [:detailed_assessment :mouth_opening :degree]}
         [:> Select {:placeholder "选择张口度" :style {:width "100%"} :allowClear true
                     :value (get-val [:detailed_assessment :mouth_opening :degree])
                     :options mouth-opening-options
                     :onChange #(dispatch-update [:detailed_assessment :mouth_opening :degree] %)}]]
        ;; ... (rest of airway card including special_facial_features, snoring, airway_related_diseases, mediastinal_history, current_airway_symptoms etc.)
        [:> Form.Item {:label "其他气道相关情况" :name :other_airway_conditions}
         [:> Input.TextArea {:placeholder "如有其他气道相关情况请在此注明" :rows 3
                             :value (get-val [:other_airway_conditions])
                             :onChange #(dispatch-update-event [:other_airway_conditions] %)}]]
        ]
       [:> Empty {:description "请先选择患者"}])]))


(defn spinal-anesthesia-assessment-card        "椎管内麻醉相关评估" []
  (let [saa-data @(rf/subscribe [::subs/spinal-anesthesia-assessment-data])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        get-val (fn [path] (get-in saa-data path))
        dispatch-update (fn [path value] (rf/dispatch [::events/update-canonical-assessment-field (into [:spinal_anesthesia_assessment] path) value]))
        dispatch-update-event (fn [path event] (dispatch-update path (-> event .-target .-value)))
        yes-no-options [{:label "无" :value "无"} {:label "有" :value "有"}]]
    (letfn [(render-radio-group [main-key item-key label-text] ; Specific helper
              [:> Form.Item {:label label-text :name [main-key item-key] :key (str (name main-key) "-" (name item-key))}
               [:> Radio.Group {:options yes-no-options
                                :value (get-val [main-key item-key])
                                :onChange #(dispatch-update-event [main-key item-key] %)}]])
            (render-subsection-title [title] ; Specific helper
              [:h4 {:style {:marginTop "16px" :marginBottom "8px" :borderBottom "1px solid #f0f0f0" :paddingBottom "4px"}} title])]
      [custom-styled-card
       [:> GatewayOutlined {:style {:marginRight "8px"}}]
       "椎管内麻醉相关评估"
       "#f0f5ff"
       (if patient-id
         [:> Form {:layout "vertical"
                   :initialValues (clj->js saa-data)
                   :key (str patient-id "-spinal-anesthesia")}
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
          [:> Form.Item {:label "腰椎间盘突出" :name [:lumbar_disc_herniation :present] :key "ldh-present"}
           [:> Radio.Group {:options yes-no-options
                            :value (get-val [:lumbar_disc_herniation :present])
                            :onChange #(let [val (-> % .-target .-value)]
                                         (dispatch-update [:lumbar_disc_herniation :present] val)
                                         (when (= val "无")
                                           (dispatch-update [:lumbar_disc_herniation :lower_limb_numbness_symptoms] nil)))}]]
          (when (= (get-val [:lumbar_disc_herniation :present]) "有")
            [:div {:style {:marginLeft "20px"}}
             [:> Form.Item {:label "下肢麻木症状" :name [:lumbar_disc_herniation :lower_limb_numbness_symptoms] :key "ldh-symptoms"}
              [:> Radio.Group {:options yes-no-options
                               :value (get-val [:lumbar_disc_herniation :lower_limb_numbness_symptoms])
                               :onChange #(dispatch-update-event [:lumbar_disc_herniation :lower_limb_numbness_symptoms] %)}]]])
          (render-subsection-title "心血管系统")
          (render-radio-group :cardiovascular_system :aortic_stenosis "主动脉瓣狭窄")
          (render-radio-group :cardiovascular_system :hypertrophic_obstructive_cardiomyopathy "肥厚型梗阻型心肌病")
          [:> Form.Item {:label "抗凝 / 抗血小板药物" :name [:cardiovascular_system :anticoagulants_present] :key "cvs-anticoag-present"}
           [:> Radio.Group {:options yes-no-options
                            :value (get-val [:cardiovascular_system :anticoagulants_present])
                            :onChange #(let [val (-> % .-target .-value)]
                                         (dispatch-update [:cardiovascular_system :anticoagulants_present] val)
                                         (when (= val "无")
                                           (dispatch-update [:cardiovascular_system :anticoagulants_details] nil)))}]]
          (when (= (get-val [:cardiovascular_system :anticoagulants_present]) "有")
            [:div {:style {:marginLeft "20px"}}
             [:> Form.Item {:label "详情" :name [:cardiovascular_system :anticoagulants_details] :key "cvs-anticoag-details"}
              [:> Input.TextArea {:placeholder "请描述药物名称、剂量、频率、末次用药时间" :rows 2
                                  :value (get-val [:cardiovascular_system :anticoagulants_details])
                                  :onChange #(dispatch-update-event [:cardiovascular_system :anticoagulants_details] %)}]]])
          (render-subsection-title "穿刺点检查")
          (render-radio-group :puncture_site_inspection :difficult_puncture_history "既往穿刺困难史")
          (render-radio-group :puncture_site_inspection :local_infection "局部感染")
          (render-radio-group :puncture_site_inspection :deformity "畸形")
          (render-subsection-title "局麻药过敏")
          [:> Form.Item {:label "局麻药过敏史" :name :local_anesthetic_allergy :key "la-allergy"}
           [:> Radio.Group {:options yes-no-options
                            :value (get-val [:local_anesthetic_allergy])
                            :onChange #(dispatch-update-event [:local_anesthetic_allergy] %)}]]
          ]
         [:> Empty {:description "请先选择患者"}])])))
