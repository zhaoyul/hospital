(ns hc.hospital.pages.anesthesia
  "麻醉管理, 医生补充患者自己填写的评估报告, 最终评估患者的情况, 判断是否可以麻醉"
  (:require
   ["@ant-design/icons" :as icons :refer [CheckCircleOutlined
                                          ClockCircleOutlined
                                          CloseCircleOutlined EditOutlined
                                          FileTextOutlined HeartOutlined
                                          MedicineBoxOutlined MessageOutlined
                                          PrinterOutlined ProfileOutlined
                                          QrcodeOutlined SaveOutlined
                                          SolutionOutlined SyncOutlined
                                          UploadOutlined UserOutlined]]
   ["dayjs" :as dayjs] ; Added Image, Modal
   ;; 确保 antd/Form 等组件已引入
   ["antd" :refer [Button Card Col DatePicker Descriptions Empty Form Input
                   InputNumber Layout Modal Radio Row Select Space Tag Upload]] ; Removed Tooltip as it's not used, Added Checkbox
   [hc.hospital.events :as events]
   [hc.hospital.pages.assessment-cards :as acards]
   [hc.hospital.subs :as subs]
   [hc.hospital.ui-helpers :refer [custom-styled-card]]
   [hc.hospital.utils :as utils]
   [hc.hospital.form-utils :as form-utils] ; Added form-utils
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs] ; Added assessment-specs
   [malli.core :as m] ; Added malli.core
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react" :as React] ; Added React for useEffect
   [taoensso.timbre :as timbre]
   ["signature_pad" :as SignaturePad])) ; Added SignaturePad & ui-helpers require

;; Define common grid style maps and helper function
(def ^:private grid-style-4-col
  {:display "grid", :gridTemplateColumns "repeat(4, 1fr)", :gap "0px 16px"})

(defn ^:private grid-col-span-style [span]
  {:gridColumn (str "span " span)})


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
           [:> UserOutlined {:style {:marginRight "8px" :fontSize "16px"}}]
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

(defn- patient-info-card "显示患者基本信息" [props]
  (let [{:keys [report-form-instance-fn]} props
        basic-info @(rf/subscribe [::subs/canonical-basic-info])
        patient-id (get basic-info :门诊号) ; Used for keying the form and useEffect dep
        [form] (Form.useForm)]

    (React/useEffect ; Changed to React/useEffect
     (fn []
       (when report-form-instance-fn
         (report-form-instance-fn :基本信息 form)) ; Register this form with key :基本信息
       js/undefined)
     #js [form report-form-instance-fn patient-id]) ; Dependencies

    [custom-styled-card
     [:> UserOutlined {:style {:marginRight "8px"}}]
     "患者基本信息"
     "#e6fffb" ; Header background color
     (if (seq basic-info)
       [:div ; Main container for card content
        ;; Display-only information (can be styled as before)
        [:div {:style {:display "flex" :flex-wrap "wrap" :align-items "center" :padding "10px 0" :marginBottom "10px"}}
         [:span {:style {:margin-right "16px" :margin-bottom "8px"}} (str "门诊号: " (get basic-info :门诊号 "未知"))]
         [:span {:style {:margin-right "16px" :margin-bottom "8px"}} (str "姓名: " (get basic-info :姓名 "未知"))]
         [:span {:style {:margin-right "16px" :margin-bottom "8px"}} (str "性别: " (get basic-info :性别 "未知"))]
         [:span {:style {:margin-right "16px" :margin-bottom "8px"}} (str "年龄: " (get basic-info :年龄 "未知") "岁")]
         [:span {:style {:margin-right "16px" :margin-bottom "8px"}} (str "病区: " (get basic-info :院区 "未知"))]
         [:span {:style {:margin-bottom "8px"}} (str "身份证号: " (get basic-info :身份证号 "无"))]]

        ;; Form for editable fields: "术前诊断" and "拟施手术"
        [:> Form {:form form
                  :layout "vertical" ; Using vertical layout for simplicity
                  :initialValues (clj->js (select-keys basic-info [:术前诊断 :拟施手术]))
                  :onFinish (fn [values]
                              (let [values-clj (js->clj values :keywordize-keys true)]
                                (rf/dispatch [::events/update-canonical-assessment-section :基本信息 values-clj])))
                  :key patient-id} ; Ensure form re-initializes when patient changes
         [:> Form.Item {:label "术前诊断" :name :术前诊断}
          [:> Input.TextArea {:placeholder "请输入术前诊断"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :术前诊断] (-> % .-target .-value)])}]]

         [:> Form.Item {:label "拟施手术" :name :拟施手术}
          [:> Input.TextArea {:placeholder "请输入拟施手术"
                              :rows 2
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :拟施手术] (-> % .-target .-value)])}]]]]
       [:> Empty {:description "请先选择患者或患者无基本信息"}])]))

(defn- general-condition-card "显示一般情况" []
  (let [basic-info-data @(rf/subscribe [::subs/canonical-basic-info]) ; Changed subscription
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
       [:> HeartOutlined {:style {:marginRight "8px"}}]
       "一般情况"
       "#f6ffed" ; Header background color
       (if (seq basic-info-data) ; Check if data is not empty
         [:> Form {:layout "horizontal" :labelCol {:sm {:span 24} :md {:span 10}} :wrapperCol {:sm {:span 24} :md {:span 14}} :labelAlign "left"
                   :initialValues (clj->js basic-info-data) ; Use basic-info-data
                   :key patient-id} ; Key to re-initialize form when patient changes
          ;; 第一部分：身高、体重、精神状态、活动能力
          [:div {:key "vital-signs-group-1"}
           [:div {:style (assoc grid-style-4-col :marginBottom "16px")}
            [:> Form.Item {:label "身高" :name :身高cm} ; Updated name
             [:> InputNumber {:placeholder "cm"
                              :addonAfter "cm"
                              :style {:width "100%"}
                              :min 0
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :身高cm] %])}]] ; Updated path
            [:> Form.Item {:label "体重" :name :体重kg} ; Updated name
             [:> InputNumber {:placeholder "kg"
                              :addonAfter "kg"
                              :style {:width "100%"}
                              :min 0
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :体重kg] %])}]] ; Updated path
            [:> Form.Item {:label "精神状态" :name :精神状态} ; Updated name
             [:> Select {:placeholder "请选择"
                         :style {:width "100%"}
                         :allowClear true
                         :options mental-status-options
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :精神状态] %])}]] ; Updated path
            [:> Form.Item {:label "活动能力" :name :活动能力} ; Updated name
             [:> Select {:placeholder "请选择"
                         :style {:width "100%"}
                         :allowClear true
                         :options activity-level-options
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :活动能力] %])}]]]] ; Updated path

          ;; 分隔线 (可选，如果视觉上需要)
          ;; [:> Divider {:style {:margin "0 0 16px 0"}}]

          ;; 第二部分：血压、脉搏、呼吸、体温、SpO2
          [:div {:key "vital-signs-group-2"}
           [:div {:style {:display "flex" :flexWrap "wrap" :gap "8px 24px"}} ; 增大列间距
            ;; 血压 - Temporarily commented out as per instructions
            #_[:> Form.Item {:label "血压"}
               [:> Space {:align "center"} ; Use Space as the single child
                [:> Form.Item {:name :bp_systolic :noStyle true} ; Canonical: bp_systolic
                 [:> InputNumber {:placeholder "收缩压"
                                  :min 0
                                  :style {:width "70px"}
                                  :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :bp_systolic] %])}]] ; Path would change
                [:span {:style {:margin "0 4px"}} "/"] ; Keep the separator
                [:> Form.Item {:name :bp_diastolic :noStyle true} ; Canonical: bp_diastolic
                 [:> InputNumber {:placeholder "舒张压"
                                  :min 0
                                  :style {:width "70px"}
                                  :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :bp_diastolic] %])}]] ; Path would change
                [:span {:style {:marginLeft "4px"}} "mmHg"]]]

            ;; 脉搏
            [:> Form.Item {:label "脉搏" :name :脉搏次每分} ; Updated name
             [:> InputNumber {:placeholder "次/分"
                              :addonAfter "次/分"
                              :min 0
                              :style {:width "130px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :脉搏次每分] %])}]] ; Updated path

            ;; 呼吸
            [:> Form.Item {:label "呼吸" :name :呼吸次每分} ; Updated name
             [:> InputNumber {:placeholder "次/分"
                              :addonAfter "次/分"
                              :min 0
                              :style {:width "130px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :呼吸次每分] %])}]] ; Updated path

            ;; 体温
            [:> Form.Item {:label "体温" :name :体温摄氏度} ; Updated name
             [:> InputNumber {:placeholder "°C"
                              :addonAfter "°C"
                              :precision 1
                              :step 0.1
                              :style {:width "110px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :体温摄氏度] %])}]] ; Updated path

            ;; SpO2
            [:> Form.Item {:label "SpO2" :name :SpO2百分比} ; Updated name
             [:> InputNumber {:placeholder "%"
                              :addonAfter "%"
                              :min 0 :max 100
                              :style {:width "100px"}
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :SpO2百分比] %])}]]]]] ; Updated path
         [:> Empty {:description "暂无一般情况信息或未选择患者"}])])))




(defn- render-allergy-section [medical-history]
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
       [:> DatePicker {:style {:width "100%"}
                       :format "YYYY-MM-DD"
                       :placeholder "请选择日期"
                       ;; value is handled by Form :initialValues after preprocessing
                       :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :allergy :last_reaction_date] (utils/date->iso-string %)])}]]])])

(defn- render-lifestyle-section [medical-history]
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
                   :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:medical_history :drinking :alcohol_per_day] (-> % .-target .-value)])}]]]])])

(defn- medical-history-summary-card []
  (let [raw-medical-history @(rf/subscribe [::subs/canonical-medical-history])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        medical-history-processing-schema [:map {:closed true}
                                           [:allergy [:map {:closed true}
                                                      [:last_reaction_date assessment-specs/Optional日期字符串]
                                                      [:has_history {:optional true} :boolean]
                                                      [:details {:optional true} :string]]]
                                           [:smoking {:optional true} [:map {:closed true} [:has_history {:optional true} :boolean] [:years {:optional true} :int] [:cigarettes_per_day {:optional true} :int]]]
                                           [:drinking {:optional true} [:map {:closed true} [:has_history {:optional true} :boolean] [:years {:optional true} :int] [:alcohol_per_day {:optional true} :string]]]]
        processed-medical-history (form-utils/preprocess-date-fields raw-medical-history medical-history-processing-schema)]
    [custom-styled-card
     [:> FileTextOutlined {:style {:marginRight "8px"}}]
     "病情摘要"
     "#fff7e6"
     (if (seq processed-medical-history)
       [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left"
                 :initialValues (clj->js processed-medical-history) ;; 使用预处理后的数据
                 :key patient-id}
        [render-allergy-section processed-medical-history] ;; 确保 render-allergy-section 也使用处理后的数据
        [render-lifestyle-section processed-medical-history]]
       [:> Empty {:description "请先选择患者或患者无病情摘要信息"}])]))

(defn- comorbidities-card []
  (let [comorbidities-data @(rf/subscribe [::subs/canonical-comorbidities])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])]
    (letfn [(comorbidity-item [field-key label-text]
              (let [base-path [:comorbidities field-key]
                    form-item-name [field-key :has]
                    details-form-item-name [field-key :details]
                    has-value (get-in comorbidities-data [field-key :has])]
                [:> Col {:span 12}
                 [:> Form.Item {:label label-text :name form-item-name}
                  [:> Radio.Group {
                                   :onChange #(let [val (-> % .-target .-value)]
                                                (rf/dispatch [::events/update-canonical-assessment-field base-path (assoc (get comorbidities-data field-key) :has val)])
                                                (when-not val
                                                  (rf/dispatch [::events/update-canonical-assessment-field (conj base-path :details) nil])))}
                   [:> Radio {:value true} "有"]
                   [:> Radio {:value false} "无"]]]
                 (when has-value
                   [:> Form.Item {:name details-form-item-name
                                  :noStyle true
                                  :style {:marginTop "8px"}}
                    [:> Input {
                               :placeholder "请填写具体内容"
                               :onChange #(rf/dispatch [::events/update-canonical-assessment-field (conj base-path :details) (-> % .-target .-value)])}]])]))]
      #_[custom-styled-card ;; This card is currently commented out in the original code
         [:> MedicineBoxOutlined]
         "并存疾病"
         "#f9f0ff"
         (if (seq comorbidities-data)
           (let [comorbidities-processing-schema [:map {:closed true}
                                                  [:respiratory {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:cardiovascular {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:endocrine {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:neuro_psychiatric {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:neuromuscular {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:hepatic {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:renal {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]]
                                                  [:musculoskeletal {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]] ; Added back
                                                  [:malignant_hyperthermia_fh {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]] ; Added back
                                                  [:anesthesia_surgery_history {:optional true} [:map {:closed true} [:has {:optional true} :boolean] [:details {:optional true} :string]]] ; Added back
                                                  [:special_medications {:optional true}
                                                   [:map {:closed true}
                                                    [:has_taken {:optional true} :boolean]
                                                    [:details {:optional true} :string]
                                                    [:last_dose_time assessment-specs/Optional日期时间字符串]]]]
                 processed-comorbidities (form-utils/preprocess-date-fields comorbidities-data comorbidities-processing-schema)]
             [:> Form {:layout "horizontal" :labelCol {:span 10} :wrapperCol {:span 14} :labelAlign "left"
                       :initialValues (clj->js processed-comorbidities) ;; 使用预处理后的数据
                       :key patient-id}
              [:> Row {:gutter [16 0]}
               (comorbidity-item :respiratory "呼吸系统疾病")
               (comorbidity-item :cardiovascular "心血管疾病")
               (comorbidity-item :endocrine "内分泌疾病")
               (comorbidity-item :neuro_psychiatric "神经精神疾病")
               (comorbidity-item :neuromuscular "神经肌肉疾病")
               (comorbidity-item :hepatic "肝脏疾病")
               (comorbidity-item :renal "肾脏疾病")
               (comorbidity-item :musculoskeletal "关节骨骼系统")
               (comorbidity-item :malignant_hyperthermia_fh "家族恶性高热史")
               (comorbidity-item :anesthesia_surgery_history "既往麻醉、手术史")

               (let [base-path [:comorbidities :special_medications]
                     form-item-base [:special_medications]
                     has-taken-path (conj base-path :has_taken)
                     details-path (conj base-path :details)
                     last-dose-time-path (conj base-path :last_dose_time)
                     has-taken-value (get-in processed-comorbidities [:special_medications :has_taken])]
                 [:> Col {:span 24}
                  [:> Form.Item {:label "使用的特殊药物" :name (conj form-item-base :has_taken)}
                   [:> Radio.Group {
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
                       [:> Input {:placeholder "药物名称及剂量"
                                  :style {:marginBottom "8px"}
                                  :onChange #(rf/dispatch [::events/update-canonical-assessment-field details-path (-> % .-target .-value)])}]]
                      [:> Form.Item {:name (conj form-item-base :last_dose_time) :label "最近用药时间" :labelCol {:span 6} :wrapperCol {:span 18}}
                       [:> DatePicker {:showTime true
                                       :format "YYYY-MM-DD HH:mm"
                                       :placeholder "选择日期和时间"
                                       :style {:width "100%"}
                                       ;; value handled by Form :initialValues
                                       :onChange #(rf/dispatch [::events/update-canonical-assessment-field last-dose-time-path (utils/datetime->string % "YYYY-MM-DD HH:mm")])}]]])]] ) ] ] )
           [:> Empty {:description "暂无并存疾病信息或未选择患者"}])])))

(defn- physical-examination-card []
  (let [phys-exam-data @(rf/subscribe [::subs/canonical-physical-examination])
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])]
    (letfn [(exam-item [field-key label-text]
              (let [base-path [:physical_examination field-key] ; Path for dispatch, e.g. [:physical_examination :heart]
                    form-item-name [field-key :status] ; Path for Form.Item name, e.g. [:heart :status]
                    notes-form-item-name [field-key :notes]
                    current-status-val (get-in phys-exam-data [field-key :status] "normal")] ; Default to "normal" string as per original logic
                [:> Col {:span 12}
                 [:> Form.Item {:label label-text :name form-item-name}
                  [:> Radio.Group {;; :value current-status-val ; Let Form handle
                                   :onChange #(let [val (-> % .-target .-value)]
                                                (rf/dispatch [::events/update-canonical-assessment-field base-path (assoc (get phys-exam-data field-key) :status val)])
                                                (when (= val "normal") 
                                                  (rf/dispatch [::events/update-canonical-assessment-field (conj base-path :notes) nil])))}
                   [:> Radio {:value "normal"} "正常"]
                   [:> Radio {:value "abnormal"} "异常"]]
                  (when (= current-status-val "abnormal")
                    [:> Form.Item {:name notes-form-item-name
                                   :noStyle true
                                   :style {:marginTop "8px"}}
                     [:> Input {:placeholder "请描述异常情况"
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
                 :initialValues {:辅助检查备注 aux-notes} ; Updated name
                 :key patient-id}
        [:> Form.Item {:label "上传检查文件 (如ECG, 胸片, 血常规等)"}
         [:> Upload upload-props
          [:div
           [:> UploadOutlined]
           [:div {:style {:marginTop 8}} "上传文件"]]]]
        
        [:> Form.Item {:label "其他检查结果说明" :name :辅助检查备注} ; Updated name
         [:> Input.TextArea {;; :value aux-notes ; Let Form handle
                             :placeholder "请在此记录其他重要检查结果的文字描述或总结"
                             :rows 4
                             :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:辅助检查备注] (-> % .-target .-value)])}]] ; Updated path
        
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
     [:> EditOutlined {:style {:marginRight "8px"}}]
     "术前麻醉医嘱"
     "#fff1f0" ; Header background color
     (if (seq anesthesia-plan-data) ; Check if data is not empty
       [:> Form {:layout "horizontal" :labelCol {:span 6} :wrapperCol {:span 18} :labelAlign "left"
                 :initialValues (clj->js anesthesia-plan-data)
                 :key patient-id} ; Key for re-initialization
        [:div {:style grid-style-4-col}
         [:> Form.Item {:label "ASA分级" :name :ASA分级 :style (grid-col-span-style 1)} ; Updated name
          [:> Select {;; :value (:ASA分级 anesthesia-plan-data) ; Let Form handle
                      :placeholder "请选择ASA分级"
                      :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:麻醉评估与医嘱 :ASA分级] %]) ; Updated path
                      :options (mapv (fn [i] {:value (str "ASA " i) :label (str "ASA " i)}) (range 1 7))}]]
         [:> Form.Item {:label "麻醉方式" :name :拟行麻醉方式 :style (grid-col-span-style 3)} ; Updated name
          [:> Input {;; :value (:拟行麻醉方式 anesthesia-plan-data) ; Let Form handle
                     :placeholder "请输入麻醉方式"
                     :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:麻醉评估与医嘱 :拟行麻醉方式] (-> % .-target .-value)])}]] ; Updated path
         [:> Form.Item {:label "术前医嘱" :name :术前麻醉医嘱 :style (grid-col-span-style 4)} ; Updated name
          [:> Input.TextArea {;; :value (:术前麻醉医嘱 anesthesia-plan-data) ; Let Form handle
                              :rows 3
                              :placeholder "请输入术前医嘱，例如禁食水时间、特殊准备等"
                              :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:麻醉评估与医嘱 :术前麻醉医嘱] (-> % .-target .-value)])}]]]]) ; Updated path
     [:> Empty {:description "暂无术前麻醉医嘱信息或未选择患者"}]]))

;; 新增：签名板组件
(defn signature-pad-comp []
  (let [canvas-ref (r/atom nil)
        signature-pad-instance (r/atom nil)
        init-signature-pad (fn []
                             (when @canvas-ref
                               (reset! signature-pad-instance (SignaturePad. @canvas-ref #js {:penColor "black"}))))]
    (r/create-class
     {:component-did-mount init-signature-pad
      :reagent-render
      (fn []
        [:div
         [:canvas {:ref (fn [el] (reset! canvas-ref el)) :style {:border "1px solid #eee" :width "300px" :height "150px"}}]
         [:div {:style {:marginTop "10px"}}
          [:> Button {:on-click (fn [] (when @signature-pad-instance (.clear @signature-pad-instance)))
                      :style {:marginRight "10px"}}
           "清除签名"]
          [:> Button {:type "primary"
                      :on-click (fn []
                                  (when (and @signature-pad-instance (not (.isEmpty @signature-pad-instance)))
                                    (let [signature-data (.toDataURL @signature-pad-instance)]
                                      (rf/dispatch [::events/update-signature-data signature-data]))))}
           "确认签名"]]])})))

;; 辅助函数，用于显示签名和日期
(defn- signature-and-date-card []
  (let [basic-info @(rf/subscribe [::subs/canonical-basic-info])
        assessment-updated-at (get basic-info :评估更新时间 (utils/date->iso-string (js/Date.now))) ; Updated key
        doctor-name (get basic-info :医生姓名) ; Updated key
        saved-signature-image @(rf/subscribe [::subs/doctor-signature-image])]
    [custom-styled-card
     [:> SaveOutlined {:style {:marginRight "8px"}}]
     "麻醉医师签名及日期"
     "#fff0f6" ; Header background color
     [:> Descriptions {:bordered true :column 1 :size "small"}
      [:> Descriptions.Item {:label "麻醉医师姓名"} ; Label updated
       [:> Input {:placeholder "记录医师姓名"
                  :value doctor-name
                  :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :医生姓名] (-> % .-target .-value)])}]]
      [:> Descriptions.Item {:label "麻醉医师签名"}
       (if saved-signature-image
         [:div
          [:img {:src saved-signature-image :alt "医生签名" :style {:width "200px" :height "100px" :border "1px solid #eee"}}]
          [:> Button {:style {:marginLeft "10px"}
                      :on-click (fn [] (rf/dispatch [::events/update-signature-data nil]))} ; 清除签名
           "重新签名"]]
         [signature-pad-comp {}])] ; 显示签名板
      [:> Descriptions.Item {:label "评估更新日期"}
       (utils/format-date assessment-updated-at "YYYY-MM-DD HH:mm")]]]))

;; 辅助函数，用于显示备注信息 (now part of basic_info)
(defn- remarks-card []
  (let [assessment-notes (rf/subscribe [::subs/canonical-basic-info :评估备注])] ; Updated subscription path
    [custom-styled-card
     [:> MessageOutlined {:style {:marginRight "8px"}}]
     "评估备注" ; Changed title to be more specific
     "#fffaf0" ; Header background color
     [:> Input.TextArea {:rows 4 ; Increased rows for better visibility
                         :value (timbre/spy :info (or @assessment-notes "")) ; Deref the atom from subscription
                         :placeholder "评估备注（如有特殊情况请在此注明）"
                         :onChange #(rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :评估备注] (-> % .-target .-value)])}]])) ; Updated path

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

(defn- assessment []
  (let [card-form-instances (r/atom {})
        current-patient-id @(rf/subscribe [::subs/current-patient-id])
        basic-info @(rf/subscribe [::subs/canonical-basic-info])
        patient-name (get basic-info :name "未知患者")
        patient-status (get basic-info :assessment_status "待评估")

        register-form-instance (fn [card-key form-instance]
                                 (swap! card-form-instances assoc card-key form-instance))

        save-button (fn [] ; Moved save-button inside assessment
                      [:div {:style {:padding "10px 0"
                                     :background "white"
                                     :borderTop "1px solid #f0f0f0"
                                     :textAlign "center"
                                     :position "sticky"
                                     :bottom 0
                                     :zIndex 10}} ; Ensure it's above scrolled content
                       [:> Button {:type "primary"
                                   :size "large"
                                   :icon (r/as-element [:> SaveOutlined])
                                   :onClick (fn []
                                              (let [forms-map @card-form-instances]
                                                (timbre/info "Attempting to submit all card forms. Forms found:" (count (keys forms-map)))
                                                (doseq [[card-key form-inst] forms-map]
                                                  (if form-inst
                                                    (do
                                                      (timbre/info "Submitting form for card:" card-key)
                                                      (.submit form-inst))
                                                    (timbre/warn "No form instance found for card key:" card-key)))
                                                (timbre/info "All card forms submitted, proceeding to save final assessment.")
                                                (rf/dispatch [::events/save-final-assessment-later])))} ;; 稍等一会, db修改完成后再提交
                        "保存评估结果"]])]
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
        [:f> patient-info-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/circulatory-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/respiratory-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/mental-neuromuscular-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/endocrine-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/liver-kidney-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/digestive-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/hematologic-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/immune-system-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/special-medication-history-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/special-disease-history-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/nutritional-assessment-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/pregnancy-assessment-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/surgical-anesthesia-history-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/airway-assessment-card {:report-form-instance-fn register-form-instance}]
        [:f> acards/spinal-anesthesia-assessment-card {:report-form-instance-fn register-form-instance}]
        [general-condition-card]
        [medical-history-summary-card] ; Uncommented and modified
        [comorbidities-card] ; Uncommented and modified (internally, the card itself is still #_ [] )
        ;; [physical-examination-card] ; Commented out as per instructions
        [auxiliary-tests-card]
        [preoperative-orders-card]
        [remarks-card]
        [signature-and-date-card]
        [save-button]]] ; save-button is now called as a function defined in the let block

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
