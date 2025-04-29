(ns hc.hospital.pages.anesthesia-home
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]))

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

(defn radio-with-input [field label]
  (let [history-item @(rf/subscribe [::subs/medical-history-item field])]
    [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
     [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} label]
     [antd/radio-group {:value (:value history-item)
                        :onChange #(rf/dispatch [::events/update-medical-history-option 
                                                field 
                                                (.. % -target -value)
                                                (when (= (.. % -target -value) "none") "")])
                        :style {:marginRight "10px"}}
      [antd/radio {:value "none"} "无"]
      [antd/radio {:value "yes"} "有"]]
     [antd/input {:placeholder "请输入描述内容"
                  :value (:description history-item)
                  :onChange #(rf/dispatch [::events/update-medical-history-option 
                                           field 
                                           (:value history-item) 
                                           (.. % -target -value)])
                  :disabled (= (:value history-item) "none")
                  :style {:flex "1"}}]]))

(defn brief-medical-history []
  (let [medical-history @(rf/subscribe [::subs/brief-medical-history])]
    [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
     [radio-with-input :past-history "既往史"]
     [radio-with-input :allergic-history "过敏史"]
     [radio-with-input :surgery-anesthesia-history "手术麻醉史"]
     [radio-with-input :pregnancy "怀孕"]
     [radio-with-input :blood-transfusion-history "输血史"]
     [radio-with-input :menstrual-period "月经期"]
     [:div {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "个人史"]
      [antd/checkbox-group 
       {:value (cond-> []
                 (:smoking (:personal-history medical-history)) (conj "smoke")
                 (:drinking (:personal-history medical-history)) (conj "drink"))
        :onChange #(rf/dispatch [::events/update-personal-history 
                                {:smoking (boolean (some #{"smoke"} %))
                                 :drinking (boolean (some #{"drink"} %))}])}
       [antd/checkbox {:value "smoke"} "烟"]
       [antd/checkbox {:value "drink"} "酒"]]]
     [:div {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "其他"]
      [antd/input {:placeholder "请输入" 
                   :style {:flex "1"}
                   :value (:other-description medical-history)
                   :onChange #(rf/dispatch [::events/update-other-description 
                                           :other-description
                                           (.. % -target -value)])}]]]))

(defn physical-examination []
  (let [exam-data @(rf/subscribe [::subs/physical-examination])]
    [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
     ;; 一般状况
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "一般状况:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:general-condition exam-data)
                         :onChange #(rf/dispatch [::events/update-physical-exam :general-condition (.. % -target -value)])}
       [antd/radio {:value "bad"} "差"]
       [antd/radio {:value "fair"} "尚可"]
       [antd/radio {:value "average"} "一般"]
       [antd/radio {:value "good"} "好"]]]
     
     ;; 身高体重行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "身高:"]
      [antd/input-number {:style {:width "120px"}
                          :value (:height exam-data)
                          :onChange #(rf/dispatch [::events/update-physical-exam :height %])}]
      [:span {:style {:margin "0 10px"}} "cm"]
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "体重:"]
      [antd/input-number {:style {:width "120px"}
                          :value (:weight exam-data)
                          :onChange #(rf/dispatch [::events/update-physical-exam :weight %])}]
      [:span {:style {:margin "0 10px"}} "kg"]]
     
     ;; 血压行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "BP:"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :value (get-in exam-data [:bp :systolic])
                   :onChange #(rf/dispatch [::events/update-blood-pressure 
                                           (js/parseInt (.. % -target -value))
                                           (get-in exam-data [:bp :diastolic])])}]
      [:span {:style {:margin "0 10px"}} "/"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :value (get-in exam-data [:bp :diastolic])
                   :onChange #(rf/dispatch [::events/update-blood-pressure 
                                           (get-in exam-data [:bp :systolic])
                                           (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "mmHg"]]
     
     ;; PR和RR行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "PR:"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :value (:heart-rate exam-data)
                   :onChange #(rf/dispatch [::events/update-physical-exam :heart-rate (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "次/分"]
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "RR:"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :value (:respiratory-rate exam-data)
                   :onChange #(rf/dispatch [::events/update-physical-exam :respiratory-rate (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "次/分"]]
     
     ;; 体温行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "T:"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :step "0.1"
                   :value (:temperature exam-data)
                   :onChange #(rf/dispatch [::events/update-physical-exam :temperature (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "°C"]]
     
     ;; 精神行为
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "精神行为:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:mental-state exam-data)
                         :onChange #(rf/dispatch [::events/update-physical-exam :mental-state (.. % -target -value)])}
       [antd/radio {:value "normal"} "正常"]
       [antd/radio {:value "drowsy"} "嗜睡"]
       [antd/radio {:value "coma"} "昏迷"]
       [antd/radio {:value "irritable"} "烦躁"]
       [antd/radio {:value "delirium"} "谵妄"]
       [antd/radio {:value "cognitive"} "认知障碍"]]]
     
     ;; 头颈部
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "头颈部:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:head-neck exam-data)
                         :onChange #(rf/dispatch [::events/update-physical-exam :head-neck (.. % -target -value)])}
       [antd/radio {:value "normal"} "无异常"]
       [antd/radio {:value "scar"} "疤痕"]
       [antd/radio {:value "short_neck"} "颈短"]
       [antd/radio {:value "neck_mass"} "颈部肿块"]
       [antd/radio {:value "limited_mobility"} "后仰困难"]]]
     
     ;; 口腔行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "口腔:"]
      [:span {:style {:margin "0 10px"}} "张口"]
      [antd/input {:style {:width "120px"}
                   :type "number"
                   :step "0.1"
                   :value (:mouth-opening exam-data)
                   :onChange #(rf/dispatch [::events/update-physical-exam :mouth-opening (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "cm"]]
     
     ;; Mallampati
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "Mallampati:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:mallampati-score exam-data)
                         :onChange #(rf/dispatch [::events/update-physical-exam :mallampati-score (.. % -target -value)])}
       [antd/radio {:value "I"} "I"]
       [antd/radio {:value "II"} "II"]
       [antd/radio {:value "III"} "III"]
       [antd/radio {:value "IV"} "IV"]]
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "颏颌距离:"]
      [antd/input {:style {:width "120px"}
                   :type "number" 
                   :step "0.1"
                   :value (:thyromental-distance exam-data)
                   :onChange #(rf/dispatch [::events/update-physical-exam :thyromental-distance (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "cm"]]
     
     ;; 相关病史
     (let [related-history (get-in exam-data [:related-history])]
       [:div.form-item-inline {:style {:marginBottom "16px"}}
        [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px" :display "block" :marginBottom "10px"}} "相关病史:"]
        [:div {:style {:display "flex" :flexWrap "wrap"}}
         [:div {:style {:width "33%" :marginBottom "10px"}}
          [antd/checkbox {:checked (:difficult-airway related-history)
                          :onChange #(rf/dispatch [::events/update-related-history :difficult-airway (.. % -target -checked)])}
           "困难气道史"]]
         [:div {:style {:width "33%" :marginBottom "10px"}}
          [antd/checkbox {:checked (:postoperative-nausea related-history)
                          :onChange #(rf/dispatch [::events/update-related-history :postoperative-nausea (.. % -target -checked)])}
           "术后恶心呕吐史"]]
         [:div {:style {:width "33%" :marginBottom "10px"}}
          [antd/checkbox {:checked (:malignant-hyperthermia related-history)
                          :onChange #(rf/dispatch [::events/update-related-history :malignant-hyperthermia (.. % -target -checked)])}
           "恶性高热史"]]
         [:div {:style {:width "100%" :display "flex" :alignItems "center"}}
          [antd/checkbox {:checked (boolean (:other related-history))
                          :onChange #(rf/dispatch [::events/update-related-history :other (if (.. % -target -checked) "" false)])}
           "其他"]
          [antd/input {:placeholder "请输入" 
                       :style {:width "300px" :marginLeft "10px"}
                       :disabled (not (:other related-history))
                       :value (or (:other related-history) "")
                       :onChange #(rf/dispatch [::events/update-related-history :other (.. % -target -value)])}]]]])
     
     ;; 胸
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "100px" :fontWeight "500" :marginRight "10px"}} "胸:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:chest exam-data)
                         :onChange #(rf/dispatch [::events/update-physical-exam :chest (.. % -target -value)])}
       [antd/radio {:value "normal"} "正常"]
       [antd/radio {:value "barrel"} "桶状胸"]
       [antd/radio {:value "pectus_excavatum"} "佝偻胸"]]]]))

(defn lab-tests []
  (let [lab-data @(rf/subscribe [::subs/lab-tests])]
    [antd/form {:layout "vertical" :style {:padding-bottom "24px"}}
     ;; 血常规标题
     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :marginBottom "16px"}}
      "血常规："]
     
     ;; RBC 和 Hct 行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "RBC:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :step "0.1"
                   :value (get-in lab-data [:complete-blood-count :hemoglobin])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:complete-blood-count :hemoglobin] 
                                           (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "×10¹²/L"]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "Hct:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :step "0.01"
                   :value (get-in lab-data [:complete-blood-count :hematocrit])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:complete-blood-count :hematocrit] 
                                           (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "%"]]
     
     ;; PLT 和 WBC 行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "PLT:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :value (get-in lab-data [:complete-blood-count :platelets])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:complete-blood-count :platelets] 
                                           (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "×10⁹/L"]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "WBC:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :step "0.1"
                   :value (get-in lab-data [:complete-blood-count :wbc])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:complete-blood-count :wbc] 
                                           (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "×10⁹/L"]]
     
     ;; 血型和 Rh 行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "血型:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:blood-type lab-data)
                         :onChange #(rf/dispatch [::events/update-lab-test :blood-type (.. % -target -value)])}
       [antd/radio {:value "A"} "A"]
       [antd/radio {:value "B"} "B"]
       [antd/radio {:value "AB"} "AB"]
       [antd/radio {:value "O"} "O"]]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "Rh:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:rh lab-data)
                         :onChange #(rf/dispatch [::events/update-lab-test :rh (.. % -target -value)])}
       [antd/radio {:value "negative"} "阴性"]
       [antd/radio {:value "positive"} "阳性"]]]
     
     ;; 凝血检查和血糖值行
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "凝血检查:"]
      [antd/radio-group {:buttonStyle "solid" 
                         :optionType "button"
                         :value (:coagulation lab-data)
                         :onChange #(rf/dispatch [::events/update-lab-test :coagulation (.. % -target -value)])}
       [antd/radio {:value "normal"} "正常"]
       [antd/radio {:value "abnormal"} "异常"]]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "血糖值:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :step "0.1"
                   :value (get-in lab-data [:biochemistry :glucose])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:biochemistry :glucose] 
                                           (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "mmol/L"]]
     
     ;; 生化项目标题
     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :margin "24px 0 16px"}}
      "生化指标："]
     
     ;; 肝功能
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "ALT:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :value (get-in lab-data [:biochemistry :alt])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:biochemistry :alt] 
                                           (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "U/L"]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "AST:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :value (get-in lab-data [:biochemistry :ast])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:biochemistry :ast] 
                                           (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "U/L"]]
     
     ;; 电解质
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "钠:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :value (get-in lab-data [:biochemistry :sodium])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:biochemistry :sodium] 
                                           (js/parseInt (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "mmol/L"]
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px" :marginLeft "30px"}} "钾:"]
      [antd/input {:style {:width "150px"}
                   :type "number"
                   :step "0.1"
                   :value (get-in lab-data [:biochemistry :potassium])
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                           [:biochemistry :potassium] 
                                           (js/parseFloat (.. % -target -value))])}]
      [:span {:style {:margin "0 10px"}} "mmol/L"]]
     
     ;; 心电图和胸片
     [:div.section-title {:style {:fontWeight "bold" :fontSize "16px" :margin "24px 0 16px"}}
      "医学影像："]
     
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "心电图:"]
      [antd/input {:style {:width "450px"}
                   :value (:ecg lab-data)
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                          :ecg
                                          (.. % -target -value)])}]]
     
     [:div.form-item-inline {:style {:display "flex" :alignItems "center" :marginBottom "16px"}}
      [:label {:style {:minWidth "80px" :fontWeight "500" :marginRight "10px"}} "胸片:"]
      [antd/input {:style {:width "450px"}
                   :value (:chest-xray lab-data)
                   :onChange #(rf/dispatch [::events/update-lab-test 
                                          :chest-xray
                                          (.. % -target -value)])}]]]))

(defn assessment-result []
  (let [active-tab @(rf/subscribe [::subs/active-assessment-tab])]
    [antd/card {:variant "borderless" :style {:height "calc(100vh - 180px)"}}
     [antd/tabs 
      {:activeKey active-tab
       :onChange #(rf/dispatch [::events/set-active-assessment-tab %])
       :type "card"
       :size "large"
       :tabPosition "left"
       :style {:height "100%"}
       :items [{:key "brief-history"
                :label "简要病史"
                :children (r/as-element
                           [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                            [brief-medical-history]])}
               {:key "physical-exam"
                :label "体格检查"
                :children (r/as-element
                           [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                            [physical-examination]])}
               {:key "lab-tests"
                :label "实验室检查"
                :children (r/as-element
                           [:div {:style {:padding "0 16px" :height "100%" :overflowY "auto"}}
                            [lab-tests]])}]}]]))

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
