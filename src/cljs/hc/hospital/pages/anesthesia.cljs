(ns hc.hospital.pages.anesthesia
  "麻醉管理, 医生补充患者自己填写的评估报告, 最终评估患者的情况, 判断是否可以麻醉"
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]
            [hc.hospital.utils :as utils]
            [taoensso.timbre :as timbre]
            [clojure.string :as str]
            ["@ant-design/icons" :as icons :refer [SyncOutlined QrcodeOutlined]]
            [hc.hospital.components.form-components :as form-comp]
            ["antd" :refer [Collapse
                            Form  Descriptions Empty Affix Tooltip Button Input Select]]))

(def collapse-panel (r/adapt-react-class (.-Panel Collapse)))

(defn patient-list-filters []
  (let [search-term @(rf/subscribe [::subs/search-term])
        date-range @(rf/subscribe [::subs/date-range])
        assessment-status @(rf/subscribe [::subs/assessment-status-filter])
        assessment-status-options [{:value "all" :label "全部状态"}
                                   {:value "待评估" :label "待评估"}
                                   {:value "已批准" :label "已批准"}
                                   {:value "已驳回" :label "已驳回"}
                                   {:value "已暂缓" :label "已暂缓"}]]
    [:div {:style {:padding "16px" :borderBottom "1px solid #f0f0f0"}}
     ;; 按钮组
     [antd/space {:style {:marginBottom "16px" :width "100%"}}
      [antd/button {:type "primary"
                    :icon (r/as-element [:> SyncOutlined])
                    :onClick #(rf/dispatch [::events/sync-applications]) ; 您需要定义此事件
                    :style {:display "flex" :alignItems "center"}}
       "同步申请"]
      [antd/button {:icon (r/as-element [:> QrcodeOutlined])
                    :onClick #(rf/dispatch [::events/scan-check-in]) ; 您需要定义此事件
                    :style {:display "flex" :alignItems "center"}}
       "扫码签到"]]

     ;; 申请日期
     [:div {:style {:marginBottom "8px" :color "#666"}} "申请日期:"]
     [antd/range-picker
      {:style {:width "100%" :marginBottom "12px"}
       :value date-range
       :onChange #(rf/dispatch [::events/set-date-range %])}]

     ;; 评估状态
     [antd/select
      {:style {:width "100%" :marginBottom "16px"}
       :placeholder "评估状态: 请选择"
       :value (if (= assessment-status "all") nil assessment-status) ; "all" 时显示 placeholder
       :allowClear true
       :onChange #(rf/dispatch [::events/set-assessment-status-filter (or % "all")])
       :options assessment-status-options}]

     ;; 搜索框
     [antd/input-search
      {:placeholder "请输入患者姓名/门诊号"
       :allowClear true
       :value search-term
       :onChange #(rf/dispatch [::events/update-search-term (-> % .-target .-value)])
       :onSearch #(rf/dispatch [::events/search-patients %])}]]))

(defn patient-list []
  (let [patients @(rf/subscribe [::subs/filtered-patients])
        current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    [:div {:style {:height "100%" :overflowY "auto"}} ; Outer :div vector starts
     (if (seq patients)
       (for [item patients]
         ^{:key (:key item)}
         [:div {:style {:padding "10px 12px"
                        :borderBottom "1px solid #f0f0f0"
                        :display "flex"
                        :justifyContent "space-between"
                        :alignItems "center"
                        :background (when (= (:key item) current-patient-id) "#e6f7ff")
                        :cursor "pointer"}
                :onClick #(rf/dispatch [::events/select-patient (:key item)])}
          [:div {:style {:display "flex" :alignItems "center"}}
           [:> icons/UserOutlined {:style {:marginRight "8px" :fontSize "16px"}}]
           [:div
            [:div {:style {:fontWeight "500"}} (:name item)]
            [:div {:style {:fontSize "12px" :color "gray"}}
             (str (:sex item) " " (:age item) " " (:anesthesia-type item))]]]
          [:div {:style {:textAlign "right"}}
           [:div {:style {:fontSize "12px" :color "gray" :marginBottom "4px"}} (:date item)]
           [antd/tag {:color (case (:status item)
                               "待评估" "orange"
                               "已批准" "green"
                               "已暂缓" "blue"
                               "已驳回" "red"
                               "default")} (:status item)]]])

       [:> Empty {:description "暂无患者数据" :style {:marginTop "40px"}}])
     ]))


(defn patient-list-panel []
  [:<>
   [patient-list-filters]
   [patient-list]])

(defn brief-medical-history []
  (let [medical-history @(rf/subscribe [::subs/doctor-form-brief-medical-history])
        [form] ((.-useForm Form))]
    (when medical-history               ; 确保数据存在才渲染表单
      [antd/form {:form form
                  :layout "vertical"
                  :initialValues medical-history
                  :onValuesChange (fn [_changed-values all-values]
                                    (rf/dispatch [::events/update-brief-medical-history all-values]))
                  :style {:paddingBottom "24px"}}

       ;; 既往史
       [form-comp/yes-no-with-description
        {:form form :label "既往史" :name-yes-no [:past-history :yes-no] :name-description [:past-history :description]}]
       ;; 过敏史
       [form-comp/yes-no-with-description
        {:form form :label "过敏史" :name-yes-no [:allergic-history :yes-no] :name-description [:allergic-history :description]}]
       ;; 手术麻醉史
       [form-comp/yes-no-with-description
        {:form form :label "手术麻醉史" :name-yes-no [:surgery-anesthesia-history :yes-no] :name-description [:surgery-anesthesia-history :description]}]
       ;; 怀孕
       [form-comp/yes-no-with-description
        {:form form :label "怀孕" :name-yes-no [:pregnancy :yes-no] :name-description [:pregnancy :description]}]
       ;; 输血史
       [form-comp/yes-no-with-description
        {:form form :label "输血史" :name-yes-no [:blood-transfusion-history :yes-no] :name-description [:blood-transfusion-history :description]}]
       ;; 月经期
       [form-comp/yes-no-with-description
        {:form form :label "月经期" :name-yes-no [:menstrual-period :yes-no] :name-description [:menstrual-period :description]}]

       ;; 个人史 (Personal History) - Checkbox Group
       [antd/form-item {:name :personal-history
                        :label "个人史:"}
        [antd/checkbox-group {}
         [antd/checkbox {:value "smoke"} "烟"]
         [antd/checkbox {:value "drink"} "酒"]]]

       ;; 其他 (Other)
       [form-comp/checkbox-with-conditional-input
        {:form form :label "其他" :name-checkbox [:other :checkbox] :name-input [:other :description]
         :checkbox-label "有其他情况需要说明" :input-placeholder "请描述其他情况详情"}]])))


(defn physical-examination []
  (let [exam-data @(rf/subscribe [::subs/doctor-form-physical-examination])
        [form] ((.-useForm Form))]
    (when exam-data
      [antd/form {:form form :layout "horizontal"
                  :labelCol {:span 6 :style {:textAlign "left"}}
                  :wrapperCol {:span 18}
                  :style {:paddingBottom "24px" :maxWidth "800px" :marginLeft "0"}
                  :initialValues exam-data
                  :onValuesChange (fn [_ all-values] (rf/dispatch [::events/update-physical-examination all-values]))}

       [form-comp/radio-button-group
        {:label "一般状况" :name :general-condition
         :options [{:value "bad" :label "差"} {:value "fair" :label "尚可"} {:value "average" :label "一般"} {:value "good" :label "好"}]}]
       [antd/row {:gutter 16}
        [antd/col {:span 12} [antd/form-item {:name :height :label "身高"} [form-comp/number-input-with-unit {:unit "cm"}]]]
        [antd/col {:span 12} [antd/form-item {:name :weight :label "体重"} [form-comp/number-input-with-unit {:unit "kg"}]]]]
       [antd/form-item {:label "BP:"}
        [Input.Group {:compact true}
         [antd/form-item {:name [:bp :systolic] :noStyle true} [antd/input {:style {:width "80px"} :type "number"}]]
         [:span {:style {:display "inline-block" :width "30px" :lineHeight "32px" :textAlign "center"}} "/"]
         [antd/form-item {:name [:bp :diastolic] :noStyle true} [antd/input {:style {:width "80px"} :type "number"}]]
         [:span {:style {:marginLeft "8px"}} "mmHg"]]]
       [antd/row {:gutter 16}
        [antd/col {:span 12} [antd/form-item {:name :heart-rate :label "PR"} [form-comp/number-input-with-unit {:unit "次/分"}]]]
        [antd/col {:span 12} [antd/form-item {:name :respiratory-rate :label "RR"} [form-comp/number-input-with-unit {:unit "次/分"}]]]]
       [antd/form-item {:name :temperature :label "T"} [form-comp/number-input-with-unit {:step "0.1" :unit "°C"}]]
       [form-comp/radio-button-group
        {:label "精神行为" :name :mental-state
         :options [{:value "normal" :label "正常"} {:value "drowsy" :label "嗜睡"} {:value "coma" :label "昏迷"} {:value "irritable" :label "烦躁"} {:value "delirium" :label "谵妄"} {:value "cognitive" :label "认知障碍"}]}]
       [form-comp/radio-button-group
        {:label "头颈部" :name :head-neck
         :options [{:value "normal" :label "无异常"} {:value "scar" :label "疤痕"} {:value "short_neck" :label "颈短"} {:value "neck_mass" :label "颈部肿块"} {:value "limited_mobility" :label "后仰困难"}]}]
       [antd/form-item {:name :mouth-opening :label "口腔: 张口"} [form-comp/number-input-with-unit {:step "0.1" :unit "cm"}]]
       [antd/row {:gutter 16}
        [antd/col {:span 12}
         [antd/form-item {:name :mallampati-score :label "Mallampati"}
          [form-comp/radio-button-group {:name :mallampati-score :options [{:value "I" :label "I"} {:value "II" :label "II"} {:value "III" :label "III"} {:value "IV" :label "IV"}]}]]]
        [antd/col {:span 12} [antd/form-item {:name :thyromental-distance :label "颏颌距离"} [form-comp/number-input-with-unit {:step "0.1" :unit "cm"}]]]]
       [antd/form-item {:label "相关病史:"}
        [antd/space {:direction "vertical"}
         [antd/form-item {:name [:related-history :difficult-airway] :valuePropName "checked" :noStyle true} [antd/checkbox "困难气道史"]]
         [antd/form-item {:name [:related-history :postoperative-nausea] :valuePropName "checked" :noStyle true} [antd/checkbox "术后恶心呕吐史"]]
         [antd/form-item {:name [:related-history :malignant-hyperthermia] :valuePropName "checked" :noStyle true} [antd/checkbox "恶性高热史"]]
         [form-comp/checkbox-with-conditional-input {:form form :name-checkbox [:related-history :other-checkbox] :name-input [:related-history :other] :checkbox-label "其他" :input-placeholder "请输入其他相关病史"}]]]
       [form-comp/radio-button-group
        {:label "胸" :name :chest
         :options [{:value "normal" :label "正常"} {:value "barrel" :label "桶状胸"} {:value "pectus_excavatum" :label "佝偻胸"}]}]])))

(defn lab-tests []
  (let [lab-data @(rf/subscribe [::subs/doctor-form-lab-tests])
        [form] ((.-useForm Form))]
    (when lab-data
      [antd/form {:form form :layout "vertical" :initialValues lab-data
                  :onValuesChange (fn [_ all-values] (rf/dispatch [::events/update-lab-tests all-values]))
                  :style {:paddingBottom "24px"}}
       [form-comp/section-title {:title "血常规：" :margin-top "0"}]
       [form-comp/two-column-row
        {:left-item [form-comp/number-input-with-unit {:label "HGB" :name [:complete-blood-count :hemoglobin] :unit "g/L"}] ; HGB not RBC
         :right-item [form-comp/number-input-with-unit {:label "Hct" :name [:complete-blood-count :hematocrit] :step "0.01" :unit "%"}]}]
       [form-comp/two-column-row
        {:left-item [form-comp/number-input-with-unit {:label "PLT" :name [:complete-blood-count :platelets] :unit "×10⁹/L"}]
         :right-item [form-comp/number-input-with-unit {:label "WBC" :name [:complete-blood-count :wbc] :step "0.1" :unit "×10⁹/L"}]}]
       [form-comp/two-column-row
        {:left-item [form-comp/radio-button-group {:label "血型" :name :blood-type :options [{:value "A" :label "A"} {:value "B" :label "B"} {:value "AB" :label "AB"} {:value "O" :label "O"}]}]
         :right-item [form-comp/radio-button-group {:label "Rh" :name :rh :options [{:value "negative" :label "阴性"} {:value "positive" :label "阳性"}]}]}]
       [form-comp/two-column-row
        {:left-item [form-comp/radio-button-group {:label "凝血检查" :name :coagulation :options [{:value "normal" :label "正常"} {:value "abnormal" :label "异常"}]}]
         :right-item [form-comp/number-input-with-unit {:label "血糖值" :name [:biochemistry :glucose] :step "0.1" :unit "mmol/L"}]}]
       [form-comp/section-title {:title "生化指标："}]
       [form-comp/two-column-row
        {:left-item [form-comp/number-input-with-unit {:label "ALT" :name [:biochemistry :alt] :unit "U/L"}]
         :right-item [form-comp/number-input-with-unit {:label "AST" :name [:biochemistry :ast] :unit "U/L"}]}]
       [form-comp/two-column-row
        {:left-item [form-comp/number-input-with-unit {:label "钠" :name [:biochemistry :sodium] :unit "mmol/L"}]
         :right-item [form-comp/number-input-with-unit {:label "钾" :name [:biochemistry :potassium] :step "0.1" :unit "mmol/L"}]}]
       [form-comp/section-title {:title "医学影像："}]
       [form-comp/limited-text-input {:name :ecg :label "心电图" :placeholder "请输入心电图检查结果"}]
       [form-comp/limited-text-input {:name :chest-xray :label "胸片" :placeholder "请输入胸片检查结果"}]])))


;; 辅助函数，用于显示患者基本信息
(defn- patient-info-display []
  (let [patient-details @(rf/subscribe [::subs/selected-patient-raw-details])
        basic-info (or (:basic-info patient-details) ; API data might have basic_info nested
                       patient-details)] ; Example data might be flat
    (if (seq basic-info) ; Check if basic-info has keys
      [:> Descriptions {:bordered true :column 4 :size "small"}
       [:> Descriptions.Item {:label "门诊号"} (or (:outpatient-number basic-info) (:patient-id-display basic-info) "N/A")]
       [:> Descriptions.Item {:label "姓名"} (or (:name basic-info) "N/A")]
       [:> Descriptions.Item {:label "性别"} (or (:sex basic-info) "N/A")]
       [:> Descriptions.Item {:label "年龄"} (if-let [age (:age basic-info)] (str age "岁") "N/A")]
       [:> Descriptions.Item {:label "病区" :span 2} (or (:department basic-info) "N/A")]
       [:> Descriptions.Item {:label "电子健康卡号" :span 2} (or (:health-card-number basic-info) "暂无")]
       [:> Descriptions.Item {:label "术前诊断" :span 4} (or (:diagnosis basic-info) "N/A")]
       [:> Descriptions.Item {:label "拟施手术" :span 4} (or (:planned-surgery basic-info) (:type basic-info) "N/A")]]
      [:> Empty {:description "暂无患者信息"}])))

;; 辅助函数，用于显示一般情况
(defn- general-condition-display []
  (let [exam-data @(rf/subscribe [::subs/doctor-form-physical-examination])]
    (if (seq exam-data) ; 检查 exam-data 是否有内容
      [:> Descriptions {:bordered true :column 2 :size "small"}
       [:> Descriptions.Item {:label "身高"} (str (:height exam-data "N/A") " cm")]
       [:> Descriptions.Item {:label "体重"} (str (:weight exam-data "N/A") " kg")]
       [:> Descriptions.Item {:label "精神状态"} (utils/display-value (:mental-state exam-data))]
       [:> Descriptions.Item {:label "活动能力"} (utils/display-value (get exam-data :activity-level))] ; 需要在 db 和 subs 中添加 :activity-level
       [:> Descriptions.Item {:label "血压 (BP)"} (str (get-in exam-data [:bp :systolic] "N/A") "/" (get-in exam-data [:bp :diastolic] "N/A") " mmHg")]
       [:> Descriptions.Item {:label "脉搏 (PR)"} (str (:heart-rate exam-data "N/A") " 次/分")]
       [:> Descriptions.Item {:label "呼吸 (RR)"} (str (:respiratory-rate exam-data "N/A") " 次/分")]
       [:> Descriptions.Item {:label "体温 (T)"} (str (:temperature exam-data "N/A") " °C")]
       [:> Descriptions.Item {:label "SpO2"} (utils/display-value (get exam-data :spo2))]] ; 需要在 db 和 subs 中添加 :spo2
      [:> Empty {:description "暂无一般情况信息"}])))


(defn- medical-summary-display []
  (let [current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    (if current-patient-id
      [:div
       [:> Collapse {:defaultActiveKey ["1"] :accordion false} ; accordion false 允许多个展开
        [collapse-panel {:header "简要病史" :key "1"}
         [:f> brief-medical-history]]
        [collapse-panel {:header "体格检查" :key "2"}
         [:f> physical-examination]]
        [collapse-panel {:header "实验室检查" :key "3"}
         [:f> lab-tests]]
        [collapse-panel {:header "麻醉计划摘要" :key "4"}
         (let [plan-details @(rf/subscribe [::subs/anesthesia-plan-details])]
           (if (seq plan-details)
             [:> Descriptions {:bordered true :column 1 :size "small"}
              [:> Descriptions.Item {:label "ASA分级"} (utils/display-value (get-in plan-details [:risk-assessment :asa-grade]))]
              [:> Descriptions.Item {:label "拟行麻醉方案"} (utils/display-value (:anesthesia-type plan-details))]
              [:> Descriptions.Item {:label "监测项目"} (utils/display-list (get-in plan-details [:monitoring :items]))]
              [:> Descriptions.Item {:label "特殊技术"} (utils/display-list (:special-considerations plan-details))]]
             [:> Empty {:description "暂无麻醉计划摘要"}]))]]]
      [:> Empty {:description "请先选择一位患者以查看病情摘要" :style {:paddingTop "20px"}}])))


;; 辅助函数，用于显示术前麻醉医嘱
(defn- preoperative-orders-display []
  (let [plan-details @(rf/subscribe [::subs/anesthesia-plan-details])]
    (if (seq plan-details)
      [:> Descriptions {:bordered true :column 1 :size "small"}
       [:> Descriptions.Item {:label "术前用药医嘱"} (utils/display-list (get-in plan-details [:medications :premedication]))]
       [:> Descriptions.Item {:label "术日晨继续应用药物"} (utils/display-value (get-in plan-details [:medications :continue-on-surgery-day]))] ; 需要添加
       [:> Descriptions.Item {:label "需进一步检查"} (utils/display-value (get-in plan-details [:further-checks-needed]))] ; 需要添加
       [:> Descriptions.Item {:label "术时麻醉注意事项"} (utils/display-value (get-in plan-details [:intraoperative-notes]))]] ; 需要添加
      [:> Empty {:description "暂无术前麻醉医嘱"}])))

;; 辅助函数，用于显示签名和日期
(defn- signature-and-date-display []
  (let [patient-details @(rf/subscribe [::subs/selected-patient-raw-details])
        assessment-date (or (get-in patient-details [:assessment_data :assessment-date]) ; 尝试从评估数据中获取
                            (:updated_at patient-details) ; API 患者对象的更新时间
                            (js/Date.now))] ; 最后回退到当前时间
    [:> Descriptions {:bordered true :column 2 :size "small"}
     [:> Descriptions.Item {:label "麻醉医师签名"} [:input {:type "text" :class "ant-input ant-input-sm" :placeholder "在此签名或记录医师姓名"}]]
     [:> Descriptions.Item {:label "评估日期"} (utils/format-date assessment-date "YYYY-MM-DD HH:mm")]]))

;; 辅助函数，用于显示备注信息
(defn- remarks-display []
  (let [notes @(rf/subscribe [::subs/assessment-notes])]
    [antd/text-area {:rows 3
                     :value (or notes "")
                     :placeholder "备注信息（如有特殊情况请在此注明）"
                     :onChange (fn [e] (rf/dispatch [::events/update-assessment-notes (.. e -target -value)]))}]))

(defn assessment-result []
  (let [current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    [antd/card {:title "麻醉评估总览"
                :variant "borderless"
                :style {:height "calc(100vh - 120px)" ; 调整高度以适应顶部栏和底部按钮
                        :overflowY "auto"
                        :padding "0 8px"}}
     (if current-patient-id
       [:<>
        [antd/card {:title (r/as-element [:span [:> icons/UserOutlined {:style {:marginRight "8px"}}] "患者信息"])
                    :type "inner" :style {:marginBottom "12px"}}
         [patient-info-display]]

        [antd/card {:title (r/as-element [:span [:> icons/HeartOutlined {:style {:marginRight "8px"}}] "一般情况"])
                    :type "inner" :style {:marginBottom "12px"}}
         [general-condition-display]]

        [antd/card {:title (r/as-element [:span [:> icons/FileTextOutlined {:style {:marginRight "8px"}}] "病情摘要（病史、体检及辅助检查）"])
                    :type "inner" :style {:marginBottom "12px"}}
         [medical-summary-display]]

        [antd/card {:title (r/as-element [:span [:> icons/ExperimentOutlined {:style {:marginRight "8px"}}] "术前麻醉医嘱"])
                    :type "inner" :style {:marginBottom "12px"}}
         [preoperative-orders-display]]

        [antd/card {:title (r/as-element [:span [:> icons/EditOutlined {:style {:marginRight "8px"}}] "麻醉医师签名及日期"])
                    :type "inner" :style {:marginBottom "12px"}}
         [signature-and-date-display]]

        [antd/card {:title (r/as-element [:span [:> icons/MessageOutlined {:style {:marginRight "8px"}}] "备注信息"])
                    :type "inner" :style {:marginBottom "12px"}}
         [remarks-display]]

        [:> Affix {:offsetBottom 10 :style {:textAlign "center" :padding "10px 0" :background "white" :borderTop "1px solid #f0f0f0"}}
         [:> Button {:type "primary"
                     :size "large"
                     :icon (r/as-element [:> icons/SaveOutlined])
                     :on-click #(rf/dispatch [::events/save-final-assessment])}
          "保存评估结果"]]]
       [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100%"}}
        [:> Empty {:description "请从左侧选择一位患者开始评估" :imageStyle {:height 80}}]])]))


(defn anesthesia-content []
  [antd/content {:style {:padding "0" :margin 0 :minHeight 280 :overflow "hidden" :display "flex" :flexDirection "row"}}
   ;; 左侧患者列表区域
   [antd/card {:style {:width "350px"
                       :minWidth "300px"
                       :height "calc(100vh - 64px)" ; 假设顶部导航栏高度为 64px
                       :borderRight "1px solid #f0f0f0"
                       :display "flex"
                       :flexDirection "column"
                       :padding "0"}
               :bodyStyle {:padding "0" :flexGrow 1 :overflow "hidden" :display "flex" :flexDirection "column"}}

    ;; 患者列表主体
    [:div {:style {:flexGrow 1 :overflowY "auto"}}
     [patient-list-panel]]]

   ;; 右侧评估详情区域
   [:div {:style {:flexGrow 1 :height "calc(100vh - 64px)" :overflowY "auto" :background "#f0f2f5" :padding "10px"}}
    [assessment-result]]])
