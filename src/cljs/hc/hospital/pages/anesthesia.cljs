(ns hc.hospital.pages.anesthesia
  "麻醉管理, 医生补充患者自己填写的评估报告, 最终评估患者的情况, 判断是否可以麻醉"
  (:require ; Added Image, Modal
   ;; 确保 antd/Form 等组件已引入
   [taoensso.timbre :as timbre]
   ["@ant-design/icons" :as icons :refer [FileTextOutlined MedicineBoxOutlined
                                          ProfileOutlined QrcodeOutlined
                                          SolutionOutlined SyncOutlined
                                          UserOutlined HeartOutlined EditOutlined
                                          SaveOutlined MessageOutlined
                                          UploadOutlined CheckCircleOutlined ClockCircleOutlined CloseCircleOutlined PrinterOutlined]]
   ["antd" :refer [Button Card Col DatePicker Descriptions Empty Form Image
                   Input InputNumber Layout Modal Radio Row Select Space Tag
                   Upload]] ; Removed Tooltip as it's not used
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [hc.hospital.utils :as utils]
   [re-frame.core :as rf]
   [reagent.core :as r]))


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
             (str (:sex item) " " (:age item) " " (:anesthesia-type item))]]]
          [:div {:style {:textAlign "right"}}
           [:div {:style {:fontSize "12px" :color "gray" :marginBottom "4px"}} (:date item)]
           [:> Tag {:color (case (:status item)
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

;; 新的辅助函数，用于创建统一样式的卡片
(defn- custom-styled-card [icon title-text header-bg-color content]
  [:> Card {:title (r/as-element [:span icon title-text])
            :styles {:header {:background header-bg-color}
                     :body {:background "#ffffff"}} ; 确保内容区域背景为白色
            :type "inner"
            :style {:marginBottom "12px"}}
   content])

;; 辅助函数，用于显示患者基本信息
(defn- patient-info-card []
  (let [raw @(rf/subscribe [::subs/selected-patient-raw-details])
        basic-info (get-in raw [:assessment_data :basic-info] {})]
    [custom-styled-card
     [:> icons/UserOutlined {:style {:marginRight "8px"}}]
     "患者基本信息"
     "#e6fffb"                          ; Header background color
     (if (seq basic-info)
       [:> Form {:layout "horizontal" :labelCol {:span 8} :wrapperCol {:span 16} :labelAlign "left"
                 :initialValues basic-info}
        [:div {:style {:display "grid"
                       :gridTemplateColumns "repeat(4, 1fr)" ; 4 列
                       :gap "0px 16px"}}                     ; 列间距

         [:> Form.Item {:label "门诊号" :name :outpatient-number ; :name 用于 Form 自动关联
                        :rules [{:required true :message "请输入门诊号!"}]} ; 示例：添加校验规则
          [:> Input {:value (str (timbre/spy :info (:outpatient-number basic-info))) ; 显式绑定 value
                     :placeholder "请输入门诊号"
                     :onChange #(rf/dispatch [::events/update-patient-form-field :outpatient-number (-> % .-target .-value)])
                     }]]

         [:> Form.Item {:label "姓名" :name :name}
          [:> Input {:value (:name basic-info)
                     :placeholder "请输入姓名"
                     :onChange #(rf/dispatch [::events/update-patient-form-field :name (-> % .-target .-value)])}]]

         [:> Form.Item {:label "性别" :name :sex}
          [:> Select {:value (:sex basic-info)
                      :placeholder "请选择性别"
                      :onChange #(rf/dispatch [::events/update-patient-form-field :sex %])
                      :options [{:value "男" :label "男"}
                                {:value "女" :label "女"}
                                {:value "其他" :label "其他"}]}]]

         [:> Form.Item {:label "年龄" :name :age}
          [:> InputNumber {:value (:age basic-info)
                           :placeholder "岁"
                           :min 0
                           :style {:width "100%"}
                           :addonAfter "岁"
                           :onChange #(rf/dispatch [::events/update-patient-form-field :age %])}]]

         [:> Form.Item {:label "病区" :name :department
                        :style {:gridColumn "span 2"}} ; 占据两列
          [:> Input {:value (:department basic-info)
                     :placeholder "请输入病区"
                     :onChange #(rf/dispatch [::events/update-patient-form-field :department (-> % .-target .-value)])}]]

         [:> Form.Item {:label "电子健康卡号" :name :health-card-number
                        :style {:gridColumn "span 2"}} ; 占据两列
          [:> Input {:value (:health-card-number basic-info)
                     :placeholder "请输入电子健康卡号 (可选)"
                     :onChange #(rf/dispatch [::events/update-patient-form-field :health-card-number (-> % .-target .-value)])}]]

         [:> Form.Item {:label "术前诊断" :name :diagnosis
                        :style {:gridColumn "span 4"}} ; 占据四列
          [:> Input.TextArea {:value (:diagnosis basic-info)
                              :placeholder "请输入术前诊断"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-patient-form-field :diagnosis (-> % .-target .-value)])}]]

         [:> Form.Item {:label "拟施手术" :name :planned-surgery ; 假设API返回的字段是 :planned-surgery
                        :style {:gridColumn "span 4"}}           ; 占据四列
          [:> Input.TextArea {:value (:planned-surgery basic-info) ; 或者 (:type basic-info) 取决于你的数据结构
                              :placeholder "请输入拟施手术"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-patient-form-field :planned-surgery (-> % .-target .-value)])}]]]]
       [:> Empty {:description "请先选择患者或患者无基本信息可编辑"}])]))

;; 辅助函数，用于显示一般情况
(defn- general-condition-card []
  (let [raw @(rf/subscribe [::subs/selected-patient-raw-details])
        exam-data (get-in raw [:assessment_data :physical-examination] {})
        ;; 定义选项数据
        mental-status-options [{:value "清醒" :label "清醒"}
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
     (if (some? exam-data) ; 确保 exam-data 不是 nil，空 map {} 也是有效的
       [:> Form {:layout "horizontal" :labelCol {:sm {:span 24} :md {:span 10}} :wrapperCol {:sm {:span 24} :md {:span 14}} :labelAlign "left"
                 :initialValues exam-data} ; Bind initialValues to the form
        ;; 第一部分：身高、体重、精神状态、活动能力
        [:div {:key "vital-signs-group-1"}
         [:div {:style {:display "grid"
                        :gridTemplateColumns "repeat(4, 1fr)"
                        :gap "0px 16px"
                        :marginBottom "16px"}}
          [:> Form.Item {:label "身高" :name :height}
           [:> InputNumber {:value (:height exam-data)
                            :placeholder "cm"
                            :addonAfter "cm"
                            :style {:width "100%"}
                            :min 0
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :height %])}]]
          [:> Form.Item {:label "体重" :name :weight}
           [:> InputNumber {:value (:weight exam-data)
                            :placeholder "kg"
                            :addonAfter "kg"
                            :style {:width "100%"}
                            :min 0
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :weight %])}]]
          [:> Form.Item {:label "精神状态" :name :mental-state}
           [:> Select {:value (:mental-state exam-data)
                       :placeholder "请选择"
                       :style {:width "100%"}
                       :allowClear true
                       :options mental-status-options
                       :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :mental-state %])}]]
          [:> Form.Item {:label "活动能力" :name :activity-level}
           [:> Select {:value (:activity-level exam-data)
                       :placeholder "请选择"
                       :style {:width "100%"}
                       :allowClear true
                       :options activity-level-options
                       :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :activity-level %])}]]]]

        ;; 分隔线 (可选，如果视觉上需要)
        ;; [:> Divider {:style {:margin "0 0 16px 0"}}]

        ;; 第二部分：血压、脉搏、呼吸、体温、SpO2
        [:div {:key "vital-signs-group-2"}
         [:div {:style {:display "flex" :flexWrap "wrap" :gap "8px 24px"}} ; 增大列间距
          ;; 血压
          [:> Form.Item {:label "血压"}
           [:div {:style {:display "flex" :alignItems "center"}}
            [:> Form.Item {:name [:bp :systolic] :noStyle true}
             [:> InputNumber {:value (get-in exam-data [:bp :systolic])
                              :placeholder "收缩压"
                              :min 0
                              :style {:width "70px"}
                              :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field [:bp :systolic] %])}]]
            [:span {:style {:margin "0 4px"}} "/"]
            [:> Form.Item {:name [:bp :diastolic] :noStyle true}
             [:> InputNumber {:value (get-in exam-data [:bp :diastolic])
                              :placeholder "舒张压"
                              :min 0
                              :style {:width "70px"}
                              :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field [:bp :diastolic] %])}]]
            [:span {:style {:marginLeft "8px"}} "mmHg"]]]

          ;; 脉搏
          [:> Form.Item {:label "脉搏" :name :heart-rate}
           [:> InputNumber {:value (:heart-rate exam-data)
                            :placeholder "次/分"
                            :addonAfter "次/分"
                            :min 0
                            :style {:width "130px"}
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :heart-rate %])}]]

          ;; 呼吸
          [:> Form.Item {:label "呼吸" :name :respiratory-rate}
           [:> InputNumber {:value (:respiratory-rate exam-data)
                            :placeholder "次/分"
                            :addonAfter "次/分"
                            :min 0
                            :style {:width "130px"}
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :respiratory-rate %])}]]

          ;; 体温
          [:> Form.Item {:label "体温" :name :temperature}
           [:> InputNumber {:value (:temperature exam-data)
                            :placeholder "°C"
                            :addonAfter "°C"
                            :precision 1 ; 体温通常一位小数
                            :step 0.1
                            :style {:width "110px"}
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :temperature %])}]]

          ;; SpO2
          [:> Form.Item {:label "SpO2" :name :spo2}
           [:> InputNumber {:value (:spo2 exam-data)
                            :placeholder "%"
                            :addonAfter "%"
                            :min 0 :max 100
                            :style {:width "100px"}
                            :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :spo2 %])}]]]]])
     [:> Empty {:description "暂无一般情况信息或未选择患者"}]]))

(defn- medical-history-summary-card []
  (let [summary-data @(rf/subscribe [::subs/medical-summary-data])]
    [custom-styled-card
     [:> FileTextOutlined {:style {:marginRight "8px"}}]
     "病情摘要"
     "#fff7e6" ; Header background color
     [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left" :initialValues summary-data}
      ;; 过敏史
      [:div {:style {:marginBottom "16px"}}
       [:> Form.Item {:name [:allergy :has] :label "过敏史" :colon false}
        [:> Radio.Group {:value (get-in summary-data [:allergy :has])
                         :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :has] (-> % .-target .-value)])}
         [:> Radio {:value "no"} "无"]
         [:> Radio {:value "yes"} "有"]]]
       (when (= (get-in summary-data [:allergy :has]) "yes")
         [:<>
          [:> Form.Item {:name [:allergy :allergen] :label "过敏源头"}
           [:> Input {:value (get-in summary-data [:allergy :allergen])
                      :placeholder "请输入过敏源"
                      :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :allergen] (-> % .-target .-value)])}]]
          [:> Form.Item {:name [:allergy :last-reaction-date] :label "最近发生过敏时间"}
           [:> DatePicker {:value (utils/to-moment (get-in summary-data [:allergy :last-reaction-date]))
                           :style {:width "100%"}
                           :placeholder "请选择日期"
                           :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :last-reaction-date] (utils/date->iso-string %)])}]]])]

      ;; 生活习惯
      [:div
       ;; 吸烟史
       [:> Form.Item {:name [:habits :smoking :has] :label "吸烟史" :colon false}
        [:> Radio.Group {:value (get-in summary-data [:habits :smoking :has])
                         :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :has] (-> % .-target .-value)])}
         [:> Radio {:value "no"} "无"]
         [:> Radio {:value "yes"} "有"]]]
       (when (= (get-in summary-data [:habits :smoking :has]) "yes")
         [:> Row {:gutter 16}
          [:> Col {:span 12}
           [:> Form.Item {:name [:habits :smoking :years] :label "吸烟年数"}
            [:> InputNumber {:value (get-in summary-data [:habits :smoking :years])
                             :min 0 :addonAfter "年" :style {:width "100%"}
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :years] %])}]]]
          [:> Col {:span 12}
           [:> Form.Item {:name [:habits :smoking :per-day] :label "每天吸烟支数"}
            [:> InputNumber {:value (get-in summary-data [:habits :smoking :per-day])
                             :min 0 :addonAfter "支" :style {:width "100%"}
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :per-day] %])}]]]])
       ;; 饮酒史
       [:> Form.Item {:name [:habits :drinking :has] :label "饮酒史" :colon false :style {:marginTop "8px"}}
        [:> Radio.Group {:value (get-in summary-data [:habits :drinking :has])
                         :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :has] (-> % .-target .-value)])}
         [:> Radio {:value "no"} "无"]
         [:> Radio {:value "yes"} "有"]]]
       (when (= (get-in summary-data [:habits :drinking :has]) "yes")
         [:> Row {:gutter 16}
          [:> Col {:span 12}
           [:> Form.Item {:name [:habits :drinking :years] :label "饮酒年数"}
            [:> InputNumber {:value (get-in summary-data [:habits :drinking :years])
                             :min 0 :addonAfter "年" :style {:width "100%"}
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :years] %])}]]]
          [:> Col {:span 12}
           [:> Form.Item {:name [:habits :drinking :per-day] :label "每天饮酒量"}
            [:> InputNumber {:value (get-in summary-data [:habits :drinking :per-day])
                             :min 0 :addonAfter "ml" :style {:width "100%"}
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :per-day] %])}]]]])]]]))

(defn- comorbidities-card []
  (let [form-data @(rf/subscribe [::subs/medical-summary-data])
        comorbidity-data (get-in form-data [:comorbidities])
        comorbidity-item (fn [field-key label-text form-item-name]
                           (let [;; Path for dispatching updates, relative to form-data (which is root assessment_data)
                                 dispatch-has-path [:comorbidities field-key :has]
                                 dispatch-details-path [:comorbidities field-key :details]
                                 ;; Values for display, read directly from comorbidity-data using field-key
                                 current-has (get-in comorbidity-data [field-key :has] "no")
                                 current-details (get-in comorbidity-data [field-key :details])]
                             [:> Col {:span 12}
                              [:> Form.Item {:label label-text :name form-item-name} ; Antd Form uses this with initialValues for display
                               [:> Radio.Group {:value current-has ; Explicit value binding
                                                :onChange #(rf/dispatch [::events/update-medical-summary-field dispatch-has-path (-> % .-target .-value)])}
                                [:> Radio {:value "yes"} "有"]
                                [:> Radio {:value "no"} "无"]]
                               (when (= current-has "yes")
                                 [:> Input {:value current-details ; Explicit value binding
                                            :placeholder "请填写具体内容"
                                            :style {:marginTop "8px"}
                                            :onChange #(rf/dispatch [::events/update-medical-summary-field dispatch-details-path (-> % .-target .-value)])}])]]))]
    [custom-styled-card
     [:> MedicineBoxOutlined]
     "并存疾病"
     "#f9f0ff" ; Header background color
     [:> Form {:layout "horizontal" :labelCol {:span 10} :wrapperCol {:span 14} :labelAlign "left" :initialValues comorbidity-data}
      [:> Row {:gutter [16 0]} ; Horizontal gutter 16, vertical 0
       (comorbidity-item :respiratory "呼吸系统疾病" [:respiratory :has])
       (comorbidity-item :cardiovascular "心血管疾病" [:cardiovascular :has])
       (comorbidity-item :endocrine "内分泌疾病" [:endocrine :has])
       (comorbidity-item :neuro-psychiatric "神经精神疾病" [:neuro-psychiatric :has])
       (comorbidity-item :neuromuscular "神经肌肉疾病" [:neuromuscular :has])
       (comorbidity-item :hepatic "肝脏疾病" [:hepatic :has])
       (comorbidity-item :renal "肾脏疾病" [:renal :has])
       (comorbidity-item :musculoskeletal "关节骨骼系统" [:musculoskeletal :has])
       (comorbidity-item :malignant-hyperthermia "家族恶性高热史" [:malignant-hyperthermia :has])
       (comorbidity-item :anesthesia-surgery-history "既往麻醉、手术史" [:anesthesia-surgery-history :has])
       ;; 使用的特殊药物 - 单独处理，因为它可能需要不同的输入字段
       (let [path [:comorbidities :special-medications]
             has-path (conj path :has)
             details-path (conj path :details)
             last-dose-time-path (conj path :last-dose-time)
             current-has (get-in comorbidity-data has-path "no")]
         [:> Col {:span 24} ; 占据整行
          [:> Form.Item {:label "使用的特殊药物" :name [:special-medications :has]}
           [:> Radio.Group {:value current-has
                            :onChange #(rf/dispatch [::events/update-medical-summary-field has-path (-> % .-target .-value)])}
            [:> Radio {:value "yes"} "有"]
            [:> Radio {:value "no"} "无"]]
           (when (= current-has "yes")
             [:div {:style {:marginTop "8px"}}
              [:> Form.Item {:name [:special-medications :details] :label "药物名称及剂量" :noStyle true} ; noStyle for inline display
               [:> Input {:value (get-in comorbidity-data details-path)
                          :placeholder "药物名称及剂量"
                          :style {:marginBottom "8px"}
                          :onChange #(rf/dispatch [::events/update-medical-summary-field details-path (-> % .-target .-value)])}]]
              [:> Form.Item {:name [:special-medications :last-dose-time] :label "最近用药时间" :noStyle true}
               [:> DatePicker {:value (utils/to-moment (get-in comorbidity-data last-dose-time-path))
                               :showTime true
                               :placeholder "选择日期和时间"
                               :style {:width "100%"}
                               :onChange #(rf/dispatch [::events/update-medical-summary-field last-dose-time-path (utils/date->iso-string %)])}]]])]])]]]))

(defn- physical-examination-card []
  (let [raw @(rf/subscribe [::subs/selected-patient-raw-details])
        phys-data (get-in raw [:assessment_data :physical-examination] {}) ; This is the map e.g. {:heart {...}, :lungs {...}}
        exam-item (fn [field-key label-text form-item-name]
                    (let [;; Correct paths for dispatching updates to form-data (root assessment_data)
                          dispatch-status-path [:physical-examination field-key :status]
                          dispatch-notes-path [:physical-examination field-key :notes]
                          ;; Values for display, read directly from phys-data (which is raw[:assessment_data :physical-examination])
                          current-status-val (get-in phys-data [field-key :status] "normal")
                          current-notes-val (get-in phys-data [field-key :notes])]
                      [:> Col {:span 12}
                       [:> Form.Item {:label label-text :name form-item-name} ; Antd Form uses this with initialValues for display
                        [:> Radio.Group {:value current-status-val ; Explicit value binding
                                         :onChange #(rf/dispatch [::events/update-medical-summary-field dispatch-status-path (-> % .-target .-value)])}
                         [:> Radio {:value "normal"} "正常"]
                         [:> Radio {:value "abnormal"} "异常"]]
                        (when (= current-status-val "abnormal")
                          [:> Input {:value current-notes-val ; Explicit value binding
                                     :placeholder "请描述异常情况"
                                     :style {:marginTop "8px"}
                                     :onChange #(rf/dispatch [::events/update-medical-summary-field dispatch-notes-path (-> % .-target .-value)])}])]]))]
    [custom-styled-card
     [:> ProfileOutlined]
     "体格检查"
     "#e6f7ff" ; Header background color
     [:> Form {:layout "horizontal" :labelCol {:span 8} :wrapperCol {:span 16} :labelAlign "left" :initialValues phys-data}
      [:> Row {:gutter [16 0]}
       (exam-item :heart "心脏" [:heart :status])
       (exam-item :lungs "肺脏" [:lungs :status])
       (exam-item :airway "气道" [:airway :status])
       (exam-item :teeth "牙齿" [:teeth :status])
       (exam-item :spine-limbs "脊柱四肢" [:spine-limbs :status])
       (exam-item :neuro "神经" [:neuro :status])]
      [:> Form.Item {:label "其它" :name [:other :notes]} ; Antd Form uses this with initialValues for display
       [:> Input.TextArea {:value (get-in phys-data [:other :notes]) ; Explicit value binding
                           :placeholder "如有其他体格检查发现请在此注明"
                           :rows 2
                           :onChange #(rf/dispatch [::events/update-medical-summary-field [:physical-examination :other :notes] (-> % .-target .-value)])}]]]]))

(defn- auxiliary-tests-card []
  (let [form-data @(rf/subscribe [::subs/medical-summary-data]) ; ::subs/medical-summary-data points to the root of form-data
        aux-data (get-in form-data [:aux-exams] {}) ; Access :aux-exams from form-data
        modal-open? (r/atom false)
        preview-image-url (r/atom "")
        handle-preview (fn [file-or-url]
                         (if (string? file-or-url)
                           (do
                             (reset! preview-image-url file-or-url)
                             (reset! modal-open? true))
                           (when-let [origin-file (.-originFileObj file-or-url)]
                             (let [reader (js/FileReader.)]
                               (set! (.-onload reader)
                                     (fn []
                                       (reset! preview-image-url (.-result reader))
                                       (reset! modal-open? true)))
                               (.readAsDataURL reader origin-file)))))
        upload-props (fn [field-key]
                       {:name field-key
                        :listType "picture-card"
                        :fileList (let [files (get-in aux-data [field-key] [])]
                                    (mapv (fn [file-url idx]
                                            {:uid (str field-key "-" idx)
                                             :name (str "文件" (inc idx))
                                             :status "done"
                                             :url file-url})
                                          files
                                          (range)))
                        :onPreview handle-preview
                        :onChange (fn [info]
                                    (let [file (.-file info)
                                          file-list (-> info .-fileList js->clj (vec))
                                          event-type (cond
                                                       (= (.-status file) "removed") ::events/remove-aux-exam-file
                                                       (or (= (.-status file) "done") (= (.-status file) "uploading")) ::events/upload-aux-exam-file
                                                       :else nil)]
                                      (when event-type
                                        (rf/dispatch [event-type field-key file file-list]))))
                        :beforeUpload (fn [file]
                                        (rf/dispatch [::events/before-upload-aux-exam-file field-key file])
                                        false) ; Prevent auto-upload, handle in event
                        })
        upload-button (fn [field-key]
                        [:> Upload (upload-props field-key)
                         [:div
                          [:> icons/UploadOutlined]
                          [:div {:style {:marginTop 8}} "上传"]]])
        image-display (fn [field-key label]
                        (let [files (get-in aux-data [field-key] [])]
                          [:> Form.Item {:label label}
                           (if (seq files)
                             (upload-props field-key) ; Show Upload component with existing files
                             (upload-button field-key))]))] ; Show Upload button if no files
    [custom-styled-card
     [:> SolutionOutlined]
     "相关辅助检查检验结果"
     "#fffbe6" ; Header background color
     [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left" :initialValues aux-data}
      [:> Row {:gutter [16 16]}
       [:> Col {:span 12}
        (image-display :ecg "心电图 (ECG)")]
       [:> Col {:span 12}
        (image-display :chest-xray "胸部X光")]
       [:> Col {:span 12}
        (image-display :blood-tests "血常规/生化")]
       [:> Col {:span 12}
        (image-display :coagulation "凝血功能")]
       [:> Col {:span 24}
        [:> Form.Item {:label "其他检查结果" :name [:other-tests :notes]}
         [:> Input.TextArea {:value (get-in aux-data [:other-tests :notes])
                             :placeholder "请在此记录其他重要检查结果的文字描述"
                             :rows 3
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:aux-exams :other-tests :notes] (-> % .-target .-value)])}]]]]
      (when @modal-open?
        [:> Modal {:visible @modal-open?
                   :footer nil
                   :onCancel #(reset! modal-open? false)}
         [:img {:alt "预览" :style {:width "100%"} :src @preview-image-url}]])]]))

;; 辅助函数，用于显示术前麻醉医嘱（可编辑表单）
(defn- preoperative-orders-card []
  (let [plan-details @(rf/subscribe [::subs/anesthesia-plan-details])]
    [custom-styled-card
     [:> icons/EditOutlined {:style {:marginRight "8px"}}]
     "术前麻醉医嘱"
     "#fff1f0" ; Header background color
     [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left" :initialValues plan-details}
      [:div {:style {:display "grid"
                     :gridTemplateColumns "repeat(4, 1fr)"
                     :gap "0px 16px"}}
       [:> Form.Item {:label "ASA分级" :name :asa-rating :style {:gridColumn "span 1"}}
        [:> Select {:value (:asa-rating plan-details)
                    :placeholder "请选择ASA分级"
                    :onChange #(rf/dispatch [::events/update-anesthesia-plan-field :asa-rating %])
                    :options (mapv (fn [i] {:value (str "ASA " i) :label (str "ASA " i)}) (range 1 7))}]]
       [:> Form.Item {:label "麻醉方式" :name :anesthesia-type :style {:gridColumn "span 3"}}
        [:> Input {:value (:anesthesia-type plan-details)
                   :placeholder "请输入麻醉方式"
                   :onChange #(rf/dispatch [::events/update-anesthesia-plan-field :anesthesia-type (-> % .-target .-value)])}]]
       [:> Form.Item {:label "术前医嘱" :name :preoperative-instructions :style {:gridColumn "span 4"}}
        [:> Input.TextArea {:value (:preoperative-instructions plan-details)
                            :rows 3
                            :placeholder "请输入术前医嘱，例如禁食水时间、特殊准备等"
                            :onChange #(rf/dispatch [::events/update-anesthesia-plan-field :preoperative-instructions (-> % .-target .-value)])}]]]]]))

;; 辅助函数，用于显示签名和日期
(defn- signature-and-date-card []
  (let [patient-details @(rf/subscribe [::subs/selected-patient-raw-details])
        assessment-date (or (get-in patient-details [:assessment_data :assessment-date]) ; 尝试从评估数据中获取
                            (:updated_at patient-details) ; API 患者对象的更新时间
                            (js/Date.now))] ; 最后回退到当前时间
    [custom-styled-card
     [:> icons/SaveOutlined {:style {:marginRight "8px"}}]
     "麻醉医师签名及日期"
     "#fff0f6" ; Header background color
     [:> Descriptions {:bordered true :column 2 :size "small"}
      [:> Descriptions.Item {:label "麻醉医师签名"} [:input {:type "text" :class "ant-input ant-input-sm" :placeholder "在此签名或记录医师姓名"}]]
      [:> Descriptions.Item {:label "评估日期"} (utils/format-date assessment-date "YYYY-MM-DD HH:mm")]]]))

;; 辅助函数，用于显示备注信息
(defn- remarks-card []
  (let [notes @(rf/subscribe [::subs/assessment-notes])]
    [custom-styled-card
     [:> icons/MessageOutlined {:style {:marginRight "8px"}}]
     "备注信息"
     "#fffaf0" ; Header background color
     [:> Input.TextArea {:rows 3
                         :defaultValue (or notes "")
                         :placeholder "备注信息（如有特殊情况请在此注明）"
                         :onChange (fn [e] (rf/dispatch [::events/update-assessment-notes (.. e -target -value)]))}]]))

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
        patient-details @(rf/subscribe [::subs/selected-patient-raw-details])
        patient-name (if patient-details
                       (or (get-in patient-details [:assessment_data :basic-info :name])
                           (:name patient-details) ; Fallback to root name if basic_info name is not there
                           "未知患者")
                       "未知患者")
        patient-status (when patient-details (get patient-details :status "待评估"))] ; Default to "待评估" if status is nil
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
