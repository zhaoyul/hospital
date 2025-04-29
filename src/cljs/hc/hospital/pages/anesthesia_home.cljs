(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [reagent.dom :as d]
            ["antd" :as antd :refer [Layout  Menu  DatePicker Input Button Tabs Row Col Card Form Radio Checkbox InputNumber Typography]]
            ["@ant-design/icons" :refer [UserOutlined LaptopOutlined NotificationOutlined SearchOutlined FilterOutlined]]))

(def Content (.-Content Layout))
(def Sider (.-Sider Layout))
(def Header (.-Header Layout))
(def Footer (.-Footer Layout))
(def RangePicker (.-RangePicker DatePicker))
(def TabPane (.-TabPane Tabs))
(def Item (.-Item Form))
(def Title (.-Title Typography))
(def Text (.-Text Typography))
(def RadioGroup (.-Group Radio))
(def CheckboxGroup (.-Group Checkbox))

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
       [:> UserOutlined {:style {:marginRight "8px"}}] ; Placeholder icon
       [:div
        [:div {:style {:fontWeight "bold"}} (:name item)]
        [:div {:style {:fontSize "12px" :color "gray"}}
         (str (:sex item) " " (:age item) "岁 " (:type item))]]]
      [:div {:style {:textAlign "right"}}
       [:div {:style {:fontSize "12px" :color "gray"}} (:date item)]
       [:> antd/Tag {:color (case (:status item)
                              "待评估" "orange"
                              "已批准" "green"
                              "已暂缓" "blue"
                              "已驳回" "red"
                              "default")} (:status item)]]])])

(defn brief-medical-history []
  [:> Card {:title (r/as-element [:> Title {:level 5} "简要病史"]) :bordered false}
   [:> Form {:layout "vertical"}
    [:> Row {:gutter 16}
     [:> Col {:span 12}
      [:> Item {:label "既往史"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 12}
      [:> Item {:label "过敏史"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 12}
      [:> Item {:label "手术麻醉史"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 12}
      [:> Item {:label "怀孕"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 12}
      [:> Item {:label "输血史"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 12}
      [:> Item {:label "月经期"}
       [:> RadioGroup {}
        [:> Radio {:value "none"} "无"]
        [:> Radio {:value "yes"} "有"]]
       [:> Input {:placeholder "请输入"}]]]
     [:> Col {:span 24}
      [:> Item {:label "个人史"}
       [:> CheckboxGroup {}
        [:> Checkbox {:value "smoke"} "烟"]
        [:> Checkbox {:value "drink"} "酒"]]]]
     [:> Col {:span 24}
      [:> Item {:label "其他"}
       [:> Input {:placeholder "请输入"}]]]]]])

(defn physical-examination []
  [:> Card {:title (r/as-element [:> Title {:level 5} "体格检查"]) :bordered false}
   [:> Form {:layout "vertical"}
    [:> Item {:label "一般状况"}
     [:> RadioGroup {}
      [:> Radio {:value "bad"} "差"]
      [:> Radio {:value "fair"} "尚可"]
      [:> Radio {:value "average"} "一般"]
      [:> Radio {:value "good"} "好"]]]
    [:> Row {:gutter 16}
     [:> Col {:span 8} [:> Item {:label "身高"} [:> InputNumber {:addonAfter "cm"}]]]
     [:> Col {:span 8} [:> Item {:label "体重"} [:> InputNumber {:addonAfter "kg"}]]]
     [:> Col {:span 8} [:> Item {:label "BP"} [:> Input {:addonAfter "mmHg"}]]]
     [:> Col {:span 8} [:> Item {:label "PR"} [:> Input {:addonAfter "次/分"}]]]
     [:> Col {:span 8} [:> Item {:label "RR"} [:> Input {:addonAfter "次/分"}]]]
     [:> Col {:span 8} [:> Item {:label "T"} [:> Input {:addonAfter "°C"}]]]]
    [:> Item {:label "精神行为"}
     [:> RadioGroup {}
      [:> Radio {:value "normal"} "正常"]
      [:> Radio {:value "drowsy"} "嗜睡"]
      [:> Radio {:value "coma"} "昏迷"]
      [:> Radio {:value "irritable"} "烦躁"]
      [:> Radio {:value "delirium"} "谵妄"]
      [:> Radio {:value "cognitive"} "认知障碍"]]]
    [:> Item {:label "头颈部"}
     [:> CheckboxGroup {}
      [:> Checkbox {:value "normal"} "无异常"]
      [:> Checkbox {:value "scar"} "疤痕"]
      [:> Checkbox {:value "short_neck"} "颈短"]
      [:> Checkbox {:value "neck_mass"} "颈部肿块"]
      [:> Checkbox {:value "limited_mobility"} "后仰困难"]]]
    [:> Item {:label "口腔"} [:> Input {:addonAfter "cm" :placeholder "张口"}]]
    [:> Item {:label "Mallampati"}
     [:> RadioGroup {}
      [:> Radio {:value "I"} "I"]
      [:> Radio {:value "II"} "II"]
      [:> Radio {:value "III"} "III"]
      [:> Radio {:value "IV"} "IV"]]]
    [:> Item {:label "颏颌距离"} [:> InputNumber {:addonAfter "cm"}]]
    [:> Item {:label "相关病史"}
     [:> CheckboxGroup {:style {:width "100%"}}
      [:> Row
       [:> Col {:span 8} [:> Checkbox {:value "facial_injury"} "颌面部损伤"]]
       [:> Col {:span 8} [:> Checkbox {:value "tracheal_deviation"} "气管压迫移位"]]
       [:> Col {:span 8} [:> Checkbox {:value "sleep_apnea"} "睡眠呼吸暂停综合征"]]
       [:> Col {:span 8} [:> Checkbox {:value "acromegaly"} "肢端肥大"]]
       [:> Col {:span 8} [:> Checkbox {:value "congenital_malformation"} "先天畸形"]]
       [:> Col {:span 8} [:> Checkbox {:value "rheumatoid_arthritis"} "风湿性关节炎"]]
       [:> Col {:span 24} [:> Checkbox {:value "other"} "其他"] [:> Input {:placeholder "请输入"}]]]]]
    [:> Item {:label "胸"}
     [:> RadioGroup {}
      [:> Radio {:value "normal"} "正常"]
      [:> Radio {:value "barrel"} "桶状胸"]
      [:> Radio {:value "pectus_excavatum"} "佝偻胸"]]]
    ;; ... Add more physical examination fields as needed
    ]])

(defn lab-tests []
  [:> Card {:title (r/as-element [:> Title {:level 5} "实验室检查"]) :bordered false}
   [:> Form {:layout "vertical"}
    [:> Title {:level 5} "血常规"]
    [:> Row {:gutter 16}
     [:> Col {:span 8} [:> Item {:label "RBC"} [:> Input {:addonAfter "x10¹²/L"}]]]
     [:> Col {:span 8} [:> Item {:label "Hct"} [:> Input {:addonAfter "%"}]]]
     [:> Col {:span 8} [:> Item {:label "PLT"} [:> Input {:addonAfter "x10⁹/L"}]]]
     [:> Col {:span 8} [:> Item {:label "WBC"} [:> Input {:addonAfter "x10⁹/L"}]]]
     [:> Col {:span 8}
      [:> Item {:label "血型"}
       [:> RadioGroup {}
        [:> Radio {:value "A"} "A"]
        [:> Radio {:value "B"} "B"]
        [:> Radio {:value "AB"} "AB"]
        [:> Radio {:value "O"} "O"]]]]
     [:> Col {:span 8}
      [:> Item {:label "Rh"}
       [:> RadioGroup {}
        [:> Radio {:value "negative"} "阴性"]
        [:> Radio {:value "positive"} "阳性"]]]]]
    [:> Item {:label "凝血检查"}
     [:> RadioGroup {}
      [:> Radio {:value "normal"} "正常"]
      [:> Radio {:value "abnormal"} "异常"]]]
    [:> Item {:label "血糖值"} [:> Input {:addonAfter "mmol/L"}]]
    ;; ... Add more lab test fields as needed
    ]])

(defn anesthesia-home-page []
  [:> Layout {:style {:minHeight "100vh"}}
   [:> Sider {:width 200 :style {:background "#fff"}}
    [:div {:style {:height "32px" :margin "16px" :background "rgba(0, 0, 0, 0.2)"}}] ; Logo placeholder
    [:> Menu {:mode "inline" :defaultSelectedKeys ["1"] :style {:height "100%" :borderRight 0}}
     [:> Item {:key "1" :icon [:> LaptopOutlined]} "麻醉管理"]
     [:> Item {:key "2" :icon [:> UserOutlined]} "患者文书"]
     [:> Item {:key "3" :icon [:> NotificationOutlined]} "患者签到"]]]
   [:> Layout {:style {:padding "0 24px 24px"}}
    [:> Header {:style {:background "#fff" :padding "0 16px" :display "flex" :alignItems "center" :justifyContent "space-between" :borderBottom "1px solid #f0f0f0"}}
     [:div {:style {:display "flex" :alignItems "center"}}
      [:> Text {:style {:marginRight 8}} "申请日期:"]
      [:> RangePicker {:style {:marginRight 16}}]
      [:> Input.Search {:placeholder "请输入搜索内容" :allowClear true :style {:width 300 :marginRight 8}}]
      [:> Button {:icon (r/as-element [:> FilterOutlined])}]]
     [:div {:style {:display "flex" :alignItems "center"}}
      [:> Text {:style {:marginRight 8}} "患者登记:"]
      [:> Input {:placeholder "请输入患者住院号/门诊号或扫描登记患者" :style {:width 300 :marginRight 16}}]
      [:> Button {:type "primary" :style {:marginRight 8}} "批准"]
      [:> Button {:style {:marginRight 8}} "暂缓"]
      [:> Button {:danger true} "驳回"]]]
    [:> Content {:style {:padding "16px 0" :margin 0 :minHeight 280}}
     [:> Tabs {:defaultActiveKey "1"}
      [:> TabPane {:tab "门诊麻醉评估" :key "1"}
       [:> Row {:gutter 16}
        [:> Col {:span 8} ; Adjust span as needed for desired width
         [patient-list]]
        [:> Col {:span 16} ; Adjust span as needed
         [:div {:style {:background "#fff" :padding 24 :height "calc(100vh - 150px)" :overflowY "auto"}} ; Adjust height
          [:> Title {:level 4} "评估结果"]
          [brief-medical-history]
          [physical-examination]
          [lab-tests]]]]]
      [:> TabPane {:tab "门诊麻醉同意" :key "2"}
       "门诊麻醉同意内容"]]]]])
