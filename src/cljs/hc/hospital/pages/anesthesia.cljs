(ns hc.hospital.pages.anesthesia
  "麻醉管理, 医生补充患者自己填写的评估报告, 最终评估患者的情况, 判断是否可以麻醉"
  (:require ; Added Image, Modal
   ;; 确保 antd/Form 等组件已引入
   ["dayjs" :as dayjs]
   ["@ant-design/icons" :as icons :refer [FileTextOutlined MedicineBoxOutlined
                                          ProfileOutlined QrcodeOutlined
                                          SolutionOutlined SyncOutlined
                                          CheckCircleOutlined ClockCircleOutlined CloseCircleOutlined PrinterOutlined]]
   ["antd" :refer [Button Card Col DatePicker Descriptions Empty Form
                   Input InputNumber Layout Modal Radio Row Select Space Tag
                   Upload]] ; Removed Tooltip as it's not used
   [hc.hospital.events :as events]
   [taoensso.timbre :as timbre]
   [hc.hospital.subs :as subs]
   [hc.hospital.utils :as utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))


(defn patient-list-filters []
  (let [date-range @(rf/subscribe [::subs/date-range])
        assessment-status-options [{:value "all" :label "全部状态"}
                                   {:value "待评估" :label "待评估"}
                                   {:value "已批准" :label "已批准"}
                                   {:value "已驳回" :label "已驳回"}
                                   {:value "已暂缓" :label "已暂缓"}]]
    [:div {:style {:padding "16px" :borderBottom "1px solid #f0f0f0"}}
     ;; 按钮组
     [:> Space {:style {:marginBottom "16px" :width "100%"}}
      [:> Button {:type "primary"
                  :icon (r/as-element [:> SyncOutlined])
                  :onClick #(rf/dispatch [::events/sync-applications]) ; 您需要定义此事件
                  :style {:display "flex" :alignItems "center"}}
       "同步申请"]
      [:> Button {:icon (r/as-element [:> QrcodeOutlined])
                  :onClick #(rf/dispatch [::events/scan-check-in]) ; 您需要定义此事件
                  :style {:display "flex" :alignItems "center"}}
       "扫码签到"]]

     ;; 申请日期
     [:div {:style {:marginBottom "8px" :color "#666"}} "申请日期:"]
     [:> DatePicker.RangePicker
      {:style {:width "100%" :marginBottom "12px"}
       :value date-range
       :onChange #(rf/dispatch [::events/set-date-range %])}]

     ;; 评估状态
     [:> Select
      {:style {:width "100%" :marginBottom "16px"}
       :placeholder "评估状态: 请选择"
       :options assessment-status-options}]

     ;; 搜索框
     [:> Input.Search
      {:placeholder "请输入患者姓名/门诊号"
       :allowClear true
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
             (str (:gender item) " " (:age item) " " (:anesthesia-type item))]]]
          [:div {:style {:textAlign "right"}}
           [:div {:style {:fontSize "12px" :color "gray" :marginBottom "4px"}} (:date item)]
           [:> Tag {:color (case (:status item)
                             "待评估" "orange"
                             "已批准" "green"
                             "已暂缓" "blue"
                             "已驳回" "red"
                             "default")} (:status item)]]])
       [:> Empty {:description "暂无患者数据" :style {:marginTop "40px"}}])]))


(defn patient-list-panel []
  [:<>
   [patient-list-filters]
   [patient-list]])

(defn- custom-styled-card "创建统一样式的卡片"[icon title-text header-bg-color content]
  [:> Card {:title (r/as-element [:span icon title-text])
            :styles {:header {:background header-bg-color}
                     :body {:background "#ffffff"}} ; 确保内容区域背景为白色
            :type "inner"
            :style {:marginBottom "12px"}}
   content])

(defn- patient-info-card "显示患者基本信息" []
  (let [basic-info @(rf/subscribe [::subs/canonical-basic-info])] ; Use new subscription
    [custom-styled-card
     [:> icons/UserOutlined {:style {:marginRight "8px"}}]
     "患者基本信息"
     "#e6fffb" ; Header background color
     (if (seq basic-info) ; Check if basic-info map is not empty
       [:> Form {:layout "horizontal" :labelCol {:span 8} :wrapperCol {:span 16} :labelAlign "left"
                 :initialValues (clj->js basic-info)
                 ;; Add a key to force re-render when patient changes, ensuring initialValues are applied
                 :key (get basic-info :outpatient_number)}
        [:div {:style {:display "grid"
                       :gridTemplateColumns "repeat(4, 1fr)" ; 4 列
                       :gap "0px 16px"}} ; 列间距

         [:> Form.Item {:label "门诊号" :name :outpatient_number ; Matches canonical
                        :rules [{:required true :message "请输入门诊号!"}]}
          [:> Input {:placeholder "请输入门诊号"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :outpatient_number] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "姓名" :name :name} ; Matches canonical
          [:> Input {:placeholder "请输入姓名"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :name] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "性别" :name :gender} ; Matches canonical
          [:> Select {:placeholder "请选择性别"
                      :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :gender] %])
                      :options [{:value "male" :label "男"}
                                {:value "female" :label "女"}
                                {:value "其他" :label "其他"}]}]]

         [:> Form.Item {:label "年龄" :name :age} ; Matches canonical
          [:> InputNumber {:placeholder "岁"
                           :min 0
                           :style {:width "100%"}
                           :addonAfter "岁"
                           :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :age] %])}]]

         [:> Form.Item {:label "病区" :name :department ; Matches canonical
                        :style {:gridColumn "span 2"}}
          [:> Input {:placeholder "请输入病区"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :department] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "电子健康卡号" :name :health_card_number ; Matches canonical
                        :style {:gridColumn "span 2"}}
          [:> Input {:placeholder "请输入电子健康卡号 (可选)"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :health_card_number] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "术前诊断" :name :diagnosis ; Matches canonical
                        :style {:gridColumn "span 4"}}
          [:> Input.TextArea {:placeholder "请输入术前诊断"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :diagnosis] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "拟施手术" :name :planned_surgery ; Matches canonical
                        :style {:gridColumn "span 4"}}
          [:> Input.TextArea {:placeholder "请输入拟施手术"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :planned_surgery] (-> % .-target .-value)])}]]]]
       [:> Empty {:description "请先选择患者或患者无基本信息"}])])) ; Updated empty message

(defn- general-condition-card "显示一般情况" []
  (let [physical-exam-data @(rf/subscribe [::subs/canonical-physical-examination]) ; Use new subscription
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    ;; 定义选项数据
    (let [mental-status-options [{:value "清醒" :label "清醒"}
                                 {:value "嗜睡" :label "嗜睡"}
                                 {:value "模糊" :label "模糊"}
                                 {:value "谵妄" :label "谵妄"}
                                 {:value "昏睡" :label "昏睡"}
                                 {:value "浅昏迷" :label "浅昏迷"}
                                 {:value "深昏迷" :label "深昏迷"}
                                 {:value "其他" :label "其他"}]
          activity-level-options [{:value "正常活动" :label "正常活动"}
                                  {:value "轻度受限" :label "轻度受限"}
                                  {:value "重度受限" :label "重度受限"}
                                  {:value "卧床" :label "卧床"}
                                  {:value "其他" :label "其他"}]]
      [custom-styled-card
       [:> icons/HeartOutlined {:style {:marginRight "8px"}}]
       "一般情况"
       "#f6ffed" ; Header background color
       (if (seq physical-exam-data) ; Check if data is not empty
         [:> Form {:layout "horizontal" :labelCol {:sm {:span 24} :md {:span 10}} :wrapperCol {:sm {:span 24} :md {:span 14}} :labelAlign "left"
                   :initialValues (clj->js physical-exam-data)
                   :key patient-id} ; Key to re-initialize form when patient changes
          ;; 第一部分：身高、体重、精神状态、活动能力
          [:div {:key "vital-signs-group-1"}
           [:div {:style {:display "grid"
                          :gridTemplateColumns "repeat(4, 1fr)"
                          :gap "0px 16px"
                          :marginBottom "16px"}}
            [:> Form.Item {:label "身高" :name :height} ; Matches canonical
             [:> InputNumber {:placeholder "cm"
                              :addonAfter "cm"
                              :style {:width "100%"}
                              :min 0
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :height] %])}]]
            [:> Form.Item {:label "体重" :name :weight} ; Matches canonical
             [:> InputNumber {:placeholder "kg"
                              :addonAfter "kg"
                              :style {:width "100%"}
                              :min 0
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :weight] %])}]]
            [:> Form.Item {:label "精神状态" :name :mental_state} ; Matches canonical
             [:> Select {:placeholder "请选择"
                         :style {:width "100%"}
                         :allowClear true
                         :options mental-status-options
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :mental_state] %])}]]
            [:> Form.Item {:label "活动能力" :name :activity_level} ; Matches canonical
             [:> Select {:placeholder "请选择"
                         :style {:width "100%"}
                         :allowClear true
                         :options activity-level-options
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :activity_level] %])}]]]]

          ;; 分隔线 (可选，如果视觉上需要)
          ;; [:> Divider {:style {:margin "0 0 16px 0"}}]

          ;; 第二部分：血压、脉搏、呼吸、体温、SpO2
          [:div {:key "vital-signs-group-2"}
           [:div {:style {:display "flex" :flexWrap "wrap" :gap "8px 24px"}} ; 增大列间距
            ;; 血压
            [:> Form.Item {:label "血压"}
             [:div {:style {:display "flex" :alignItems "center"}}
              [:> Form.Item {:name :bp_systolic :noStyle true} ; Canonical: bp_systolic
               [:> InputNumber {:placeholder "收缩压"
                                :min 0
                                :style {:width "70px"}
                                :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :bp_systolic] %])}]]
              [:span {:style {:margin "0 4px"}} "/"]
              [:> Form.Item {:name :bp_diastolic :noStyle true} ; Canonical: bp_diastolic
               [:> InputNumber {:placeholder "舒张压"
                                :min 0
                                :style {:width "70px"}
                                :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :bp_diastolic] %])}]]
              [:span {:style {:marginLeft "8px"}} "mmHg"]]]

            ;; 脉搏
            [:> Form.Item {:label "脉搏" :name :heart_rate} ; Canonical: heart_rate
             [:> InputNumber {:placeholder "次/分"
                              :addonAfter "次/分"
                              :min 0
                              :style {:width "130px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :heart_rate] %])}]]

            ;; 呼吸
            [:> Form.Item {:label "呼吸" :name :respiratory_rate} ; Canonical: respiratory_rate
             [:> InputNumber {:placeholder "次/分"
                              :addonAfter "次/分"
                              :min 0
                              :style {:width "130px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :respiratory_rate] %])}]]

            ;; 体温
            [:> Form.Item {:label "体温" :name :temperature} ; Matches canonical
             [:> InputNumber {:placeholder "°C"
                              :addonAfter "°C"
                              :precision 1
                              :step 0.1
                              :style {:width "110px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :temperature] %])}]]

            ;; SpO2
            [:> Form.Item {:label "SpO2" :name :spo2} ; Matches canonical
             [:> InputNumber {:placeholder "%"
                              :addonAfter "%"
                              :min 0 :max 100
                              :style {:width "100px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :spo2] %])}]]]]]
         [:> Empty {:description "暂无一般情况信息或未选择患者"}])])))

(defn- medical-history-summary-card []
  (let [medical-history @(rf/subscribe [::subs/canonical-medical-history]) ; Use new subscription
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    [custom-styled-card
     [:> FileTextOutlined {:style {:marginRight "8px"}}]
     "病情摘要" ; Title remains, but content is now medical history
     "#fff7e6" ; Header background color
     (if (seq medical-history) ; Check if medical-history map is not empty
       [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left"
                 :initialValues (clj->js medical-history)
                 :key patient-id} ; Key for re-initialization
        ;; 过敏史
        [:div {:style {:marginBottom "16px"}}
         [:> Form.Item {:label "过敏史" :name [:allergy :has_history]} ; Path matches canonical
          [:> Radio.Group {;; :value (get-in medical-history [:allergy :has_history]) ; Let Form handle
                           :onChange #(let [val (-> % .-target .-value)]
                                        (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :has_history] val])
                                        (when-not val ; If "no", clear details
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :details] nil])
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :last_reaction_date] nil])))}
           [:> Radio {:value false} "无"]
           [:> Radio {:value true} "有"]]]
         (when (get-in medical-history [:allergy :has_history])
           [:<>
            [:> Form.Item {:label "过敏详情" :name [:allergy :details]} ; Path matches canonical
             [:> Input {;; :value (get-in medical-history [:allergy :details]) ; Let Form handle
                        :placeholder "请输入过敏源及症状"
                        :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :details] (-> % .-target .-value)])}]]
            [:> Form.Item {:label "最近反应日期" :name [:allergy :last_reaction_date]} ; Path matches canonical
             [:> DatePicker {;; :value (utils/to-moment (get-in medical-history [:allergy :last_reaction_date])) ; Let Form handle
                             :style {:width "100%"}
                             :format "YYYY-MM-DD"
                             :placeholder "请选择日期"
                             :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :last_reaction_date] (utils/date->iso-string %)])}]]])]

        ;; 生活习惯
        [:div
         ;; 吸烟史
         [:> Form.Item {:label "吸烟史" :name [:smoking :has_history]} ; Path matches canonical
          [:> Radio.Group {;; :value (get-in medical-history [:smoking :has_history]) ; Let Form handle
                           :onChange #(let [val (-> % .-target .-value)]
                                        (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :smoking :has_history] val])
                                        (when-not val ; If "no", clear details
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :smoking :years] nil])
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :smoking :cigarettes_per_day] nil])))}
           [:> Radio {:value false} "无"]
           [:> Radio {:value true} "有"]]]
         (when (get-in medical-history [:smoking :has_history])
           [:> Row {:gutter 16}
            [:> Col {:span 12}
             [:> Form.Item {:label "吸烟年数" :name [:smoking :years]} ; Path matches canonical
              [:> InputNumber {;; :value (get-in medical-history [:smoking :years]) ; Let Form handle
                               :min 0 :addonAfter "年" :style {:width "100%"}
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :smoking :years] %])}]]]
            [:> Col {:span 12}
             [:> Form.Item {:label "每天吸烟支数" :name [:smoking :cigarettes_per_day]} ; Path matches canonical
              [:> InputNumber {;; :value (get-in medical-history [:smoking :cigarettes_per_day]) ; Let Form handle
                               :min 0 :addonAfter "支" :style {:width "100%"}
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :smoking :cigarettes_per_day] %])}]]]])
         
         ;; 饮酒史
         [:> Form.Item {:label "饮酒史" :name [:drinking :has_history] :style {:marginTop "8px"}} ; Path matches canonical
          [:> Radio.Group {;; :value (get-in medical-history [:drinking :has_history]) ; Let Form handle
                           :onChange #(let [val (-> % .-target .-value)]
                                        (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :has_history] val])
                                        (when-not val ; If "no", clear details
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :years] nil])
                                          (rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :alcohol_per_day] nil])))}
           [:> Radio {:value false} "无"]
           [:> Radio {:value true} "有"]]]
         (when (get-in medical-history [:drinking :has_history])
           [:> Row {:gutter 16}
            [:> Col {:span 12}
             [:> Form.Item {:label "饮酒年数" :name [:drinking :years]} ; Path matches canonical
              [:> InputNumber {;; :value (get-in medical-history [:drinking :years]) ; Let Form handle
                               :min 0 :addonAfter "年" :style {:width "100%"}
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :years] %])}]]]
            [:> Col {:span 12}
             [:> Form.Item {:label "每天饮酒量" :name [:drinking :alcohol_per_day]} ; Path matches canonical
              [:> Input {;; :value (get-in medical-history [:drinking :alcohol_per_day]) ; Let Form handle
                         :placeholder "请输入饮酒量"
                         :style {:width "100%"}
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :alcohol_per_day] (-> % .-target .-value)])}]]]])]]
       [:> Empty {:description "请先选择患者或患者无病情摘要信息"}])])) ; Message can be updated if needed

(defn- comorbidities-card []
  (let [comorbidities-data @(rf/subscribe [::subs/canonical-comorbidities]) ; Use new subscription
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    (letfn [(comorbidity-item [field-key label-text]
              (let [base-path [:comorbidities field-key] ; Path for dispatch, e.g., [:comorbidities :respiratory]
                    form-item-name [field-key :has]    ; Path for Form.Item name, e.g., [:respiratory :has]
                    details-form-item-name [field-key :details]
                    has-value (get-in comorbidities-data [field-key :has])]
                [:> Col {:span 12}
                 [:> Form.Item {:label label-text :name form-item-name}
                  [:> Radio.Group {;; :value has-value ; Let Form handle value
                                   :onChange #(let [val (-> % .-target .-value)]
                                                (rf/dispatch [::events/update-canonical-assessment-field base-path (assoc (get comorbidities-data field-key) :has val)])
                                                (when-not val ; If "no" (false), clear details
                                                  (rf/dispatch [::events/update-canonical-assessment-field (conj base-path :details) nil])))}
                   [:> Radio {:value true} "有"]  ; Boolean true
                   [:> Radio {:value false} "无"]]] ; Boolean false
                 (when has-value ; Check for boolean true
                   [:> Form.Item {:name details-form-item-name
                                  :noStyle true
                                  :style {:marginTop "8px"}}
                    [:> Input {;; :value (get-in comorbidities-data [field-key :details]) ; Let Form handle
                               :placeholder "请填写具体内容"
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field (conj base-path :details) (-> % .-target .-value)])}]])]))]
      [custom-styled-card
       [:> MedicineBoxOutlined]
       "并存疾病"
       "#f9f0ff" ; Header background color
       (if (seq comorbidities-data)
         [:> Form {:layout "horizontal" :labelCol {:span 10} :wrapperCol {:span 14} :labelAlign "left"
                   :initialValues (clj->js comorbidities-data)
                   :key patient-id} ; Key for re-initialization
          [:> Row {:gutter [16 0]} ; Horizontal gutter 16, vertical 0
           (comorbidity-item :respiratory "呼吸系统疾病")
           (comorbidity-item :cardiovascular "心血管疾病")
           (comorbidity-item :endocrine "内分泌疾病")
           (comorbidity-item :neuro_psychiatric "神经精神疾病") ; Canonical key
           (comorbidity-item :neuromuscular "神经肌肉疾病")
           (comorbidity-item :hepatic "肝脏疾病")
           (comorbidity-item :renal "肾脏疾病")
           (comorbidity-item :musculoskeletal "关节骨骼系统")
           (comorbidity-item :malignant_hyperthermia_fh "家族恶性高热史") ; Canonical key
           (comorbidity-item :anesthesia_surgery_history "既往麻醉、手术史") ; Canonical key
           
           ;; 使用的特殊药物 - canonical path is [:comorbidities :special_medications]
           (let [base-path [:comorbidities :special_medications] ; Path for dispatch
                 form-item-base [:special_medications] ; Path for Form.Item name relative to comorbidities_data
                 has-taken-path (conj base-path :has_taken)
                 details-path (conj base-path :details)
                 last-dose-time-path (conj base-path :last_dose_time)
                 
                 has-taken-value (get-in comorbidities-data [:special_medications :has_taken])]
             [:> Col {:span 24} ; 占据整行
              [:> Form.Item {:label "使用的特殊药物" :name (conj form-item-base :has_taken)}
               [:> Radio.Group {;; :value has-taken-value ; Let Form handle
                                :onChange #(let [val (-> % .-target .-value)]
                                             (rf/dispatch [::events/update-canonical-assessment-field has-taken-path val])
                                             (when-not val
                                               (rf/dispatch [::events/update-canonical-assessment-field details-path nil])
                                               (rf/dispatch [::events/update-canonical-assessment-field last-dose-time-path nil])))}
                [:> Radio {:value true} "有"]
                [:> Radio {:value false} "无"]]
               (when has-taken-value
                 [:div {:style {:marginTop "8px"}}
                  [:> Form.Item {:name (conj form-item-base :details) :label "药物名称及剂量" :labelCol {:span 6} :wrapperCol {:span 18}}
                   [:> Input {;; :value (get-in comorbidities-data [:special_medications :details]) ; Let Form handle
                              :placeholder "药物名称及剂量"
                              :style {:marginBottom "8px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field details-path (-> % .-target .-value)])}]]
                  [:> Form.Item {:name (conj form-item-base :last_dose_time) :label "最近用药时间" :labelCol {:span 6} :wrapperCol {:span 18}}
                   [:> DatePicker {;; :value (utils/to-moment (get-in comorbidities-data [:special_medications :last_dose_time])) ; Let Form handle
                                   :showTime true
                                   :format "YYYY-MM-DD HH:mm"
                                   :placeholder "选择日期和时间"
                                   :style {:width "100%"}
                                   :onChange #(rf/dispatch [::events/update-canonical-assessment-field last-dose-time-path (utils/date->iso-string %)])}]]])]])]]
         [:> Empty {:description "暂无并存疾病信息或未选择患者"}])])))

(defn- physical-examination-card []
  (let [phys-exam-data @(rf/subscribe [::subs/canonical-physical-examination]) ; Use new subscription
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    (letfn [(exam-item [field-key label-text] ; field-key is like :heart
              (let [base-path [:physical_examination field-key] ; Path for dispatch, e.g. [:physical_examination :heart]
                    form-item-name [field-key :status] ; Path for Form.Item name, e.g. [:heart :status]
                    notes-form-item-name [field-key :notes]
                    current-status-val (get-in phys-exam-data [field-key :status] "normal")] ; Default to "normal" string as per original logic
                [:> Col {:span 12}
                 [:> Form.Item {:label label-text :name form-item-name}
                  [:> Radio.Group {;; :value current-status-val ; Let Form handle
                                   :onChange #(let [val (-> % .-target .-value)]
                                                (rf/dispatch [::events/update-canonical-assessment-field base-path (assoc (get phys-exam-data field-key) :status val)])
                                                (when (= val "normal") ; If "normal", clear notes
                                                  (rf/dispatch [::events/update-canonical-assessment-field (conj base-path :notes) nil])))}
                   [:> Radio {:value "normal"} "正常"]
                   [:> Radio {:value "abnormal"} "异常"]]
                  (when (= current-status-val "abnormal")
                    [:> Form.Item {:name notes-form-item-name
                                   :noStyle true
                                   :style {:marginTop "8px"}}
                     [:> Input {;; :value (get-in phys-exam-data [field-key :notes]) ; Let Form handle
                                :placeholder "请描述异常情况"
                                :onChange #(rf/dispatch [::events/update-canonical-assessment-field (conj base-path :notes) (-> % .-target .-value)])}]])]]))]
      [custom-styled-card
       [:> ProfileOutlined]
       "体格检查"
       "#e6f7ff" ; Header background color
       (if (seq phys-exam-data)
         [:> Form {:layout "horizontal" :labelCol {:span 8} :wrapperCol {:span 16} :labelAlign "left"
                   :initialValues (clj->js phys-exam-data)
                   :key patient-id} ; Key for re-initialization
          [:> Row {:gutter [16 0]}
           (exam-item :heart "心脏")
           (exam-item :lungs "肺脏")
           (exam-item :airway "气道")
           (exam-item :teeth "牙齿")
           (exam-item :spine_limbs "脊柱四肢") ; Canonical key
           (exam-item :neuro "神经")]
          [:> Form.Item {:label "其它" :name :other_findings} ; Canonical key for top-level other findings
           [:> Input.TextArea {;; :value (get phys-exam-data :other_findings) ; Let Form handle
                               :placeholder "如有其他体格检查发现请在此注明"
                               :rows 2
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:physical_examination :other_findings] (-> % .-target .-value)])}]]]
         [:> Empty {:description "暂无体格检查信息或未选择患者"}])])))

(defn- auxiliary-tests-card []
  (let [aux-exams @(rf/subscribe [::subs/canonical-auxiliary-examinations]) ; List of file maps
        aux-notes @(rf/subscribe [::subs/canonical-auxiliary-examinations-notes])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    (let [modal-open? (r/atom false)
          preview-image-url (r/atom "")
          handle-preview (fn [file] ; file is from Ant Design's fileList, which we'll construct
                           (let [url (or (.-url file) (.-thumbUrl file))]
                             (when url
                               (reset! preview-image-url url)
                               (reset! modal-open? true))))
          upload-props {;; :action "/api/files/upload" ; Replace with your actual upload endpoint or use customRequest
                        ;; For this refactor, we assume :customRequest handles the upload and then dispatches an event to add the file meta to app-db.
                        :customRequest (fn [req-info]
                                         ;; Mocking successful upload and adding to list
                                         ;; In a real app, this would involve actual AJAX call to upload req-info.file
                                         ;; On success, server returns URL and other metadata.
                                         (js/setTimeout
                                          #(let [mock-url (str "/uploads/mock-" (.-name req-info.file))
                                                 new-file-map {:uid (.-uid req-info.file) ; AntD generated UID
                                                               :type "other" ; Or determine from file type or UI
                                                               :filename (.-name req-info.file)
                                                               :url mock-url
                                                               :uploaded_by "doctor"
                                                               :uploaded_at (utils/date->iso-string (js/Date.now))}]
                                             (rf/dispatch [::events/add-aux-exam-file new-file-map])
                                             ((.-onSuccess req-info) {:status "done"} req-info.file))
                                          1000))
                        :listType "picture-card"
                        :fileList (mapv (fn [file-map]
                                          {:uid (or (:uid file-map) (:url file-map)) ; Ensure UID is present, use URL if no UID
                                           :name (:filename file-map)
                                           :status "done" ; Assuming all files from DB are "done"
                                           :url (:url file-map)
                                           ;; Store original canonical map for removal if needed
                                           :canonicalData file-map})
                                        aux-exams)
                        :onPreview handle-preview
                        :onRemove (fn [file]
                                    ;; Remove by UID if present, otherwise by URL (from canonicalData if needed)
                                    (let [uid-or-url (or (.-uid file) (get-in file [:canonicalData :url]))]
                                      (rf/dispatch [::events/remove-aux-exam-file uid-or-url])
                                      true)) ; Return true to confirm removal from UI
                        :multiple true}]
      [custom-styled-card
       [:> SolutionOutlined]
       "相关辅助检查检验结果"
       "#fffbe6" ; Header background color
       [:> Form {:layout "vertical" ; Vertical layout might be better for list + notes
                 :initialValues {:auxiliary_examinations_notes aux-notes}
                 :key patient-id}
        [:> Form.Item {:label "上传检查文件 (如ECG, 胸片, 血常规等)"}
         [:> Upload upload-props
          [:div
           [:> icons/UploadOutlined]
           [:div {:style {:marginTop 8}} "上传文件"]]]]
        
        [:> Form.Item {:label "其他检查结果说明" :name :auxiliary_examinations_notes}
         [:> Input.TextArea {;; :value aux-notes ; Let Form handle
                             :placeholder "请在此记录其他重要检查结果的文字描述或总结"
                             :rows 4
                             :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:auxiliary_examinations_notes] (-> % .-target .-value)])}]]
        
        (when @modal-open?
          [:> Modal {:visible @modal-open?
                     :title "预览"
                     :footer nil
                     :onCancel #(reset! modal-open? false)}
           [:img {:alt "预览" :style {:width "100%"} :src @preview-image-url}]])]])))


;; 辅助函数，用于显示术前麻醉医嘱（可编辑表单）
(defn- preoperative-orders-card []
  (let [anesthesia-plan-data @(rf/subscribe [::subs/canonical-anesthesia-plan]) ; Use new subscription
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])] ; For Form key
    [custom-styled-card
     [:> icons/EditOutlined {:style {:marginRight "8px"}}]
     "术前麻醉医嘱"
     "#fff1f0" ; Header background color
     (if (seq anesthesia-plan-data) ; Check if data is not empty
       [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left"
                 :initialValues (clj->js anesthesia-plan-data)
                 :key patient-id} ; Key for re-initialization
        [:div {:style {:display "grid"
                       :gridTemplateColumns "repeat(4, 1fr)"
                       :gap "0px 16px"}}
         [:> Form.Item {:label "ASA分级" :name :asa_rating :style {:gridColumn "span 1"}} ; Matches canonical
          [:> Select {;; :value (:asa_rating anesthesia-plan-data) ; Let Form handle
                      :placeholder "请选择ASA分级"
                      :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:anesthesia_plan :asa_rating] %])
                      :options (mapv (fn [i] {:value (str "ASA " i) :label (str "ASA " i)}) (range 1 7))}]]
         [:> Form.Item {:label "麻醉方式" :name :anesthesia_type :style {:gridColumn "span 3"}} ; Matches canonical
          [:> Input {;; :value (:anesthesia_type anesthesia-plan-data) ; Let Form handle
                     :placeholder "请输入麻醉方式"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:anesthesia_plan :anesthesia_type] (-> % .-target .-value)])}]]
         [:> Form.Item {:label "术前医嘱" :name :preoperative_instructions :style {:gridColumn "span 4"}} ; Matches canonical
          [:> Input.TextArea {;; :value (:preoperative_instructions anesthesia-plan-data) ; Let Form handle
                              :rows 3
                              :placeholder "请输入术前医嘱，例如禁食水时间、特殊准备等"
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:anesthesia_plan :preoperative_instructions] (-> % .-target .-value)])}]]]])
     [:> Empty {:description "暂无术前麻醉医嘱信息或未选择患者"}]]))

;; 辅助函数，用于显示签名和日期
(defn- signature-and-date-card []
  (let [basic-info @(rf/subscribe [::subs/canonical-basic-info])
        assessment-updated-at (get basic-info :assessment_updated_at (utils/date->iso-string (js/Date.now))) ; Default to now if not present
        doctor-name (get basic-info :doctor_name)]
    [custom-styled-card
     [:> icons/SaveOutlined {:style {:marginRight "8px"}}]
     "麻醉医师签名及日期"
     "#fff0f6" ; Header background color
     [:> Descriptions {:bordered true :column 1 :size "small"} ; Changed to 1 column for better layout
      [:> Descriptions.Item {:label "麻醉医师"}
       [:> Input {:placeholder "记录医师姓名"
                  :value doctor-name
                  :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :doctor_name] (-> % .-target .-value)])}]]
      [:> Descriptions.Item {:label "评估更新日期"}
       (utils/format-date assessment-updated-at "YYYY-MM-DD HH:mm")]]]))

;; 辅助函数，用于显示备注信息 (now part of basic_info)
(defn- remarks-card []
  (let [assessment-notes (rf/subscribe [::subs/canonical-basic-info :assessment_notes])]
    [custom-styled-card
     [:> icons/MessageOutlined {:style {:marginRight "8px"}}]
     "评估备注" ; Changed title to be more specific
     "#fffaf0" ; Header background color
     [:> Input.TextArea {:rows 4 ; Increased rows for better visibility
                         :value (or @assessment-notes "") ; Deref the atom from subscription
                         :placeholder "评估备注（如有特殊情况请在此注明）"
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:basic_info :assessment_notes] (-> % .-target .-value)])}]]))

(defn- assessment-action-buttons [patient-status]
  [:> Space {} ; Removed marginBottom, parent will handle layout
   [:> Button {:type "primary" :icon (r/as-element [:> CheckCircleOutlined])
               :on-click #(rf/dispatch [::events/approve-patient])
               :style {:background "#52c41a" :borderColor "#52c41a"}}
    "批准"]
   [:> Button {:type "primary" :icon (r/as-element [:> ClockCircleOutlined])
               :on-click #(rf/dispatch [::events/postpone-patient])
               :style {:background "#faad14" :borderColor "#faad14"}}
    "暂缓"]
   [:> Button {:type "primary" :icon (r/as-element [:> CloseCircleOutlined])
               :on-click #(rf/dispatch [::events/reject-patient])
               :danger true}
    "驳回"]
   (when (= patient-status "已批准") ; Check for the correct status string
     [:> Button {:icon (r/as-element [:> PrinterOutlined])
                 :on-click #(js/window.print)
                 :style {:background "#1890ff" :borderColor "#1890ff" :color "white"}} ; Added styling
      "打印表单"])])

(defn save-button []
  [:div {:style {:padding "10px 0"
                 :background "white"
                 :borderTop "1px solid #f0f0f0"
                 :textAlign "center"
                 :position "sticky"
                 :bottom 0
                 :zIndex 10}} ; Ensure it's above scrolled content
   [:> Button {:type "primary"
               :size "large"
               :icon (r/as-element [:> icons/SaveOutlined])
               :onClick #(rf/dispatch [::events/save-final-assessment])}
    "保存评估结果"]])

(defn- assessment []
  (let [current-patient-id @(rf/subscribe [::subs/current-patient-id])
        basic-info @(rf/subscribe [::subs/canonical-basic-info]) ; Use canonical basic-info
        patient-name (get basic-info :name "未知患者")
        patient-status (get basic-info :assessment_status "待评估")] ; Get status from canonical basic_info
    (if current-patient-id
      ;; 有选择患者时的视图
      [:div {:style {:height "calc(100vh - 64px)" :display "flex" :flexDirection "column"}}
       ;; Top bar with Patient Name and Action Buttons
       [:div {:style {:display "flex"
                      :justifyContent "space-between"
                      :alignItems "center"
                      :padding "12px 16px"
                      :borderBottom "1px solid #f0f0f0"
                      :background "#fff"}}
        [:h3 {:style {:margin 0 :fontSize "16px" :fontWeight "500"}} patient-name]
        [assessment-action-buttons patient-status]]

       ;; Main scrollable content area for cards
       [:div {:style {:padding "16px" :overflowY "auto" :flexGrow 1 :background "#f0f2f5"}}
        [patient-info-card]
        [general-condition-card]
        [medical-history-summary-card]
        [comorbidities-card]
        [physical-examination-card]
        [auxiliary-tests-card]
        ;; ASA评分和麻醉计划 - Consider if this needs its own card or is part of preoperative-orders
        [preoperative-orders-card]
        [remarks-card]
        [signature-and-date-card]
        [save-button]]]

      ;; 无选择患者时的空状态
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100%"}}
       [:> Empty {:description "请从左侧选择一位患者开始评估"}]])))

(defn anesthesia-content []
  [:> Layout.Content {:style {:margin 0 :minHeight 280 :overflow "hidden" :display "flex" :flexDirection "row"}}
   ;; 左侧患者列表区域
   [:> Card {:style {:width "350px"
                     :minWidth "300px"
                     :height "calc(100vh - 64px)" ; 假设顶部导航栏高度为 64px
                     :borderRight "1px solid #f0f0f0"
                     :display "flex"
                     :flexDirection "column"
                     :padding "0"}}

    ;; 患者列表主体
    [:div {:style {:flexGrow 1 :overflowY "auto"}}
     [patient-list-panel]]]

   ;; 右侧评估详情区域
   [:div {:style {:flexGrow 1 :background "#f0f2f5" :overflow "hidden"}}
    [assessment]]])
