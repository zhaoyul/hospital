(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [hc.hospital.components.antd :as antd]))

;; Placeholder data for the patient list
(def patient-data
  [{:key "1" :name "疏树华" :sex "男" :age 21 :type "吸入麻醉" :date "2022.11.09" :status "待评估"}
   {:key "2" :name "疏树华" :sex "男" :age 21 :type "表面麻醉" :date "2022.11.08" :status "待评估"}
   {:key "3" :name "疏树华" :sex "男" :age 21 :type "区域神经阻滞" :date "2022.11.08" :status "待评估"}
   {:key "4" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.08" :status "待评估"}
   {:key "5" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.07" :status "已批准"}
   {:key "6" :name "疏树华" :sex "男" :age 21 :type "表面麻醉" :date "2022.11.05" :status "已暂缓"}
   {:key "7" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.05" :status "已批准"}
   {:key "8" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.04" :status "已批准"}
   {:key "9" :name "疏树华" :sex "男" :age 21 :type "表面麻醉" :date "2022.11.04" :status "已驳回"}
   {:key "10" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.04" :status "已批准"}
   {:key "11" :name "疏树华" :sex "男" :age 21 :type "表面麻醉" :date "2022.11.03" :status "已批准"}
   {:key "12" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.03" :status "已驳回"}
   {:key "13" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.03" :status "已暂缓"}
   {:key "14" :name "疏树华" :sex "男" :age 21 :type "表面麻醉" :date "2022.11.03" :status "已批准"}
   {:key "15" :name "疏树华" :sex "男" :age 21 :type "局部麻醉" :date "2022.11.03" :status "已批准"}])

(defn patient-list []
  [:div {:style {:height "calc(100vh - 200px)" :overflowY "auto"}} ; Adjust height as needed
   (for [item patient-data]
     ^{:key (:key item)}
     [:div {:style {:padding "10px" :borderBottom "1px solid #f0f0f0" :display "flex" :justifyContent "space-between" :alignItems "center"}}
      [:div {:style {:display "flex" :alignItems "center"}}
       [antd/user-outlined {:style {:marginRight "8px"}}] ; Use wrapped icon
       [:div
        [:div {:style {:fontWeight "bold"}} (:name item)]
        [:div {:style {:fontSize "12px" :color "gray"}}
         (str (:sex item) " " (:age item) "岁 " (:type item))]]]
      [:div {:style {:textAlign "right"}}
       [:div {:style {:fontSize "12px" :color "gray"}} (:date item)]
       [antd/tag {:color (case (:status item) ; Use wrapped tag
                              "待评估" "orange"
                              "已批准" "green"
                              "已暂缓" "blue"
                              "已驳回" "red"
                              "default")} (:status item)]]])])

(defn radio-with-input [label]
  (let [option (r/atom "none")]  ;; 默认选择"无"
    (fn []
      [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
       [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} label]
       [antd/radio-group {:value @option
                          :onChange #(reset! option (.. % -target -value))
                          :style {:marginRight "10px"}}
        [antd/radio {:value "none"} "无"]
        [antd/radio {:value "yes"} "有"]]
       [antd/input {:placeholder "请输入描述内容"
                    :disabled (= @option "none")
                    :style {:flex "1"}}]])))

(defn brief-medical-history []
  [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
   [radio-with-input "既往史"]
   [radio-with-input "过敏史"]
   [radio-with-input "手术麻醉史"]
   [radio-with-input "怀孕"]
   [radio-with-input "输血史"]
   [radio-with-input "月经期"]
   [:div {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "个人史"]
    [antd/checkbox-group {}
     [antd/checkbox {:value "smoke"} "烟"]
     [antd/checkbox {:value "drink"} "酒"]]]
   [:div {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "其他"]
    [antd/input {:placeholder "请输入" :style {:flex "1"}}]]])

(defn physical-examination []
  [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
   ;; 一般状况 - 使用 button 风格的单选组
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "一般状况:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "bad"} "差"]
     [antd/radio {:value "fair"} "尚可"]
     [antd/radio {:value "average"} "一般"]
     [antd/radio {:value "good"} "好"]]]
   
   ;; 身高体重行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "身高:"]
    [antd/input-number {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "cm"]
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "体重:"]
    [antd/input-number {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "kg"]]
   
   ;; 血压行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "BP:"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "/"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "mmHg"]]
   
   ;; PR和RR行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "PR:"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "次/分"]
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "RR:"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "次/分"]]
   
   ;; 体温行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "T:"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "°C"]]
   
   ;; 精神行为 - 使用 button 风格的单选组
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "精神行为:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "normal"} "正常"]
     [antd/radio {:value "drowsy"} "嗜睡"]
     [antd/radio {:value "coma"} "昏迷"]
     [antd/radio {:value "irritable"} "烦躁"]
     [antd/radio {:value "delirium"} "谵妄"]
     [antd/radio {:value "cognitive"} "认知障碍"]]]
   
   ;; 头颈部 - 使用 button 风格的单选组
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "头颈部:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "normal"} "无异常"]
     [antd/radio {:value "scar"} "疤痕"]
     [antd/radio {:value "short_neck"} "颈短"]
     [antd/radio {:value "neck_mass"} "颈部肿块"]
     [antd/radio {:value "limited_mobility"} "后仰困难"]]]
   
   ;; 口腔行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "口腔:"]
    [:span {:style {:margin "0 10px"}} "张口"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "cm"]]
   
   ;; Mallampati - 使用 button 风格的单选组
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "Mallampati:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "I"} "I"]
     [antd/radio {:value "II"} "II"]
     [antd/radio {:value "III"} "III"]
     [antd/radio {:value "IV"} "IV"]]
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "颏颌距离:"]
    [antd/input {:style {:width "120px"}}]
    [:span {:style {:margin "0 10px"}} "cm"]]
   
   ;; 相关病史 - 使用 checkbox 组
   [:div.form-item-inline {:style {:marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :display "block" :marginBottom "10px"}} "相关病史:"]
    [:div {:style {:display "flex" :flexWrap "wrap"}}
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "facial_injury"} "颌面部损伤"]]
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "tracheal_deviation"} "气管压迫移位"]]
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "sleep_apnea"} "睡眠呼吸暂停综合征"]]
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "acromegaly"} "肢端肥大"]]
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "congenital_malformation"} "先天畸形"]]
     [:div {:style {:width "33%" :marginBottom "10px"}}
      [antd/checkbox {:value "rheumatoid_arthritis"} "风湿性关节炎"]]
     [:div {:style {:width "100%" :display "flex" :alignItems "center"}}
      [antd/checkbox {:value "other"} "其他"]
      [antd/input {:placeholder "请输入" :style {:width "300px" :marginLeft "10px"}}]]]]
   
   ;; 胸 - 使用 button 风格的单选组
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "胸:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "normal"} "正常"]
     [antd/radio {:value "barrel"} "桶状胸"]
     [antd/radio {:value "pectus_excavatum"} "佝偻胸"]]]])

(defn lab-tests []
  [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
   ;; 血常规标题
   [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :marginBottom "16px"}}
    "血常规："]
   
   ;; RBC 和 Hct 行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "RBC:"]
    [antd/input {:style {:width "150px"}}]
    [:span {:style {:margin "0 10px"}} "×10¹²/L"]
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "Hct:"]
    [antd/input {:style {:width "150px"}}]
    [:span {:style {:margin "0 10px"}} "%"]]
   
   ;; PLT 和 WBC 行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "PLT:"]
    [antd/input {:style {:width "150px"}}]
    [:span {:style {:margin "0 10px"}} "×10⁹/L"]
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "WBC:"]
    [antd/input {:style {:width "150px"}}]
    [:span {:style {:margin "0 10px"}} "×10⁹/L"]]
   
   ;; 血型和 Rh 行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "血型:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "A"} "A"]
     [antd/radio {:value "B"} "B"]
     [antd/radio {:value "AB"} "AB"]
     [antd/radio {:value "O"} "O"]]
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "Rh:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "negative"} "阴性"]
     [antd/radio {:value "positive"} "阳性"]]]
   
   ;; 凝血检查和血糖值行
   [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "凝血检查:"]
    [antd/radio-group {:buttonStyle "solid" :optionType "button"}
     [antd/radio {:value "normal"} "正常"]
     [antd/radio {:value "abnormal"} "异常"]]
    [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "血糖值:"]
    [antd/input {:style {:width "150px"}}]
    [:span {:style {:margin "0 10px"}} "mmol/L"]]
   
   ;; 可以根据需要添加更多实验室检查项目
   ])

(defn assessment-result []
  [antd/card {:variant "borderless" :style {:height "calc(100vh - 180px)"}}
   [antd/tabs 
    {:defaultActiveKey "1"
     :type "card"
     :size "large"
     :tabPosition "left"
     :style {:height "100%"}
     :items [{:key "1"
              :label "简要病史"
              :children (r/as-element
                         [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                          [brief-medical-history]])}
             {:key "2"
              :label "体格检查"
              :children (r/as-element
                         [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                          [physical-examination]])}
             {:key "3"
              :label "实验室检查"
              :children (r/as-element
                         [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                          [lab-tests]])}]}]])

(def menu-items
  [{:key "1" :icon (r/as-element [antd/laptop-outlined]) :label "麻醉管理"} ; Use wrapped icons
   {:key "2" :icon (r/as-element [antd/user-outlined]) :label "患者文书"}
   {:key "3" :icon (r/as-element [antd/notification-outlined]) :label "患者签到"}])

(defn anesthesia-home-page []
  [antd/layout {:style {:minHeight "100vh"}} ; Use wrapped components
   [antd/sider {:width 200 :style {:background "#fff"}}
    [:div {:style {:height "32px" :margin "16px" :background "rgba(0, 0, 0, 0.2)"}}] ; Logo placeholder
    [antd/menu {:mode "inline"
                :defaultSelectedKeys ["1"]
                :style {:height "100%" :borderRight 0}
                :items menu-items}]]
   [antd/layout {:style {:padding "0 24px 24px"}}
    [antd/header {:style {:background "#fff" :padding "0 16px" :display "flex" :alignItems "center" :justifyContent "space-between" :borderBottom "1px solid #f0f0f0"}}
     [:div {:style {:display "flex" :alignItems "center"}}
      [antd/text {:style {:marginRight 8}} "申请日期:"]
      [antd/range-picker {:style {:marginRight 16}}]
      [antd/input-search {:placeholder "请输入搜索内容" :allowClear true :style {:width 300 :marginRight 8}}]
      [antd/button {:icon (r/as-element [antd/filter-outlined])}]] ; Use wrapped icon
     [:div {:style {:display "flex" :alignItems "center"}}
      [antd/text {:style {:marginRight 8}} "患者登记:"]
      [antd/input {:placeholder "请输入患者住院号/门诊号或扫描登记患者" :style {:width 300 :marginRight 16}}]
      [antd/button {:type "primary" :style {:marginRight 8}} "批准"]
      [antd/button {:style {:marginRight 8}} "暂缓"]
      [antd/button {:danger true} "驳回"]]]
    [antd/content {:style {:padding "16px 0" :margin 0 :minHeight 280}}
     [antd/tabs {:defaultActiveKey "1"
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
                          :children "门诊麻醉同意内容"}]}]]]])
