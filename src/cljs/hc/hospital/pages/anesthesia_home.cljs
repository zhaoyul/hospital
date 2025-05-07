(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]
            [hc.hospital.components.form-components :as form-comp]
            ["antd" :refer [Form]]))

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
  (let [medical-history @(rf/subscribe [::subs/doctor-form-brief-medical-history])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues medical-history
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [::events/update-brief-medical-history all-values]))
                :style {:paddingBottom "24px"}}

     ;; 既往史
     [antd/form-item {:label "既往史"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:past-history :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:past-history :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:past-history :yes-no])
             [antd/form-item {:name [:past-history :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]

     ;; 过敏史
     [antd/form-item {:label "过敏史"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:allergic-history :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:allergic-history :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:allergic-history :yes-no])
             [antd/form-item {:name [:allergic-history :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]

     ;; 手术麻醉史
     [antd/form-item {:label "手术麻醉史"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:surgery-anesthesia-history :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:surgery-anesthesia-history :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:surgery-anesthesia-history :yes-no])
             [antd/form-item {:name [:surgery-anesthesia-history :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]

     ;; 怀孕
     [antd/form-item {:label "怀孕"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:pregnancy :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:pregnancy :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:pregnancy :yes-no])
             [antd/form-item {:name [:pregnancy :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]

     ;; 输血史
     [antd/form-item {:label "输血史"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:blood-transfusion-history :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:blood-transfusion-history :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:blood-transfusion-history :yes-no])
             [antd/form-item {:name [:blood-transfusion-history :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]

     ;; 月经期
     [antd/form-item {:label "月经期"}
      [antd/row {:gutter 16}
       [antd/col {:span 10}
        [antd/form-item {:name [:menstrual-period :yes-no]}
         [antd/radio-group {}
          [antd/radio {:value true} "有"]
          [antd/radio {:value false} "无"]]]]
       [antd/col {:span 14}
        [antd/form-item {:noStyle true :dependencies [[:menstrual-period :yes-no]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:menstrual-period :yes-no])
             [antd/form-item {:name [:menstrual-period :description] :noStyle true}
              [antd/input {:placeholder "描述（如有）" :style {:width "100%"}}]]))]]]]


     ;; 个人史 (Personal History) - Checkbox Group
     [antd/form-item {:name :personal-history ;; This is a direct key in DB, value is a vector
                      :label "个人史:"}
      [antd/checkbox-group {}
       [antd/checkbox {:value "smoke"} "烟"]
       [antd/checkbox {:value "drink"} "酒"]]]

     ;; 其他 (Other)
     [antd/form-item {:label "其他"}
      [antd/row {:gutter 16}
       [antd/col {:span 24}
        [antd/form-item {:name [:other :checkbox] :valuePropName "checked" :noStyle true}
         [antd/checkbox "有其他情况需要说明"]]]
       [antd/col {:span 24}
        [antd/form-item {:noStyle true :dependencies [[:other :checkbox]]}
         (fn [form-instance]
           (when (.getFieldValue form-instance [:other :checkbox])
             [antd/form-item {:name [:other :description] :noStyle true}
              [antd/input {:placeholder "请描述其他情况详情" :style {:marginTop "8px" :width "100%"}}]]))]]]]
     ]))

(defn physical-examination []
  (let [exam-data @(rf/subscribe [::subs/doctor-form-physical-examination])
        [form] ((.-useForm Form))]
    [antd/form {:form form :layout "horizontal" ;; 水平布局，让label和内容在同一行
                :labelCol {:span 4 :style {:textAlign "left"}} ;; 设置label宽度并左对齐
                :wrapperCol {:span 20} ;; 设置内容区域宽度
                :style {:padding-bottom "24px"
                        :maxWidth "800px" ;; 限制最大宽度使内容集中靠左
                        :marginLeft "0"} ;; 确保整体靠左对齐

                :initialValues exam-data
                :onValuesChange (fn [_ all-values] (rf/dispatch [::events/update-physical-examination all-values]))}

     ;; 使用通用组件
     [form-comp/radio-button-group
      {:label "一般状况"
       :name :general-condition ;; Direct key, correct
       :options [{:value "bad" :label "差"}
                 {:value "fair" :label "尚可"}
                 {:value "average" :label "一般"}
                 {:value "good" :label "好"}]}]

     ;; 身高体重
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :height :label "身高"} ;; Direct key, correct
        [form-comp/number-input-with-unit {:style {:width "100%"} :unit "cm"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :weight :label "体重"} ;; Direct key, correct
        [form-comp/number-input-with-unit {:style {:width "100%"} :unit "kg"}]]]]

     ;; 血压
     [antd/form-item {:label "BP:"}
      [antd/space-compact {:style {:width "100%"}}
       [antd/form-item {:name [:bp :systolic] :noStyle true} ;; Vector path, correct
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number"}]]
       [:span {:style {:display "inline-block" :width "30px" :lineHeight "32px" :textAlign "center"}} "/"]
       [antd/form-item {:name [:bp :diastolic] :noStyle true} ;; Vector path, correct
        [antd/input {:style {:width "calc(50% - 15px)"} :type "number" :unit "mmHg"}]]]]

     ;; 心率和呼吸
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :heart-rate :label "PR"} ;; Direct key, correct
        [form-comp/number-input-with-unit {:style {:width "100%"} :unit "次/分"}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :respiratory-rate :label "RR"} ;; Direct key, correct
        [form-comp/number-input-with-unit {:style {:width "100%"} :unit "次/分"}]]]]

     ;; 体温
     [antd/form-item {:name :temperature :label "T"} ;; Direct key, correct
      [form-comp/number-input-with-unit {:style {:width "100%"} :step "0.1" :unit "°C"}]]

     ;; 精神状态
     [form-comp/radio-button-group
      {:label "精神行为"
       :name :mental-state ;; Direct key, correct
       :options [{:value "normal" :label "正常"}
                 {:value "drowsy" :label "嗜睡"}
                 {:value "coma" :label "昏迷"}
                 {:value "irritable" :label "烦躁"}
                 {:value "delirium" :label "谵妄"}
                 {:value "cognitive" :label "认知障碍"}]}]

     ;; 头颈部
     [form-comp/radio-button-group
      {:label "头颈部"
       :name :head-neck ;; Direct key, correct
       :options [{:value "normal" :label "无异常"}
                 {:value "scar" :label "疤痕"}
                 {:value "short_neck" :label "颈短"}
                 {:value "neck_mass" :label "颈部肿块"}
                 {:value "limited_mobility" :label "后仰困难"}]}]

     ;; 张口
     [antd/form-item {:name :mouth-opening :label "口腔: 张口"} ;; Direct key, correct
      [form-comp/number-input-with-unit {:style {:width "100%"} :step "0.1" :unit "cm"}]]

     ;; Mallampati 和 甲颌间距
     [antd/row {:gutter 16}
      [antd/col {:span 12}
       [antd/form-item {:name :mallampati-score :label "Mallampati"} ;; Direct key, correct
        [form-comp/radio-button-group
         {;; :label "Mallampati" ;; Label is on Form.Item
          :name :mallampati-score ;; This name is for the radio group itself, Form.Item handles the value
          :options [{:value "I" :label "I"}
                    {:value "II" :label "II"}
                    {:value "III" :label "III"}
                    {:value "IV" :label "IV"}]}]]]
      [antd/col {:span 12}
       [antd/form-item {:name :thyromental-distance :label "颏颌距离"} ;; Direct key, correct
        [form-comp/number-input-with-unit {:style {:width "100%"} :step "0.1" :unit "cm"}]]]]

     ;; 相关病史
     [antd/form-item {:label "相关病史:"}
      [antd/space {:direction "vertical"}
       [antd/form-item {:name [:related-history :difficult-airway] :valuePropName "checked" :noStyle true} ;; Vector path, correct
        [antd/checkbox "困难气道史"]]
       [antd/form-item {:name [:related-history :postoperative-nausea] :valuePropName "checked" :noStyle true} ;; Vector path, correct
        [antd/checkbox "术后恶心呕吐史"]]
       [antd/form-item {:name [:related-history :malignant-hyperthermia] :valuePropName "checked" :noStyle true} ;; Vector path, correct
        [antd/checkbox "恶性高热史"]]
       [antd/row
        [antd/form-item {:name [:related-history :other-checkbox] :valuePropName "checked" :noStyle true} ;; Vector path, correct
         [antd/checkbox "其他"]]
        [antd/form-item {:name [:related-history :other] :noStyle true :dependencies [[:related-history :other-checkbox]]} ;; Vector path, correct
         (fn [form-instance]
           (let [other-checked (.getFieldValue form-instance [:related-history :other-checkbox])]
             [antd/input {:placeholder "请输入"
                          :style {:width "300px" :marginLeft "10px"}
                          :disabled (not other-checked)
                          :maxLength 100
                          :showCount true}]))]]]]

     ;; 胸部
     [form-comp/radio-button-group
      {:label "胸"
       :name :chest ;; Direct key, correct
       :options [{:value "normal" :label "正常"}
                 {:value "barrel" :label "桶状胸"}
                 {:value "pectus_excavatum" :label "佝偻胸"}]}]]))

(defn lab-tests []
  (let [lab-data @(rf/subscribe [::subs/doctor-form-lab-tests])
        [form] ((.-useForm Form))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues lab-data
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [::events/update-lab-tests all-values]))
                :style {:padding-bottom "24px"}}

     [form-comp/section-title {:title "血常规：" :margin-top "0"}]

     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit ;; form-comp/number-input-with-unit passes :name to an internal Form.Item
        {:label "RBC" :name [:complete-blood-count :hemoglobin] :step "0.1" :unit "×10¹²/L"}] ;; Vector path, correct
       :right-item
       [form-comp/number-input-with-unit
        {:label "Hct" :name [:complete-blood-count :hematocrit] :step "0.01" :unit "%"}]}] ;; Vector path, correct

     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "PLT" :name [:complete-blood-count :platelets] :unit "×10⁹/L"}] ;; Vector path, correct
       :right-item
       [form-comp/number-input-with-unit
        {:label "WBC" :name [:complete-blood-count :wbc] :step "0.1" :unit "×10⁹/L"}]}] ;; Vector path, correct

     [form-comp/two-column-row
      {:left-item
       [form-comp/radio-button-group ;; form-comp/radio-button-group passes :name to an internal Form.Item
        {:label "血型"
         :name :blood-type ;; Direct key, correct
         :options [{:value "A" :label "A"}
                   {:value "B" :label "B"}
                   {:value "AB" :label "AB"}
                   {:value "O" :label "O"}]}]
       :right-item
       [form-comp/radio-button-group
        {:label "Rh"
         :name :rh ;; Direct key, correct
         :options [{:value "negative" :label "阴性"}
                   {:value "positive" :label "阳性"}]}]}]

     [form-comp/two-column-row
      {:left-item
       [form-comp/radio-button-group
        {:label "凝血检查"
         :name :coagulation ;; Direct key, correct
         :options [{:value "normal" :label "正常"}
                   {:value "abnormal" :label "异常"}]}]
       :right-item
       [form-comp/number-input-with-unit
        {:label "血糖值" :name [:biochemistry :glucose] :step "0.1" :unit "mmol/L"}]}] ;; Vector path, correct

     [form-comp/section-title {:title "生化指标："}]

     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "ALT" :name [:biochemistry :alt] :unit "U/L"}] ;; Vector path, correct
       :right-item
       [form-comp/number-input-with-unit
        {:label "AST" :name [:biochemistry :ast] :unit "U/L"}]}] ;; Vector path, correct

     [form-comp/two-column-row
      {:left-item
       [form-comp/number-input-with-unit
        {:label "钠" :name [:biochemistry :sodium] :unit "mmol/L"}] ;; Vector path, correct
       :right-item
       [form-comp/number-input-with-unit
        {:label "钾" :name [:biochemistry :potassium] :step "0.1" :unit "mmol/L"}]}] ;; Vector path, correct

     [form-comp/section-title {:title "医学影像："}]

     [form-comp/limited-text-input ;; form-comp/limited-text-input passes :name to an internal Form.Item
      {:name :ecg :label "心电图" :placeholder "请输入心电图检查结果"}] ;; Direct key, correct

     [form-comp/limited-text-input
      {:name :chest-xray :label "胸片" :placeholder "请输入胸片检查结果"}]])) ;; Direct key, correct

(defn assessment-result []
  [antd/card {:variant "borderless" :style {:height "calc(100vh - 180px)" :overflowY "auto"}}
   [:div {:style {:padding "0 16px"}}
    [antd/row {:gutter 16}
     [antd/col {:span 12}
      [antd/card {:title "简要病史" :style {:marginBottom "16px"}}
       [:f> brief-medical-history]]
      [antd/card {:title "实验室检查"}
       [:f> lab-tests]]]
     [antd/col {:span 12}
      [antd/card {:title "体格检查"}
       [:f> physical-examination]]]]]])

(def menu-items
  [{:key "1" :icon (r/as-element [antd/laptop-outlined]) :label "麻醉管理"}
   {:key "2" :icon (r/as-element [antd/user-outlined]) :label "患者文书"}
   {:key "3" :icon (r/as-element [antd/notification-outlined]) :label "患者签到"}])

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])
        search-term @(rf/subscribe [::subs/search-term])
        date-range @(rf/subscribe [::subs/date-range])
        _selected-forms-data @(rf/subscribe [::subs/selected-patient-assessment-forms-data])]
    [antd/layout {:style {:minHeight "100vh"}}
     [antd/sider {:width 200 :style {:background "#fff"}}
      [:div {:style {:height "32px" :margin "16px" :background "rgba(0, 0, 0, 0.2)"}}]
      [antd/menu {:mode "inline"
                  :selectedKeys [(case active-tab "patients" "1" "assessment" "2" "history" "3" "1")]
                  :onChange #(rf/dispatch [::events/set-active-tab (case % "1" "patients" "2" "assessment" "3" "history")])
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
