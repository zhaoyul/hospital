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
            ["antd" :refer [Collapse Descriptions Empty Button]]))


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
  )


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
    (if current-patient-id
      ;; 有选择患者时的视图
      [:div {:style {:height "calc(100vh - 64px)" :display "flex" :flexDirection "column"}}
       ;; 评估内容区域 - 可滚动
       [:div {:style {:flexGrow 1 :overflowY "auto" :padding "0 8px"}}
        [antd/card {:title "麻醉评估总览"
                    :variant "borderless"
                    :style {:marginBottom "12px"}}
         [:div
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
                      :type "inner" :style {:marginBottom "44px"}}
           [remarks-display]]]]]

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
       [:> Empty {:description "请从左侧选择一位患者开始评估" :imageStyle {:height 80}}]])))

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
   [:div {:style {:flexGrow 1 :background "#f0f2f5" :overflow "hidden"}}
    [assessment-result]]])
