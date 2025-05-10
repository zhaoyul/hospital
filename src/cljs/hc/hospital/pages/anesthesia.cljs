(ns hc.hospital.pages.anesthesia
  "麻醉管理, 医生补充患者自己填写的评估报告, 最终评估患者的情况, 判断是否可以麻醉"
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.utils :as utils]
            [taoensso.timbre :as timbre]
            ["@ant-design/icons" :as icons :refer [SyncOutlined QrcodeOutlined SaveOutlined UserOutlined HeartOutlined
                                                   DeleteOutlined EyeOutlined PaperClipOutlined UploadOutlined
                                                   FileTextOutlined ExperimentOutlined EditOutlined MessageOutlined]] ; Added more icons
            [hc.hospital.components.form-components :as form-comp]
            ;; 确保 antd/Form 等组件已引入
            ["antd" :refer [Card Content Collapse Space Text Col Radio Row DatePicker Tag  Descriptions Empty Button Input InputNumber Select Form Layout Tooltip Upload]]))


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
       :value (if (= assessment-status "all") nil assessment-status) ; "all" 时显示 placeholder
       :allowClear true
       :onChange #(rf/dispatch [::events/set-assessment-status-filter (or % "all")])
       :options assessment-status-options}]

     ;; 搜索框
     [:> Input.Search
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


;; 辅助函数，用于显示患者基本信息
(defn- patient-info []
  (let [editable-info @(rf/subscribe [::subs/selected-patient-assessment-forms-data])]
    (if (seq editable-info)
      [:> Form {:layout "vertical"
                  :initialValues editable-info}
       [:div {:style {:display "grid"
                      :gridTemplateColumns "repeat(4, 1fr)" ; 4 列
                      :gap "0px 16px"}} ; 列间距

        [:> Form.Item {:label "门诊号" :name :outpatient-number ; :name 用于 Form 自动关联
                         :rules [{:required true :message "请输入门诊号!"}]} ; 示例：添加校验规则
         [:> Input {:value (:outpatient-number editable-info) ; 显式绑定 value
                      :placeholder "请输入门诊号"
                      :onChange #(rf/dispatch [::events/update-patient-form-field :outpatient-number (-> % .-target .-value)])}]]

        [:> Form.Item {:label "姓名" :name :name}
         [:> Input {:value (:name editable-info)
                      :placeholder "请输入姓名"
                      :onChange #(rf/dispatch [::events/update-patient-form-field :name (-> % .-target .-value)])}]]

        [:> Form.Item {:label "性别" :name :sex}
         [:> Select {:value (:sex editable-info)
                       :placeholder "请选择性别"
                       :onChange #(rf/dispatch [::events/update-patient-form-field :sex %])
                       :options [{:value "男" :label "男"}
                                 {:value "女" :label "女"}
                                 {:value "其他" :label "其他"}]}]]

        [:> Form.Item {:label "年龄" :name :age}
         [:> InputNumber {:value (:age editable-info)
                             :placeholder "岁"
                             :min 0
                             :style {:width "100%"}
                             :addonAfter "岁"
                             :onChange #(rf/dispatch [::events/update-patient-form-field :age %])}]]

        [:> Form.Item {:label "病区" :name :department
                         :style {:gridColumn "span 2"}} ; 占据两列
         [:> Input {:value (:department editable-info)
                      :placeholder "请输入病区"
                      :onChange #(rf/dispatch [::events/update-patient-form-field :department (-> % .-target .-value)])}]]

        [:> Form.Item {:label "电子健康卡号" :name :health-card-number
                         :style {:gridColumn "span 2"}} ; 占据两列
         [:> Input {:value (:health-card-number editable-info)
                      :placeholder "请输入电子健康卡号 (可选)"
                      :onChange #(rf/dispatch [::events/update-patient-form-field :health-card-number (-> % .-target .-value)])}]]

        [:> Form.Item {:label "术前诊断" :name :diagnosis
                         :style {:gridColumn "span 4"}} ; 占据四列
         [:> Input.TextArea {:value (:diagnosis editable-info)
                          :placeholder "请输入术前诊断"
                          :rows 2
                          :onChange #(rf/dispatch [::events/update-patient-form-field :diagnosis (-> % .-target .-value)])}]]

        [:> Form.Item {:label "拟施手术" :name :planned-surgery ; 假设API返回的字段是 :planned-surgery
                         :style {:gridColumn "span 4"}} ; 占据四列
         [:> Input.TextArea {:value (:planned-surgery editable-info) ; 或者 (:type editable-info) 取决于你的数据结构
                          :placeholder "请输入拟施手术"
                          :rows 2
                          :onChange #(rf/dispatch [::events/update-patient-form-field :planned-surgery (-> % .-target .-value)])}]]]]
      [:> Empty {:description "请先选择患者或患者无基本信息可编辑"}])))

;; 辅助函数，用于显示一般情况
(defn- general-condition []
  (let [exam-data @(rf/subscribe [::subs/doctor-form-physical-examination])
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
    (if (some? exam-data) ; 确保 exam-data 不是 nil，空 map {} 也是有效的
      [:> Form {:layout "vertical"}
       ;; 第一部分：身高、体重、精神状态、活动能力
       [:div {:key "vital-signs-group-1"}
        [:h4 {:style {:marginBottom "12px" :fontSize "14px" :fontWeight "bold"}}
         (r/as-element [:> HeartOutlined {:style {:marginRight "8px" :color "#1890ff"}}])
         "生命体征"]
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
        [:h4 {:style {:marginBottom "12px" :fontSize "14px" :fontWeight "bold"}}
         (r/as-element [:> HeartOutlined {:style {:marginRight "8px" :color "#1890ff"}}])
         "生命体征"]
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
                              :onChange #(rf/dispatch [::events/update-doctor-form-physical-examination-field :spo2 %])}]]]]]

      [:> Empty {:description "暂无一般情况信息或未选择患者"}])))


(defn- medical-summary []
  (let [summary-data @(rf/subscribe [::subs/medical-summary-data])] ; 假设订阅 medical-summary-data
    [:> Form {:layout "vertical" :initialValues summary-data}
     [:> Descriptions {:bordered true :column 1 }
      ;; 第一部分：过敏史和生活习惯
      [:> Descriptions.Item {:label (r/as-element
                                     [:div {:style {:fontWeight "500"}}
                                      [:div "病情摘要"]
                                      [:div "(病史、体检及"]
                                      [:div "辅助检查)"]])}

       [:div
        ;; 过敏史
        [:div {:style {:marginBottom "24px"}}
         [:h4 {:style {:marginBottom "12px" :fontSize "14px" :fontWeight "bold"}}
          [:i {:class "fas fa-allergies" :style {:marginRight "8px" :color "#fa8c16"}}] "过敏史"]
         [:> Row {:gutter 16}
          [:> Col {:span 24}
           [:> Form.Item {:name [:allergy :has] :label "过敏史：" :label-col {:span 24} :wrapper-col {:span 24} :style {:marginBottom "8px"}}
            [:> Radio.Group {:value (get-in summary-data [:allergy :has])
                             :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :has] (-> % .-target .-value)])}
             [:> Radio {:value "no"} "无"]
             [:> Radio {:value "yes"} "有"]]]]
          (when (= (get-in summary-data [:allergy :has]) "yes")
            [:<>
             [:> Col {:span 12}
              [:> Form.Item {:name [:allergy :allergen] :label "过敏原：" :label-col {:span 24}}
               [:> Input {:value (get-in summary-data [:allergy :allergen])
                          :placeholder "请输入过敏原"
                          :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :allergen] (-> % .-target .-value)])}]]]
             [:> Col {:span 12}
              [:> Form.Item {:name [:allergy :last-reaction-date] :label "最近发生过敏时间：" :label-col {:span 24}}
               [:> DatePicker {:value (utils/to-moment (get-in summary-data [:allergy :last-reaction-date]))
                               :style {:width "100%"}
                               :placeholder "请选择日期"
                               :onChange #(rf/dispatch [::events/update-medical-summary-field [:allergy :last-reaction-date] (utils/date->iso-string %)])}]]]])]]]

       ;; 生活习惯
       [:div
        [:h4 {:style {:marginBottom "12px" :fontSize "14px" :fontWeight "bold"}}
         [:i {:class "fas fa-smoking" :style {:marginRight "8px" :color "#722ed1"}}] "生活习惯"]
        ;; 吸烟史
        [:> Row {:gutter 16 :align "bottom"}
         [:> Col {:span 24}
          [:> Form.Item {:name [:habits :smoking :has] :label "吸烟史：" :label-col {:span 24} :wrapper-col {:span 24} :style {:marginBottom "8px"}}
           [:> Radio.Group {:value (get-in summary-data [:habits :smoking :has])
                            :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :has] (-> % .-target .-value)])}
            [:> Radio {:value "no"} "无"]
            [:> Radio {:value "yes"} "有"]]]]
         (when (= (get-in summary-data [:habits :smoking :has]) "yes")
           [:<>
            [:> Col
             [:> Form.Item {:name [:habits :smoking :years] :label "吸烟"}
              [:> InputNumber {:value (get-in summary-data [:habits :smoking :years])
                               :min 0
                               :addonAfter "年"
                               :style {:width "100px"}
                               :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :years] %])}]]]
            [:> Col
             [:> Form.Item {:name [:habits :smoking :per-day] :label "每天"}
              [:> InputNumber {:value (get-in summary-data [:habits :smoking :per-day])
                               :min 0
                               :addonAfter "支"
                               :style {:width "100px"}
                               :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :smoking :per-day] %])}]]]] )]
        ;; 饮酒史
        [:> Row {:gutter 16 :align "bottom" :style {:marginTop "8px"}}
         [:> Col {:span 24}
          [:> Form.Item {:name [:habits :drinking :has] :label "饮酒史：" :label-col {:span 24} :wrapper-col {:span 24} :style {:marginBottom "8px"}}
           [:> Radio.Group {:value (get-in summary-data [:habits :drinking :has])
                            :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :has] (-> % .-target .-value)])}
            [:> Radio {:value "no"} "无"]
            [:> Radio {:value "yes"} "有"]]]]
         (when (= (get-in summary-data [:habits :drinking :has]) "yes")
           [:<>
            [:> Col
             [:> Form.Item {:name [:habits :drinking :years] :label "饮酒"}
              [:> InputNumber {:value (get-in summary-data [:habits :drinking :years])
                               :min 0
                               :addonAfter "年"
                               :style {:width "100px"}
                               :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :years] %])}]]]
            [:> Col
             [:> Form.Item {:name [:habits :drinking :per-day] :label "每天"}
              [:> InputNumber {:value (get-in summary-data [:habits :drinking :per-day])
                               :min 0
                               :addonAfter "ml"
                               :style {:width "100px"}
                               :onChange #(rf/dispatch [::events/update-medical-summary-field [:habits :drinking :per-day] %])}]]]] )]]]]


     ;; 第二部分：体格检查 (心脏、肺脏等)
     ;; 使用一个辅助函数来创建这些重复的条目
     (letfn [(physical-exam-item [field-key label-text]
               (let [path [:physical-exam field-key]
                     status-path (conj path :status)
                     notes-path (conj path :notes)
                     current-status (get-in summary-data status-path "normal")]
                 [:> Descriptions.Item {:label label-text}
                  [:> Form.Item {:name status-path :style {:marginBottom "8px"}}
                   [:> Radio.Group {:value current-status
                                    :onChange #(rf/dispatch [::events/update-medical-summary-field status-path (-> % .-target .-value)])}
                    [:> Radio {:value "normal"} "正常"]
                    [:> Radio {:value "abnormal"} "异常"]]]
                  (when (= current-status "abnormal")
                    [:> Form.Item {:name notes-path}
                     [:> Input {:value (get-in summary-data notes-path)
                                :placeholder "请补充异常描述"
                                :onChange #(rf/dispatch [::events/update-medical-summary-field notes-path (-> % .-target .-value)])}]])]))]
       ;; 体格检查项 - 每行两个
       ;; 需要手动将它们配对放入 Descriptions.Item 中，或者调整 Descriptions 的 column 设置
       ;; 这里为了简单，我们每项单独一行，若要严格按截图，需要更复杂的布局或调整 Descriptions
       ;; 为了模仿截图的左右两栏，我们可以用 Row/Col 或者嵌套 Descriptions
       ;; 这里我们用 Descriptions column={2}
       ;; (为了简洁，下面先按单列写，后续可优化为多列)
       ;; 更新：参照截图，用 Descriptions column=2 结构
       (let [physical-exam-fields [[:heart "3.13 心脏"] [:lungs "3.14 肺脏"]
                                   [:airway "3.15 气道"] [:teeth "3.16 牙齿"]
                                   [:spine-limbs "3.17 脊柱四肢"] [:neuro "3.18 神经"]]]
         (for [pair (partition 2 physical-exam-fields)]
           ^{:key (first (first pair))}
           [:> Descriptions {:bordered false :column 2 :style {:marginBottom "-1px"}} ; 内嵌 descriptions
            (physical-exam-item (first (first pair)) (second (first pair)))
            (physical-exam-item (first (second pair)) (second (second pair)))])))


     ;; 第三部分：相关辅助检查检验结果
     [:> Descriptions.Item {:label [:div {:style {:fontWeight "500"}} "相关辅助检查检验结果"]}
      [:div
       [:h4 {:style {:marginBottom "12px" :fontSize "14px" :fontWeight "bold"}}
        [:i {:class "fas fa-file-medical" :style {:marginRight "8px" :color "#1890ff"}}] "检查报告"]
       [:> Row {:gutter 16 :align "middle" :style {:marginBottom "8px"}}
        [:> Col [:span "肺功能："]
         [:> Upload {:name [:aux-exams :pulmonary-function-files]
                     ;; :fileList (get-in summary-data [:aux-exams :pulmonary-function-files])
                     :beforeUpload (fn [_] false) ; 阻止自动上传，手动处理
                     :onChange (fn [info] (rf/dispatch [::events/handle-file-upload [:aux-exams :pulmonary-function-files] info]))}
          [:> Button {:icon (r/as-element [:> UploadOutlined])} "上传附件"]]]
        [:> Col [:span "心脏彩超："]
         [:> Upload {:name [:aux-exams :cardiac-echo-files]
                     :beforeUpload (fn [_] false)
                     :onChange (fn [info] (rf/dispatch [::events/handle-file-upload [:aux-exams :cardiac-echo-files] info]))}
          [:> Button {:icon (r/as-element [:> UploadOutlined])} "上传附件"]]]
        [:> Col [:span "心电图："]
         [:> Upload {:name [:aux-exams :ecg-files]
                     :beforeUpload (fn [_] false)
                     :onChange (fn [info] (rf/dispatch [::events/handle-file-upload [:aux-exams :ecg-files] info]))}
          [:> Button {:icon (r/as-element [:> UploadOutlined])} "上传附件"]]]]
       ;; 文件列表显示 (示例，具体实现依赖 ::events/handle-file-upload 和数据结构)
       (let [files (get-in summary-data [:aux-exams :ecg-files] [])] ; 示例仅显示心电图文件
         (when (seq files)
           [:div {:class "ant-upload-list ant-upload-list-text" :style {:marginTop "8px"}}
            (for [file files]
              ^{:key (:uid file)}
              [:div {:class "ant-upload-list-item ant-upload-list-item-done"}
               [:div {:class "ant-upload-list-item-info"}
                [:span {:class "ant-upload-span"}
                 [:> PaperClipOutlined {:style {:marginRight "8px" :color "#1890ff"}}]
                 [:a {:href (:url file) :target "_blank" :rel "noopener noreferrer" :class "ant-upload-list-item-name" :title (:name file)} (:name file)]
                 [:span {:class "ant-upload-list-item-card-actions"}
                  [:> Tooltip {:title "预览文件"}
                   [:button {:type "button" :class "ant-btn ant-btn-text ant-btn-sm ant-btn-icon-only"
                             :onClick #(js/window.open (:url file) "_blank")}
                    [:> EyeOutlined]]]
                  [:> Tooltip {:title "删除文件"}
                   [:button {:type "button" :class "ant-btn ant-btn-text ant-btn-sm ant-btn-icon-only"
                             :onClick #(rf/dispatch [::events/remove-uploaded-file [:aux-exams :ecg-files] (:uid file)])}
                    [:> DeleteOutlined]]]]]]])]))]]


     ;; 第四部分：ASA分级和心功能分级
     ;; 为了实现截图的两列效果，这里可以包裹在一个 Descriptions.Item 中，内部使用 Row/Col
     [:> Descriptions.Item {:label nil :span 1} ;; 占满整行，移除外部 label
      [:> Row {:gutter 16}
       [:> Col {:span 12}
        [:> Form.Item {:label "ASA分级" :name :asa-grade :label-col {:span 6} :wrapper-col {:span 18}}
         [:> Select {:value (:asa-grade summary-data)
                     :style {:width "100%"}
                     :placeholder "请选择ASA分级"
                     :options (for [i (range 1 7)] {:value (str "ASA " (utils/to-roman i) "级") :label (str "ASA " (utils/to-roman i) "级")})
                     :onChange #(rf/dispatch [::events/update-medical-summary-field :asa-grade %])}]]]
       [:> Col {:span 12}
        [:> Form.Item {:label "心功能分级(NYHA)" :name :nyha-grade :label-col {:span 10} :wrapper-col {:span 14}}
         [:> Select {:value (:nyha-grade summary-data)
                     :style {:width "100%"}
                     :placeholder "请选择NYHA分级"
                     :options (for [i (range 1 5)] {:value (str "NYHA " (utils/to-roman i) "级") :label (str "NYHA " (utils/to-roman i) "级")})
                     :onChange #(rf/dispatch [::events/update-medical-summary-field :nyha-grade %])}]]]]]


     ;; 第五部分：拟行麻醉方案和监测项目
     [:> Descriptions.Item {:label nil :span 1}
      [:> Row {:gutter 16}
       [:> Col {:span 12}
        [:> Form.Item {:label "拟行麻醉方案" :name :anesthesia-plan :label-col {:span 6} :wrapper-col {:span 18}}
         [:> Select {:mode "tags"
                     :value (:anesthesia-plan summary-data)
                     :style {:width "100%"}
                     :placeholder "请选择或输入麻醉方案"
                     :options (->> ["全身麻醉" "气管插管" "静脉复合麻醉" "椎管内麻醉" "神经阻滞"]
                                   (map (fn [item] {:value item :label item})))
                     :onChange #(rf/dispatch [::events/update-medical-summary-field :anesthesia-plan %])}]]]
       [:> Col {:span 12}
        [:> Form.Item {:label "监测项目" :name :monitoring-items :label-col {:span 6} :wrapper-col {:span 18}}
         [:> Select {:mode "tags"
                     :value (:monitoring-items summary-data)
                     :style {:width "100%"}
                     :placeholder "请选择或输入监测项目"
                     :options (->> ["常规监测" "有创动脉压" "中心静脉压" "麻醉深度监测" "肌松监测"]
                                   (map (fn [item] {:value item :label item})))
                     :onChange #(rf/dispatch [::events/update-medical-summary-field :monitoring-items %])}]]]]]


     ;; 第六部分：特殊技术
     [:> Descriptions.Item {:label "特殊技术"}
      [:> Form.Item {:name :special-techniques :style {:marginBottom 0}}
       [:> Input {:value (:special-techniques summary-data)
                  :placeholder "如有特殊技术要求请在此注明"
                  :onChange #(rf/dispatch [::events/update-medical-summary-field :special-techniques (-> % .-target .-value)])}]]]]))

;; 辅助函数，用于显示术前麻醉医嘱（可编辑表单）
(defn- preoperative-orders []
  (let [plan-details @(rf/subscribe [::subs/anesthesia-plan-details])]
    [:> Form {:layout "vertical"}
     [:div {:style {:display "grid"
                    :gridTemplateColumns "repeat(4, 1fr)"
                    :gap "16px"}}

      ;; 术前麻醉医嘱（跨两列）
      [:> Form.Item {:label "术前麻醉医嘱"
                     :name [:medications :premedication]
                     :style {:gridColumn "span 2"}}
       [:> Input.TextArea {:value (get-in plan-details [:medications :premedication])
                           :rows 2
                           :placeholder "请输入术前用药医嘱"
                           :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                                    [:medications :premedication]
                                                    (-> % .-target .-value)])}]]

      ;; 术前时间和禁食（一行两列）
      [:> Form.Item {:label "术前"
                     :name [:fasting_time :hours]
                     :style {:marginBottom "8px"}}
       [:> InputNumber {:value (get-in plan-details [:fasting_time :hours])
                        :min 0
                        :addonAfter "小时"
                        :style {:width "100%"}
                        :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                                 [:fasting_time :hours] %])}]]

      [:> Form.Item {:label "小时禁食"
                     :name [:fasting :description]
                     :style {:marginBottom "8px"}}
       [:> Input {:value (get-in plan-details [:fasting :description])
                  :placeholder "禁食说明"
                  :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                           [:fasting :description]
                                           (-> % .-target .-value)])}]]

      ;; 术日晨继续应用药物（跨两列）
      [:> Form.Item {:label "术日晨继续应用药物"
                     :name [:medications :continue-on-surgery-day]
                     :style {:gridColumn "span 2"}}
       [:> Input.TextArea {:value (get-in plan-details [:medications :continue-on-surgery-day])
                           :rows 2
                           :placeholder "请输入术日晨继续应用药物"
                           :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                                    [:medications :continue-on-surgery-day]
                                                    (-> % .-target .-value)])}]]

      ;; 需进一步检查（跨两列）
      [:> Form.Item {:label "需进一步检查"
                     :name [:further-checks-needed]
                     :style {:gridColumn "span 2"}}
       [:> Input.TextArea {:value (get-in plan-details [:further-checks-needed])
                           :rows 2
                           :placeholder "请输入需要进一步检查的项目"
                           :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                                    [:further-checks-needed]
                                                    (-> % .-target .-value)])}]]

      ;; 术时麻醉注意事项（跨两列）
      [:> Form.Item {:label "术时麻醉注意事项"
                     :name [:intraoperative-notes]
                     :style {:gridColumn "span 2"}}
       [:> Input.TextArea {:value (get-in plan-details [:intraoperative-notes])
                           :rows 2
                           :placeholder "请输入术时麻醉注意事项"
                           :onChange #(rf/dispatch [::events/update-anesthesia-plan-field
                                                    [:intraoperative-notes]
                                                    (-> % .-target .-value)])}]]]
     ]))

;; 辅助函数，用于显示签名和日期
(defn- signature-and-date []
  (let [patient-details @(rf/subscribe [::subs/selected-patient-raw-details])
        assessment-date (or (get-in patient-details [:assessment_data :assessment-date]) ; 尝试从评估数据中获取
                            (:updated_at patient-details) ; API 患者对象的更新时间
                            (js/Date.now))] ; 最后回退到当前时间
    [:> Descriptions {:bordered true :column 2 :size "small"}
     [:> Descriptions.Item {:label "麻醉医师签名"} [:input {:type "text" :class "ant-input ant-input-sm" :placeholder "在此签名或记录医师姓名"}]]
     [:> Descriptions.Item {:label "评估日期"} (utils/format-date assessment-date "YYYY-MM-DD HH:mm")]]))

;; 辅助函数，用于显示备注信息
(defn- remarks []
  (let [notes @(rf/subscribe [::subs/assessment-notes])]
    [:> Input.TextArea {:rows 3
                     :value (or notes "")
                     :placeholder "备注信息（如有特殊情况请在此注明）"
                     :onChange (fn [e] (rf/dispatch [::events/update-assessment-notes (.. e -target -value)]))}]))

(defn- assessment []
  (let [current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    (if current-patient-id
      ;; 有选择患者时的视图
      [:div {:style {:height "calc(100vh - 64px)" :display "flex" :flexDirection "column"}}
       ;; 评估内容区域 - 可滚动
       [:div {:style {:flexGrow 1 :overflowY "auto" :padding "0 8px"}}
        [:> Card {:title "麻醉评估总览"
                  :variant "borderless"
                  :style {:marginBottom "12px"}}
         [:div
          [:> Card {:title (r/as-element [:span [:> icons/UserOutlined {:style {:marginRight "8px"}}] "患者信息"])
                    :type "inner" :style {:marginBottom "12px"}}
           [patient-info]]

          [:> Card {:title (r/as-element [:span [:> icons/HeartOutlined {:style {:marginRight "8px"}}] "一般情况"])
                    :type "inner" :style {:marginBottom "12px"}}
           [general-condition]]

          [:> Card {:title (r/as-element [:span [:> icons/FileTextOutlined {:style {:marginRight "8px"}}] "病情摘要（病史、体检及辅助检查）"])
                    :type "inner" :style {:marginBottom "12px"}}
           [medical-summary]]

          [:> Card {:title (r/as-element [:span [:> icons/ExperimentOutlined {:style {:marginRight "8px"}}] "术前麻醉医嘱"])
                    :type "inner" :style {:marginBottom "12px"}}
           [preoperative-orders]]

          [:> Card {:title (r/as-element [:span [:> icons/EditOutlined {:style {:marginRight "8px"}}] "麻醉医师签名及日期"])
                    :type "inner" :style {:marginBottom "12px"}}
           [signature-and-date]]

          [:> Card {:title (r/as-element [:span [:> icons/MessageOutlined {:style {:marginRight "8px"}}] "备注信息"])
                    :type "inner" :style {:marginBottom "44px"}}
           [remarks]]]]]

       ;; 固定在底部的保存按钮区域
       [:div {:style {:padding "10px 0"
                      :background "white"
                      :borderTop "1px solid #f0f0f0"
                      :textAlign "center"
                      :position "sticky"
                      :bottom 0}}
        [:> Button {:type "primary"
                    :size "large"
                    :icon (r/as-element [:> icons/SaveOutlined])
                    :on-click #(rf/dispatch [::events/save-final-assessment])}
         "保存评估结果"]]]

      ;; 无选择患者时的空状态
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100%"}}
       [:> Empty {:description "请从左侧选择一位患者开始评估"}]])))

(defn anesthesia-content []
  [:> Layout.Content {:style {:padding "0" :margin 0 :minHeight 280 :overflow "hidden" :display "flex" :flexDirection "row"}}
   ;; 左侧患者列表区域
   [:> Card {:style {:width "350px"
                     :minWidth "300px"
                     :height "calc(100vh - 64px)" ; 假设顶部导航栏高度为 64px
                     :borderRight "1px solid #f0f0f0"
                     :display "flex"
                     :flexDirection "column"
                     :padding "0"}
             }

    ;; 患者列表主体
    [:div {:style {:flexGrow 1 :overflowY "auto"}}
     [patient-list-panel]]]

   ;; 右侧评估详情区域
   [:div {:style {:flexGrow 1 :background "#f0f2f5" :overflow "hidden"}}
    [assessment]]])
