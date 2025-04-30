(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]
            [hc.hospital.components.form-components :as form-comp]
            ["antd" :refer [Form]])) ; Import Form

(defn patient-list []
  (let [patients @(rf/subscribe [::subs/filtered-patients])
        current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    [:div {:style {:height "calc(100vh - 200px)" :overflowY "auto"}}
     (for [item patients]
       ^{:key (:key item)}
       [:div {:style {:padding "10px"
                      :borderBottom "1px solid #f0f0f0"
                      :display "flex"
                      :justifyContent "space-between"
                      :alignItems "center"
                      :background (when (= (:key item) current-patient-id) "#e6f7ff")
                      :cursor "pointer"}
              :onClick #(rf/dispatch [::events/select-patient (:key item)])}
        [:div {:style {:display "flex" :alignItems "center"}}
         [antd/user-outlined {:style {:marginRight "8px"}}]
         [:div
          [:div {:style {:fontWeight "bold"}} (:name item)]
          [:div {:style {:fontSize "12px" :color "gray"}}
           (str (:sex item) " " (:age item) "岁 " (:type item))]]]
        [:div {:style {:textAlign "right"}}
         [:div {:style {:fontSize "12px" :color "gray"}} (:date item)]
         [antd/tag {:color (case (:status item)
                             "待评估" "orange"
                             "已批准" "green"
                             "已暂缓" "blue"
                             "已驳回" "red"
                             "default")} (:status item)]]])]))

(defn brief-medical-history []
  (let [medical-history @(rf/subscribe [::subs/brief-medical-history])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues medical-history
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [::events/update-brief-medical-history all-values]))
                :style {:paddingBottom "24px"}}

     ;; 使用通用组件替换重复的模式
     [form-comp/yes-no-with-description {:label "既往史" :field-name-prefix "past-history"}]
     [form-comp/yes-no-with-description {:label "过敏史" :field-name-prefix "allergic-history"}]
     [form-comp/yes-no-with-description {:label "手术麻醉史" :field-name-prefix "surgery-anesthesia-history"}]
     [form-comp/yes-no-with-description {:label "怀孕" :field-name-prefix "pregnancy"}]
     [form-comp/yes-no-with-description {:label "输血史" :field-name-prefix "blood-transfusion-history"}]
     [form-comp/yes-no-with-description {:label "月经期" :field-name-prefix "menstrual-period"}]

     ;; 个人史 (Personal History) - Checkbox Group
     [antd/form-item {:name :personal-history
                      :label "个人史:"}
      [antd/checkbox-group {}
       [antd/checkbox {:value "smoke"} "烟"]
       [antd/checkbox {:value "drink"} "酒"]]]

     ;; 其他 (Other) - 使用通用复选框组件
     [form-comp/checkbox-with-conditional-input
      {:label "其他"
       :checkbox-label "其他"
       :field-name :other-desc}]]))

(defn physical-examination []
  (let [exam-data @(rf/subscribe [::subs/physical-examination])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "horizontal" ;; Changed from vertical to horizontal
                :labelCol {:span 6}  ;; Controls width of label
                :wrapperCol {:span 18} ;; Controls width of form controls
                :initialValues exam-data
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [::events/update-physical-examination all-values]))
                :style {:padding-bottom "24px"}}

           ;; 使用通用组件
     [form-comp/radio-button-group
      {:label "一般状况"
       :name :general-condition
       :options [{:value "bad" :label "差"}
                 {:value "fair" :label "尚可"}
                 {:value "average" :label "一般"}
                 {:value "good" :label "好"}]}]

           ;; 身高体重
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :height :label "身高:" :labelCol {:span 8} :wrapperCol {:span 16}}
        [antd/input-number {:style {:width "100%"} :addonAfter "cm"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :weight :label "体重:" :labelCol {:span 8} :wrapperCol {:span 16}}
        [antd/input-number {:style {:width "100%"} :addonAfter "kg"}]]]]

           ;; 血压
     [antd/form-item {:label "BP:"}
      [antd/space-compact {:style {:width "100%"}}
       [antd/form-item {:name [:bp :systolic] :noStyle true}
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number"}]]
       [:span {:style {:display "inline-block" :width "30px" :lineHeight "32px" :textAlign "center"}} "/"]
       [antd/form-item {:name [:bp :diastolic] :noStyle true}
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number" :addonAfter "mmHg"}]]]]

           ;; 心率和呼吸
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :heart-rate :label "PR" :labelCol {:span 8} :wrapperCol {:span 16}}
        [antd/input-number {:style {:width "100%"} :addonAfter "次/分"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :respiratory-rate :label "RR" :labelCol {:span 8} :wrapperCol {:span 16}}
        [antd/input-number {:style {:width "100%"} :addonAfter "次/分"}]]]]

     ;; 体温
     [antd/form-item {:name :temperature :label "T"}
      [antd/input-number {:style {:width "100%"} :step "0.1" :addonAfter "°C"}]]

     ;; 精神状态
     [form-comp/radio-button-group
      {:label "精神行为"
       :name :mental-state
       :options [{:value "normal" :label "正常"}
                 {:value "drowsy" :label "嗜睡"}
                 {:value "coma" :label "昏迷"}
                 {:value "irritable" :label "烦躁"}
                 {:value "delirium" :label "谵妄"}
                 {:value "cognitive" :label "认知障碍"}]}]

     ;; 头颈部
     [form-comp/radio-button-group
      {:label "头颈部"
       :name :head-neck
       :options [{:value "normal" :label "无异常"}
                 {:value "scar" :label "疤痕"}
                 {:value "short_neck" :label "颈短"}
                 {:value "neck_mass" :label "颈部肿块"}
                 {:value "limited_mobility" :label "后仰困难"}]}]

     ;; 张口
     [antd/form-item {:name :mouth-opening :label "口腔: 张口"}
      [antd/input-number {:style {:width "100%"} :step "0.1" :addonAfter "cm"}]]

           ;; Mallampati 和 甲颌间距
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :mallampati-score :label "Mallampati" :labelCol {:span 8} :wrapperCol {:span 16}}
        [form-comp/radio-button-group
         {:label "Mallampati"
          :name :mallampati-score
          :options [{:value "I" :label "I"}
                    {:value "II" :label "II"}
                    {:value "III" :label "III"}
                    {:value "IV" :label "IV"}]}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :thyromental-distance :label "颏颌距离" :labelCol {:span 8} :wrapperCol {:span 16}}
        [antd/input-number {:style {:width "100%"} :step "0.1" :addonAfter "cm"}]]]]

     ;; 相关病史
     [antd/form-item {:label "相关病史:"}
      [antd/space {:direction "vertical"}
       [antd/form-item {:name [:related-history :difficult-airway] :valuePropName "checked" :noStyle true}
        [antd/checkbox "困难气道史"]]
       [antd/form-item {:name [:related-history :postoperative-nausea] :valuePropName "checked" :noStyle true}
        [antd/checkbox "术后恶心呕吐史"]]
       [antd/form-item {:name [:related-history :malignant-hyperthermia] :valuePropName "checked" :noStyle true}
        [antd/checkbox "恶性高热史"]]
       [antd/row
        [antd/form-item {:name [:related-history :other-checkbox] :valuePropName "checked" :noStyle true}
         [antd/checkbox "其他"]]
        [antd/form-item {:name [:related-history :other] :noStyle true :dependencies [[:related-history :other-checkbox]]}
         (fn [form-instance]
           (let [other-checked (.getFieldValue form-instance [:related-history :other-checkbox])]
             [antd/input {:placeholder "请输入"
                          :style {:width "300px" :marginLeft "10px"}
                          :disabled (not other-checked)}]))]]]]

     ;; 胸部
     [form-comp/radio-button-group
      {:label "胸"
       :name :chest
       :options [{:value "normal" :label "正常"}
                 {:value "barrel" :label "桶状胸"}
                 {:value "pectus_excavatum" :label "佝偻胸"}]}]]))

(defn lab-tests []
  (let [lab-data @(rf/subscribe [::subs/lab-tests])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues lab-data
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [::events/update-lab-tests all-values]))
                :style {:padding-bottom "24px"}}

     ;; 使用通用组件创建标题
     [form-comp/section-title {:title "血常规：" :margin-top "0"}]

     ;; RBC和Hct - 使用通用组件
     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "RBC" :name [:complete-blood-count :hemoglobin] :step "0.1" :unit "×10¹²/L"}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "Hct" :name [:complete-blood-count :hematocrit] :step "0.01" :unit "%"}]}]

     ;; PLT和WBC - 使用通用组件
     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "PLT" :name [:complete-blood-count :platelets] :unit "×10⁹/L"}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "WBC" :name [:complete-blood-count :wbc] :step "0.1" :unit "×10⁹/L"}]}]

     ;; 血型和Rh
     [form-comp/two-column-row
      {:left-item
       [form-comp/radio-button-group
        {:label "血型"
         :name :blood-type
         :options [{:value "A" :label "A"}
                   {:value "B" :label "B"}
                   {:value "AB" :label "AB"}
                   {:value "O" :label "O"}]}]
       :right-item
       [form-comp/radio-button-group
        {:label "Rh"
         :name :rh
         :options [{:value "negative" :label "阴性"}
                   {:value "positive" :label "阳性"}]}]}]

     ;; 凝血检查和血糖值
     [form-comp/two-column-row
      {:left-item
       [form-comp/radio-button-group
        {:label "凝血检查"
         :name :coagulation
         :options [{:value "normal" :label "正常"}
                   {:value "abnormal" :label "异常"}]}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "血糖值" :name [:biochemistry :glucose] :step "0.1" :unit "mmol/L"}]}]

     ;; 使用通用组件创建标题
     [form-comp/section-title {:title "生化指标："}]

     ;; ALT和AST - 使用通用组件
     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "ALT" :name [:biochemistry :alt] :unit "U/L"}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "AST" :name [:biochemistry :ast] :unit "U/L"}]}]

     ;; 钠和钾 - 使用通用组件
     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "钠" :name [:biochemistry :sodium] :unit "mmol/L"}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "钾" :name [:biochemistry :potassium] :step "0.1" :unit "mmol/L"}]}]

     ;; 使用通用组件创建标题
     [form-comp/section-title {:title "医学影像："}]

     ;; 心电图
     [antd/form-item {:name :ecg :label "心电图:"}
      [antd/input {:style {:width "100%"}}]]

     ;; 胸片
     [antd/form-item {:name :chest-xray :label "胸片:"}
      [antd/input {:style {:width "100%"}}]]]))

(defn assessment-result []
  [antd/card {:variant "borderless" :style {:height "calc(100vh - 180px)" :overflowY "auto"}}
   [:div {:style {:padding "0 16px"}}
    [antd/row {:gutter 16}
     ;; Left column (divided into top and bottom)
     [antd/col {:span 12}
      ;; Top section - Brief Medical History in its own Card
      [antd/card {:title "简要病史" :style {:marginBottom "16px"}}
       [:f> brief-medical-history]]

      ;; Bottom section - Lab Tests in its own Card
      [antd/card {:title "实验室检查"}
       [:f> lab-tests]]]

     ;; Right column - Physical Examination in its own Card
     [antd/col {:span 12}
      [antd/card {:title "体格检查"}
       [:f> physical-examination]]]]]])

(def menu-items
  [{:key "1" :icon (r/as-element [antd/laptop-outlined]) :label "麻醉管理"} ; Use wrapped icons
   {:key "2" :icon (r/as-element [antd/user-outlined]) :label "患者文书"}
   {:key "3" :icon (r/as-element [antd/notification-outlined]) :label "患者签到"}])

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])
        search-term @(rf/subscribe [::subs/search-term])
        date-range @(rf/subscribe [::subs/date-range])]
    [antd/layout {:style {:minHeight "100vh"}}
     [antd/sider {:width 200 :style {:background "#fff"}}
      [:div {:style {:height "32px" :margin "16px" :background "rgba(0, 0, 0, 0.2)"}}]
      [antd/menu {:mode "inline"
                  :selectedKeys [(case active-tab
                                   "patients" "1"
                                   "assessment" "2"
                                   "history" "3"
                                   "1")]
                  :onChange #(rf/dispatch [::events/set-active-tab (case %
                                                                     "1" "patients"
                                                                     "2" "assessment"
                                                                     "3" "history")])
                  :style {:height "100%" :borderRight 0}
                  :items menu-items}]]
     [antd/layout {:style {:padding "0 24px 24px"}}
      [antd/header {:style {:background "#fff" :padding "0 16px" :display "flex" :alignItems "center" :justifyContent "space-between" :borderBottom "1px solid #f0f0f0"}}
       [:div {:style {:display "flex" :alignItems "center"}}
        [antd/text {:style {:marginRight 8}} "申请日期:"]
        [antd/range-picker {:style {:marginRight 16}
                            :value date-range
                            :onChange #(rf/dispatch [::events/set-date-range %])}]
        [antd/input-search {:placeholder "请输入搜索内容"
                            :allowClear true
                            :value search-term
                            :onChange #(rf/dispatch [::events/update-search-term (.. % -target -value)])
                            :onSearch #(rf/dispatch [::events/search-patients %])
                            :style {:width 300 :marginRight 8}}]
        [antd/button {:icon (r/as-element [antd/filter-outlined])}]]
       [:div {:style {:display "flex" :alignItems "center"}}
        [antd/text {:style {:marginRight 8}} "患者登记:"]
        [antd/input {:placeholder "请输入患者住院号/门诊号或扫描登记患者"
                     :style {:width 300 :marginRight 16}}]
        [antd/button {:type "primary"
                      :style {:marginRight 8}
                      :onClick #(rf/dispatch [::events/approve-patient])} "批准"]
        [antd/button {:style {:marginRight 8}
                      :onClick #(rf/dispatch [::events/postpone-patient])} "暂缓"]
        [antd/button {:danger true
                      :onClick #(rf/dispatch [::events/reject-patient])} "驳回"]]]
      [antd/content {:style {:padding "16px 0" :margin 0 :minHeight 280}}
       [antd/tabs {:activeKey (if (= active-tab "patients") "1" "2")
                   :onChange #(rf/dispatch [::events/set-active-tab (if (= % "1") "patients" "assessment")])
                   :items [{:key "1"
                            :label "门诊麻醉评估"
                            :children (r/as-element
                                       [antd/row {:gutter 16}
                                        [antd/col {:span 6}
                                         [antd/card {:title "患者列表" :variant "borderless" :style {:height "calc(100vh - 180px)"}}
                                          [patient-list]]]
                                        [antd/col {:span 18}
                                         [assessment-result]]])}
                           {:key "2"
                            :label "门诊麻醉同意"
                            :children "门诊麻醉同意内容"}]}]]]]))
