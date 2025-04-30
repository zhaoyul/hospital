(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]
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
        [form] ((.-useForm Form))] ; Get form instance
    [antd/form {:form form
                :layout "vertical"
                :initialValues medical-history ; Set initial values from subscription
                :onValuesChange (fn [changed-values all-values]
                                  ;; Dispatch one event with all form values
                                  (rf/dispatch [::events/update-brief-medical-history all-values]))
                :style {:padding-bottom "24px"}}
     ;; Use Form.Item for each field
     [antd/form-item {:name :past-history-radio ; Use a temporary name for the radio
                      :label "既往史"
                      :style {:marginBottom "0px"}} ; Adjust style as needed
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :past-history-desc ; Use a separate name for description
                      :noStyle true ; Hide label/styling for this item
                      :dependencies [:past-history-radio]} ; Make dependent on radio value
      (fn [form-instance] ; Function as child to access form state
        (let [radio-value (.getFieldValue form-instance :past-history-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     ;; Repeat similar pattern for other radio-with-input fields
     [antd/form-item {:name :allergic-history-radio
                      :label "过敏史"
                      :style {:marginBottom "0px"}}
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :allergic-history-desc
                      :noStyle true
                      :dependencies [:allergic-history-radio]}
      (fn [form-instance]
        (let [radio-value (.getFieldValue form-instance :allergic-history-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     [antd/form-item {:name :surgery-anesthesia-history-radio
                      :label "手术麻醉史"
                      :style {:marginBottom "0px"}}
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :surgery-anesthesia-history-desc
                      :noStyle true
                      :dependencies [:surgery-anesthesia-history-radio]}
      (fn [form-instance]
        (let [radio-value (.getFieldValue form-instance :surgery-anesthesia-history-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     [antd/form-item {:name :pregnancy-radio
                      :label "怀孕"
                      :style {:marginBottom "0px"}}
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :pregnancy-desc
                      :noStyle true
                      :dependencies [:pregnancy-radio]}
      (fn [form-instance]
        (let [radio-value (.getFieldValue form-instance :pregnancy-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     [antd/form-item {:name :blood-transfusion-history-radio
                      :label "输血史"
                      :style {:marginBottom "0px"}}
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :blood-transfusion-history-desc
                      :noStyle true
                      :dependencies [:blood-transfusion-history-radio]}
      (fn [form-instance]
        (let [radio-value (.getFieldValue form-instance :blood-transfusion-history-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     [antd/form-item {:name :menstrual-period-radio
                      :label "月经期"
                      :style {:marginBottom "0px"}}
      [antd/radio-group {}
       [antd/radio {:value "none"} "无"]
       [antd/radio {:value "yes"} "有"]]]
     [antd/form-item {:name :menstrual-period-desc
                      :noStyle true
                      :dependencies [:menstrual-period-radio]}
      (fn [form-instance]
        (let [radio-value (.getFieldValue form-instance :menstrual-period-radio)]
          [antd/input {:placeholder "请输入描述内容"
                       :disabled (= radio-value "none")
                       :style {:marginTop "8px" :marginBottom "16px"}}]))]

     ;; Personal History Checkbox
     [antd/form-item {:name [:personal-history :smoking-drinking] ; Use path for name
                      :label "个人史"}
      [antd/checkbox-group {}
       [antd/checkbox {:value "smoke"} "烟"]
       [antd/checkbox {:value "drink"} "酒"]]]

     ;; Other Description Input
     [antd/form-item {:name :other-description
                      :label "其他"}
      [antd/input {:placeholder "请输入"
                   :style {:flex "1"}}]]]))

(defn physical-examination []
  (let [exam-data @(rf/subscribe [::subs/physical-examination])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues exam-data
                :onValuesChange (fn [changed-values all-values]
                                  (rf/dispatch [::events/update-physical-examination all-values]))
                :style {:padding-bottom "24px"}}

     ;; General Condition
     [antd/form-item {:name :general-condition :label "一般状况:"}
      [antd/radio-group {:buttonStyle "solid" :optionType "button"}
       [antd/radio {:value "bad"} "差"]
       [antd/radio {:value "fair"} "尚可"]
       [antd/radio {:value "average"} "一般"]
       [antd/radio {:value "good"} "好"]]]

     ;; Height and Weight (using Row/Col for layout within Form)
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :height :label "身高:"}
        [antd/input-number {:style {:width "100%"} :addonAfter "cm"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :weight :label "体重:"}
        [antd/input-number {:style {:width "100%"} :addonAfter "kg"}]]]]

     ;; Blood Pressure
     [antd/form-item {:label "BP:"}
      [antd/input-group {:compact true}
       [antd/form-item {:name [:bp :systolic] :noStyle true}
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number"}]]
       [:span {:style {:display "inline-block" :width "30px" :lineHeight "32px" :textAlign "center"}} "/"]
       [antd/form-item {:name [:bp :diastolic] :noStyle true}
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number" :addonAfter "mmHg"}]]]]

     ;; PR and RR
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :heart-rate :label "PR:"}
        [antd/input {:type "number" :addonAfter "次/分"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :respiratory-rate :label "RR:"}
        [antd/input {:type "number" :addonAfter "次/分"}]]]]

     ;; Temperature
     [antd/form-item {:name :temperature :label "T:"}
      [antd/input {:type "number" :step "0.1" :addonAfter "°C"}]]

     ;; Mental State
     [antd/form-item {:name :mental-state :label "精神行为:"}
      [antd/radio-group {:buttonStyle "solid" :optionType "button"}
       [antd/radio {:value "normal"} "正常"]
       [antd/radio {:value "drowsy"} "嗜睡"]
       [antd/radio {:value "coma"} "昏迷"]
       [antd/radio {:value "irritable"} "烦躁"]
       [antd/radio {:value "delirium"} "谵妄"]
       [antd/radio {:value "cognitive"} "认知障碍"]]]

     ;; Head and Neck
     [antd/form-item {:name :head-neck :label "头颈部:"}
      [antd/radio-group {:buttonStyle "solid" :optionType "button"}
       [antd/radio {:value "normal"} "无异常"]
       [antd/radio {:value "scar"} "疤痕"]
       [antd/radio {:value "short_neck"} "颈短"]
       [antd/radio {:value "neck_mass"} "颈部肿块"]
       [antd/radio {:value "limited_mobility"} "后仰困难"]]]

     ;; Mouth Opening
     [antd/form-item {:name :mouth-opening :label "口腔: 张口"}
      [antd/input {:type "number" :step "0.1" :addonAfter "cm"}]]

     ;; Mallampati and Thyromental Distance
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :mallampati-score :label "Mallampati:"}
        [antd/radio-group {:buttonStyle "solid" :optionType "button"}
         [antd/radio {:value "I"} "I"]
         [antd/radio {:value "II"} "II"]
         [antd/radio {:value "III"} "III"]
         [antd/radio {:value "IV"} "IV"]]]]
      [antd/col {:span 12}
       [antd/form-item {:name :thyromental-distance :label "颏颌距离:"}
        [antd/input {:type "number" :step "0.1" :addonAfter "cm"}]]]]

     ;; Related History
     [antd/form-item {:label "相关病史:"}
      [antd/form-item {:name [:related-history :difficult-airway] :valuePropName "checked" :noStyle true}
       [antd/checkbox "困难气道史"]]
      [antd/form-item {:name [:related-history :postoperative-nausea] :valuePropName "checked" :noStyle true}
       [antd/checkbox "术后恶心呕吐史"]]
      [antd/form-item {:name [:related-history :malignant-hyperthermia] :valuePropName "checked" :noStyle true}
       [antd/checkbox "恶性高热史"]]
      [antd/form-item {:name [:related-history :other-checkbox] :valuePropName "checked" :noStyle true} ; Temp name for checkbox
       [antd/checkbox "其他"]]
      [antd/form-item {:name [:related-history :other] :noStyle true :dependencies [[:related-history :other-checkbox]]}
       (fn [form-instance]
         (let [other-checked (.getFieldValue form-instance [:related-history :other-checkbox])]
           [antd/input {:placeholder "请输入"
                        :style {:width "300px" :marginLeft "10px"}
                        :disabled (not other-checked)}]))]]

     ;; Chest
     [antd/form-item {:name :chest :label "胸:"}
      [antd/radio-group {:buttonStyle "solid" :optionType "button"}
       [antd/radio {:value "normal"} "正常"]
       [antd/radio {:value "barrel"} "桶状胸"]
       [antd/radio {:value "pectus_excavatum"} "佝偻胸"]]]]))

(defn lab-tests []
  (let [lab-data @(rf/subscribe [::subs/lab-tests])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues lab-data
                :onValuesChange (fn [changed-values all-values]
                                  (rf/dispatch [::events/update-lab-tests all-values]))
                :style {:padding-bottom "24px"}}

     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :marginBottom "16px"}} "血常规："]

     ;; RBC and Hct
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name [:complete-blood-count :hemoglobin] :label "RBC:"} ; Corrected label to Hemoglobin (assuming RBC meant this)
        [antd/input {:type "number" :step "0.1" :addonAfter "×10¹²/L"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name [:complete-blood-count :hematocrit] :label "Hct:"}
        [antd/input {:type "number" :step "0.01" :addonAfter "%"}]]]]

     ;; PLT and WBC
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name [:complete-blood-count :platelets] :label "PLT:"}
        [antd/input {:type "number" :addonAfter "×10⁹/L"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name [:complete-blood-count :wbc] :label "WBC:"}
        [antd/input {:type "number" :step "0.1" :addonAfter "×10⁹/L"}]]]]

     ;; Blood Type and Rh
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :blood-type :label "血型:"}
        [antd/radio-group {:buttonStyle "solid" :optionType "button"}
         [antd/radio {:value "A"} "A"]
         [antd/radio {:value "B"} "B"]
         [antd/radio {:value "AB"} "AB"]
         [antd/radio {:value "O"} "O"]]]]
      [antd/col {:span 12}
       [antd/form-item {:name :rh :label "Rh:"}
        [antd/radio-group {:buttonStyle "solid" :optionType "button"}
         [antd/radio {:value "negative"} "阴性"]
         [antd/radio {:value "positive"} "阳性"]]]]]

     ;; Coagulation and Glucose
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :coagulation :label "凝血检查:"}
        [antd/radio-group {:buttonStyle "solid" :optionType "button"}
         [antd/radio {:value "normal"} "正常"]
         [antd/radio {:value "abnormal"} "异常"]]]]
      [antd/col {:span 12}
       [antd/form-item {:name [:biochemistry :glucose] :label "血糖值:"}
        [antd/input {:type "number" :step "0.1" :addonAfter "mmol/L"}]]]]

     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :margin "24px 0 16px"}} "生化指标："]

     ;; ALT and AST
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name [:biochemistry :alt] :label "ALT:"}
        [antd/input {:type "number" :addonAfter "U/L"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name [:biochemistry :ast] :label "AST:"}
        [antd/input {:type "number" :addonAfter "U/L"}]]]]

     ;; Sodium and Potassium
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name [:biochemistry :sodium] :label "钠:"}
        [antd/input {:type "number" :addonAfter "mmol/L"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name [:biochemistry :potassium] :label "钾:"}
        [antd/input {:type "number" :step "0.1" :addonAfter "mmol/L"}]]]]

     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :margin "24px 0 16px"}} "医学影像："]

     ;; ECG
     [antd/form-item {:name :ecg :label "心电图:"}
      [antd/input {:style {:width "100%"}}]]

     ;; Chest X-ray
     [antd/form-item {:name :chest-xray :label "胸片:"}
      [antd/input {:style {:width "100%"}}]]]))

(defn assessment-result []
  ;; Remove active-tab subscription as tabs are removed
  [antd/card {:variant "borderless" :style {:height "calc(100vh - 180px)" :overflowY "auto"}} ; Add overflowY auto to the card
   ;; Remove antd/tabs component
   [:div {:style {:padding "0 16px"}} ; Add a container div with padding
    ;; Render the three sections vertically
    [:div.section-title {:style {:fontWeight "bold" :fontSize "18px" :marginBottom "16px" :marginTop "16px"}} "简要病史"]
    [:f> brief-medical-history]
    [:div.section-title {:style {:fontWeight "bold" :fontSize "18px" :marginBottom "16px" :marginTop "24px"}} "体格检查"]
    [:f> physical-examination]
    [:div.section-title {:style {:fontWeight "bold" :fontSize "18px" :marginBottom "16px" :marginTop "24px"}} "实验室检查"]
    [:f> lab-tests]]])

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
