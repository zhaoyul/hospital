(ns hc.hospital.patient.views
  (:require
   [re-frame.core :as rf]
   [hc.hospital.patient.events :as events]
   [hc.hospital.patient.subs :as subs]
   [hc.hospital.components.antd :refer [button form form-item input input-number text-area
                                        radio radio-group select option steps step
                                        row col alert space result card date-picker
                                        checkbox divider typography title switch]]
   ["dayjs" :as dayjs]))

;; 表单步骤组件
(defn step-container [title & children]
  [:div {:style {:margin "16px 0"}}
   [:h2 {:style {:margin-bottom "16px"}} title]
   (into [:div {:style {:padding "16px"
                        :background "#fff"
                        :border "1px solid #e8e8e8"
                        :borderRadius "4px"}}]
         children)])

;; 表单底部导航组件
(defn form-navigation [current-step max-step submitting?]
  [form-item {:style {:marginTop "24px"}}
   [space
    (when (> current-step 0)
      [button {:onClick #(rf/dispatch [::events/prev-step])}
       "上一步"])
    (if (< current-step max-step)
      [button {:type "primary"
               :onClick #(rf/dispatch [::events/next-step])}
       "下一步"]
      [button {:type "primary"
               :loading submitting?
               :onClick #(rf/dispatch [::events/validate-and-submit])}
       "提交"])]])

;; 基本信息步骤
(defn basic-info-step []
  (let [basic-info @(rf/subscribe [::subs/basic-info])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "基本信息"
     [form {:layout "vertical"}
      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "门诊号"
                   :validateStatus (when (get-in errors [:basic-info :outpatient-number]) "error")
                   :help (get-in errors [:basic-info :outpatient-number])}
         [input {:value (:outpatient-number basic-info)
                 :placeholder "请输入门诊号"
                 :onChange #(rf/dispatch [::events/update-form-field [:basic-info :outpatient-number] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "姓名"
                   :validateStatus (when (get-in errors [:basic-info :name]) "error")
                   :help (get-in errors [:basic-info :name])}
         [input {:value (:name basic-info)
                 :placeholder "请输入姓名"
                 :onChange #(rf/dispatch [::events/update-form-field [:basic-info :name] (.. % -target -value)])}]]]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "性别"
                   :validateStatus (when (get-in errors [:basic-info :gender]) "error")
                   :help (get-in errors [:basic-info :gender])}
         [radio-group {:value (:gender basic-info)
                      :onChange #(rf/dispatch [::events/update-form-field [:basic-info :gender] (.. % -target -value)])}
          [radio {:value "male"} "男"]
          [radio {:value "female"} "女"]]]]
       [col {:span 12}
        [form-item {:label "年龄"
                   :validateStatus (when (get-in errors [:basic-info :age]) "error")
                   :help (get-in errors [:basic-info :age])}
         [input-number {:value (:age basic-info)
                       :min 1
                       :max 120
                       :style {:width "100%"}
                       :placeholder "请输入年龄"
                       :onChange #(rf/dispatch [::events/update-form-field [:basic-info :age] %])}]]]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "病区"}
         [input {:value (:ward basic-info)
                :placeholder "请输入病区信息"
                :onChange #(rf/dispatch [::events/update-form-field [:basic-info :ward] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "电子健康卡号"}
         [input {:value (:health-card-number basic-info)
                :placeholder "请输入电子健康卡号"
                :onChange #(rf/dispatch [::events/update-form-field [:basic-info :health-card-number] (.. % -target -value)])}]]]]

      [row {:gutter 16}
       [col {:span 24}
        [form-item {:label "术前诊断"}
         [text-area {:value (:pre-op-diagnosis basic-info)
                   :rows 2
                   :placeholder "请输入术前诊断"
                   :onChange #(rf/dispatch [::events/update-form-field [:basic-info :pre-op-diagnosis] (.. % -target -value)])}]]]]

      [row {:gutter 16}
       [col {:span 24}
        [form-item {:label "拟施手术"}
         [text-area {:value (:planned-surgery basic-info)
                   :rows 2
                   :placeholder "请输入拟施手术"
                   :onChange #(rf/dispatch [::events/update-form-field [:basic-info :planned-surgery] (.. % -target -value)])}]]]]

      [form-navigation current-step 6 submitting?]]]))

;; 病史步骤
(defn medical-history-step []
  (let [medical-summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "病史信息"
     [form {:layout "vertical"}
      [form-item {:label "过敏史"}
       [row {:gutter 16}
        [col {:span 4}
         [switch {:checked (:allergy-history medical-summary)
                  :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :allergy-history]])}]]
        [col {:span 20}
         [:span (if (:allergy-history medical-summary) "有" "无")]]]]

      (when (:allergy-history medical-summary)
        [:<>
         [form-item {:label "过敏原"}
          [input {:value (:allergens medical-summary)
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergens] (.. % -target -value)])}]]

         [form-item {:label "最近发生过敏时间"}
          [input {:value (:allergy-time medical-summary)
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergy-time] (.. % -target -value)])}]]])

      [divider]

      [form-item {:label "吸烟史"}
       [row {:gutter 16}
        [col {:span 4}
         [switch {:checked (:smoking-history medical-summary)
                  :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :smoking-history]])}]]
        [col {:span 20}
         [:span (if (:smoking-history medical-summary) "有" "无")]]]]

      (when (:smoking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [form-item {:label "吸烟年数"}
            [input-number {:value (:smoking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-years] %])}]]]
          [col {:span 12}
           [form-item {:label "每天吸烟支数"}
            [input-number {:value (:smoking-per-day medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-per-day] %])}]]]]])

      [form-navigation current-step 6 submitting?]]]))

;; 一般情况步骤
(defn general-condition-step []
  (let [general-condition @(rf/subscribe [::subs/general-condition])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "一般情况"
     [form {:layout "vertical"}
      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "身高 (cm)"
                   :validateStatus (when (get-in errors [:general-condition :height]) "error")
                   :help (get-in errors [:general-condition :height])}
         [input-number {:value (:height general-condition)
                       :min 50
                       :max 250
                       :style {:width "100%"}
                       :onChange #(rf/dispatch [::events/update-form-field [:general-condition :height] %])}]]]
       [col {:span 12}
        [form-item {:label "体重 (kg)"
                   :validateStatus (when (get-in errors [:general-condition :weight]) "error")
                   :help (get-in errors [:general-condition :weight])}
         [input-number {:value (:weight general-condition)
                       :min 1
                       :max 300
                       :style {:width "100%"}
                       :onChange #(rf/dispatch [::events/update-form-field [:general-condition :weight] %])}]]]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "精神状态"
                   :validateStatus (when (get-in errors [:general-condition :mental-state]) "error")
                   :help (get-in errors [:general-condition :mental-state])}
         [select {:value (:mental-state general-condition)
                  :style {:width "100%"}
                  :placeholder "请选择精神状态"
                  :onChange #(rf/dispatch [::events/update-form-field [:general-condition :mental-state] %])}
          [option {:value "良好"} "良好"]
          [option {:value "一般"} "一般"]
          [option {:value "较差"} "较差"]]]]
       [col {:span 12}
        [form-item {:label "活动能力"
                   :validateStatus (when (get-in errors [:general-condition :activity-ability]) "error")
                   :help (get-in errors [:general-condition :activity-ability])}
         [select {:value (:activity-ability general-condition)
                  :style {:width "100%"}
                  :placeholder "请选择活动能力"
                  :onChange #(rf/dispatch [::events/update-form-field [:general-condition :activity-ability] %])}
          [option {:value "正常"} "正常"]
          [option {:value "轻度受限"} "轻度受限"]
          [option {:value "中度受限"} "中度受限"]
          [option {:value "重度受限"} "重度受限"]
          [option {:value "卧床不起"} "卧床不起"]]]]]]

     [form-item {:label "血压 (mmHg)"
                :validateStatus (when (or (get-in errors [:general-condition :blood-pressure :systolic])
                                          (get-in errors [:general-condition :blood-pressure :diastolic])) "error")
                :help (or (get-in errors [:general-condition :blood-pressure :systolic])
                          (get-in errors [:general-condition :blood-pressure :diastolic]))}
      [row {:gutter 8}
       [col {:span 11}
        [input-number {:value (get-in general-condition [:blood-pressure :systolic])
                      :placeholder "收缩压"
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :blood-pressure :systolic] %])}]]
       [col {:span 2 :style {:textAlign "center"}}
        [:span "/"]]
       [col {:span 11}
        [input-number {:value (get-in general-condition [:blood-pressure :diastolic])
                      :placeholder "舒张压"
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :blood-pressure :diastolic] %])}]]]]

     [row {:gutter 16}
      [col {:span 8}
       [form-item {:label "脉搏 (次/分)"
                  :validateStatus (when (get-in errors [:general-condition :pulse]) "error")
                  :help (get-in errors [:general-condition :pulse])}
        [input-number {:value (:pulse general-condition)
                      :min 30
                      :max 200
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :pulse] %])}]]]
      [col {:span 8}
       [form-item {:label "呼吸 (次/分)"
                  :validateStatus (when (get-in errors [:general-condition :respiration]) "error")
                  :help (get-in errors [:general-condition :respiration])}
        [input-number {:value (:respiration general-condition)
                      :min 5
                      :max 60
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :respiration] %])}]]]
      [col {:span 8}
       [form-item {:label "体温 (℃)"
                  :validateStatus (when (get-in errors [:general-condition :temperature]) "error")
                  :help (get-in errors [:general-condition :temperature])}
        [input-number {:value (:temperature general-condition)
                      :step 0.1
                      :min 35
                      :max 42
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :temperature] %])}]]]]

     [form-item {:label "血氧饱和度 (SpO2) %"
                :validateStatus (when (get-in errors [:general-condition :spo2]) "error")
                :help (get-in errors [:general-condition :spo2])}
      [input-number {:value (:spo2 general-condition)
                    :min 70
                    :max 100
                    :style {:width "100%"}
                    :onChange #(rf/dispatch [::events/update-form-field [:general-condition :spo2] %])}]]

     [form-navigation current-step 6 submitting?]]))

;; 病情摘要步骤
(defn medical-summary-step []
  (let [medical-summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "病情摘要"
     [form {:layout "vertical"}
      [form-item {:label "过敏史"
                  :validateStatus (when (get-in errors [:medical-summary :allergy-history]) "error")
                  :help (get-in errors [:medical-summary :allergy-history])}
       [row {:gutter 16}
        [col {:span 4}
         [radio-group {:value (if (:allergy-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :allergy-history]
                                               (= (.. % -target -value) "yes")])}
          [radio {:value "no"} "无"]
          [radio {:value "yes"} "有"]]]]]

      (when (:allergy-history medical-summary)
        [:<>
         [form-item {:label "过敏原"
                     :validateStatus (when (get-in errors [:medical-summary :allergens]) "error")
                     :help (get-in errors [:medical-summary :allergens])}
          [input {:value (:allergens medical-summary)
                  :placeholder "请输入过敏原"
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergens] (.. % -target -value)])}]]

         [form-item {:label "过敏时间"
                     :validateStatus (when (get-in errors [:medical-summary :allergy-time]) "error")
                     :help (get-in errors [:medical-summary :allergy-time])}
          [date-picker {:value (when (:allergy-time medical-summary)
                                (try
                                  (dayjs (:allergy-time medical-summary))
                                  (catch :default _
                                    nil)))
                       :placeholder "最近发生过敏时间"
                       :style {:width "100%"}
                       :format "YYYY-MM-DD"
                       :onChange #(rf/dispatch [::events/update-form-field
                                                [:medical-summary :allergy-time]
                                                (when %1 (.format %1 "YYYY-MM-DD"))])}]]])

      [divider]

      [form-item {:label "吸烟史"
                  :validateStatus (when (get-in errors [:medical-summary :smoking-history]) "error")
                  :help (get-in errors [:medical-summary :smoking-history])}
       [row {:gutter 16}
        [col {:span 4}
         [radio-group {:value (if (:smoking-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :smoking-history]
                                               (= (.. % -target -value) "yes")])}
          [radio {:value "no"} "无"]
          [radio {:value "yes"} "有"]]]]]

      (when (:smoking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [form-item {:label "吸烟年数"
                       :validateStatus (when (get-in errors [:medical-summary :smoking-years]) "error")
                       :help (get-in errors [:medical-summary :smoking-years])}
            [input-number {:value (:smoking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入年数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-years] %])}]]]
          [col {:span 12}
           [form-item {:label "每天吸烟支数"
                       :validateStatus (when (get-in errors [:medical-summary :smoking-per-day]) "error")
                       :help (get-in errors [:medical-summary :smoking-per-day])}
            [input-number {:value (:smoking-per-day medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入支数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-per-day] %])}]]]]])

      [divider]

      [form-item {:label "饮酒史"
                  :validateStatus (when (get-in errors [:medical-summary :drinking-history]) "error")
                  :help (get-in errors [:medical-summary :drinking-history])}
       [row {:gutter 16}
        [col {:span 4}
         [radio-group {:value (if (:drinking-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :drinking-history]
                                               (= (.. % -target -value) "yes")])}
          [radio {:value "no"} "无"]
          [radio {:value "yes"} "有"]]]]]

      (when (:drinking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [form-item {:label "饮酒年数"
                       :validateStatus (when (get-in errors [:medical-summary :drinking-years]) "error")
                       :help (get-in errors [:medical-summary :drinking-years])}
            [input-number {:value (:drinking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入年数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :drinking-years] %])}]]]
          [col {:span 12}
           [form-item {:label "每天饮酒量 (ml)"
                       :validateStatus (when (get-in errors [:medical-summary :drinking-ml-per-day]) "error")
                       :help (get-in errors [:medical-summary :drinking-ml-per-day])}
            [input-number {:value (:drinking-ml-per-day medical-summary)
                          :min 0
                          :max 3000
                          :style {:width "100%"}
                          :placeholder "请输入饮酒量"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :drinking-ml-per-day] %])}]]]]])

      [form-navigation current-step 6 submitting?]]]))

  ;; 并存疾病步骤
(defn comorbidities-step []
  (let [comorbidities @(rf/subscribe [::subs/comorbidities])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "并存疾病及检查"
     [form {:layout "vertical"}
      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "呼吸系统疾病"}
         [text-area {:value (:respiratory-disease comorbidities)
                    :rows 2
                    :placeholder "请描述呼吸系统疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :respiratory-disease] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "心血管疾病"}
         [text-area {:value (:cardiovascular-disease comorbidities)
                    :rows 2
                    :placeholder "请描述心血管疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :cardiovascular-disease] (.. % -target -value)])}]]]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "内分泌疾病"}
         [text-area {:value (:endocrine-disease comorbidities)
                    :rows 2
                    :placeholder "请描述内分泌疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :endocrine-disease] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "神经精神疾病"}
         [text-area {:value (:neuropsychiatric-disease comorbidities)
                    :rows 2
                    :placeholder "请描述神经精神疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :neuropsychiatric-disease] (.. % -target -value)])}]]]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "关节骨骼系统"}
         [text-area {:value (:skeletal-system comorbidities)
                    :rows 2
                    :placeholder "请描述关节骨骼系统情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :skeletal-system] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "家族恶性高热史"}
         [text-area {:value (:family-malignant-hyperthermia comorbidities)
                    :rows 2
                    :placeholder "请描述家族恶性高热史情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :family-malignant-hyperthermia] (.. % -target -value)])}]]]]

      [form-item {:label "既往麻醉、手术史"}
       [text-area {:value (:past-anesthesia-surgery comorbidities)
                  :rows 3
                  :placeholder "请描述既往麻醉、手术史情况"
                  :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :past-anesthesia-surgery] (.. % -target -value)])}]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "使用的特殊药物"}
         [text-area {:value (get-in comorbidities [:special-medications :used])
                    :rows 2
                    :placeholder "请描述使用的特殊药物情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :special-medications :used] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "最后一次用药时间"}
         [date-picker {:value (when (get-in comorbidities [:special-medications :last-time])
                               (try
                                 (dayjs (get-in comorbidities [:special-medications :last-time]))
                                 (catch :default _
                                   nil)))
                      :placeholder "最后一次用药时间"
                      :style {:width "100%"}
                      :format "YYYY-MM-DD HH:mm"
                      :showTime {:format "HH:mm"}
                      :onChange #(rf/dispatch [::events/update-form-field
                                               [:comorbidities :special-medications :last-time]
                                               (when % (.format % "YYYY-MM-DD HH:mm"))])}]]]]]

     [divider]

     [form-item {:label "心脏"}
      [text-area {:value (:heart comorbidities)
                 :rows 2
                 :placeholder "请描述心脏情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :heart] (.. % -target -value)])}]]

     [form-item {:label "肺脏"}
      [text-area {:value (:lung comorbidities)
                 :rows 2
                 :placeholder "请描述肺脏情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :lung] (.. % -target -value)])}]]

     [form-item {:label "气道"}
      [text-area {:value (:airway comorbidities)
                 :rows 2
                 :placeholder "请描述气道情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :airway] (.. % -target -value)])}]]

     [form-item {:label "牙齿"}
      [text-area {:value (:teeth comorbidities)
                 :rows 2
                 :placeholder "请描述牙齿情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :teeth] (.. % -target -value)])}]]

     [form-item {:label "脊柱四肢"}
      [text-area {:value (:spine-limbs comorbidities)
                 :rows 2
                 :placeholder "请描述脊柱四肢情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :spine-limbs] (.. % -target -value)])}]]

     [form-item {:label "其它"}
      [text-area {:value (:others comorbidities)
                 :rows 2
                 :placeholder "请描述其它情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :others] (.. % -target -value)])}]]

     [form-item {:label "神经"}
      [text-area {:value (:nerve comorbidities)
                 :rows 2
                 :placeholder "请描述神经情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :nerve] (.. % -target -value)])}]]

     [divider {:orientation "left"} "相关辅助检查检验结果"]

     [form-item {:label "胸片"}
      [text-area {:value (get-in comorbidities [:auxiliary-exam :chest-radiograph])
                 :rows 2
                 :placeholder "请描述胸片检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :chest-radiograph] (.. % -target -value)])}]]

     [form-item {:label "肺功能"}
      [text-area {:value (get-in comorbidities [:auxiliary-exam :pulmonary-function])
                 :rows 2
                 :placeholder "请描述肺功能检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :pulmonary-function] (.. % -target -value)])}]]

     [form-item {:label "心脏彩超"}
      [text-area {:value (get-in comorbidities [:auxiliary-exam :cardiac-ultrasound])
                 :rows 2
                 :placeholder "请描述心脏彩超检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :cardiac-ultrasound] (.. % -target -value)])}]]

     [form-item {:label "心电图"}
      [text-area {:value (get-in comorbidities [:auxiliary-exam :ecg])
                 :rows 2
                 :placeholder "请描述心电图检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :ecg] (.. % -target -value)])}]]

     [form-item {:label "其他"}
      [text-area {:value (get-in comorbidities [:auxiliary-exam :other])
                 :rows 2
                 :placeholder "请描述其他检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :other] (.. % -target -value)])}]]

     [form-navigation current-step 7 submitting?]]))

;; 麻醉计划步骤
(defn anesthesia-plan-step []
  (let [anesthesia-plan @(rf/subscribe [::subs/anesthesia-plan])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "麻醉计划"
     [form {:layout "vertical"}
      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "ASA分级"
                    :validateStatus (when (get-in errors [:anesthesia-plan :asa-grade]) "error")
                    :help (get-in errors [:anesthesia-plan :asa-grade])}
         [select {:value (:asa-grade anesthesia-plan)
                  :style {:width "100%"}
                  :placeholder "请选择ASA分级"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :asa-grade] %])}
          [option {:value "I"} "I级 - 健康人"]
          [option {:value "II"} "II级 - 轻度全身性疾病"]
          [option {:value "III"} "III级 - 严重全身性疾病"]
          [option {:value "IV"} "IV级 - 威胁生命的全身性疾病"]
          [option {:value "V"} "V级 - 濒死病人"]
          [option {:value "VI"} "VI级 - 脑死亡病人"]]]]
       [col {:span 12}
        [form-item {:label "心功能分级(NYHA)"
                    :validateStatus (when (get-in errors [:anesthesia-plan :heart-function]) "error")
                    :help (get-in errors [:anesthesia-plan :heart-function])}
         [select {:value (:heart-function anesthesia-plan)
                  :style {:width "100%"}
                  :placeholder "请选择心功能分级"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :heart-function] %])}
          [option {:value "I"} "I级 - 无症状"]
          [option {:value "II"} "II级 - 轻度症状"]
          [option {:value "III"} "III级 - 明显症状"]
          [option {:value "IV"} "IV级 - 症状严重"]]]]]

      [form-item {:label "拟行麻醉方式"
                  :validateStatus (when (get-in errors [:anesthesia-plan :anesthesia-method]) "error")
                  :help (get-in errors [:anesthesia-plan :anesthesia-method])}
       [select {:value (:anesthesia-method anesthesia-plan)
                :style {:width "100%"}
                :placeholder "请选择麻醉方式"
                :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :anesthesia-method] %])}
        [option {:value "全身麻醉"} "全身麻醉"]
        [option {:value "椎管内麻醉"} "椎管内麻醉"]
        [option {:value "神经阻滞麻醉"} "神经阻滞麻醉"]
        [option {:value "局部麻醉"} "局部麻醉"]
        [option {:value "静脉麻醉"} "静脉麻醉"]
        [option {:value "联合麻醉"} "联合麻醉"]]]

      [form-item {:label "监测项目"
                  :validateStatus (when (get-in errors [:anesthesia-plan :monitoring-items]) "error")
                  :help (get-in errors [:anesthesia-plan :monitoring-items])}
       [text-area {:value (:monitoring-items anesthesia-plan)
                  :rows 2
                  :placeholder "请填写监测项目"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :monitoring-items] (.. % -target -value)])}]]

      [form-item {:label "特殊技术"
                  :validateStatus (when (get-in errors [:anesthesia-plan :special-techniques]) "error")
                  :help (get-in errors [:anesthesia-plan :special-techniques])}
       [text-area {:value (:special-techniques anesthesia-plan)
                  :rows 2
                  :placeholder "请填写特殊技术"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :special-techniques] (.. % -target -value)])}]]

      [divider {:orientation "left"} "其他"]

      [form-item {:label "术前麻醉医嘱"}
       [row {:gutter 16}
        [col {:span 12}
         [form-item {:label "术前禁食"
                     :validateStatus (when (get-in errors [:anesthesia-plan :pre-op-fasting]) "error")
                     :help (get-in errors [:anesthesia-plan :pre-op-fasting])}
          [input-number {:value (:pre-op-fasting anesthesia-plan)
                        :min 0
                        :max 24
                        :addonAfter "小时"
                        :style {:width "100%"}
                        :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :pre-op-fasting] %])}]]]
        [col {:span 12}
         [form-item {:label "术前禁饮"
                     :validateStatus (when (get-in errors [:anesthesia-plan :pre-op-water-restriction]) "error")
                     :help (get-in errors [:anesthesia-plan :pre-op-water-restriction])}
          [input-number {:value (:pre-op-water-restriction anesthesia-plan)
                        :min 0
                        :max 24
                        :addonAfter "小时"
                        :style {:width "100%"}
                        :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :pre-op-water-restriction] %])}]]]]]

      [form-item {:label "术日晨继续应用药物"
                  :validateStatus (when (get-in errors [:anesthesia-plan :continue-medication]) "error")
                  :help (get-in errors [:anesthesia-plan :continue-medication])}
       [text-area {:value (:continue-medication anesthesia-plan)
                  :rows 2
                  :placeholder "需进一步检查"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :continue-medication] (.. % -target -value)])}]]

      [form-item {:label "术日晨停用药物"
                  :validateStatus (when (get-in errors [:anesthesia-plan :discontinue-medication]) "error")
                  :help (get-in errors [:anesthesia-plan :discontinue-medication])}
       [text-area {:value (:discontinue-medication anesthesia-plan)
                  :rows 2
                  :placeholder "需进一步会诊"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :discontinue-medication] (.. % -target -value)])}]]

      [form-item {:label "麻醉中需注意的问题"
                  :validateStatus (when (get-in errors [:anesthesia-plan :anesthesia-notes]) "error")
                  :help (get-in errors [:anesthesia-plan :anesthesia-notes])}
       [text-area {:value (:anesthesia-notes anesthesia-plan)
                  :rows 3
                  :placeholder "请填写麻醉中需注意的问题"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :anesthesia-notes] (.. % -target -value)])}]]

      [row {:gutter 16}
       [col {:span 12}
        [form-item {:label "麻醉医师签名"
                    :validateStatus (when (get-in errors [:anesthesia-plan :physician-signature]) "error")
                    :help (get-in errors [:anesthesia-plan :physician-signature])}
         [input {:value (:physician-signature anesthesia-plan)
                 :placeholder "请输入麻醉医师姓名"
                 :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :physician-signature] (.. % -target -value)])}]]]
       [col {:span 12}
        [form-item {:label "评估日期"
                    :validateStatus (when (get-in errors [:anesthesia-plan :assessment-date]) "error")
                    :help (get-in errors [:anesthesia-plan :assessment-date])}
         [date-picker {:value (when (:assessment-date anesthesia-plan)
                               (try
                                 (dayjs (:assessment-date anesthesia-plan))
                                 (catch :default _
                                   nil)))
                      :placeholder "请选择日期"
                      :style {:width "100%"}
                      :format "YYYY-MM-DD"
                      :onChange #(rf/dispatch [::events/update-form-field
                                               [:anesthesia-plan :assessment-date]
                                               (when % (.format % "YYYY-MM-DD"))])}]]]]

      [form-navigation current-step 7 submitting?]]]))

;; 成功页面
(defn success-page []
  [result {:status "success"
           :title "提交成功！"
           :subTitle "您的术前评估信息已成功提交，医生将会尽快审核。"
           :extra [[button {:type "primary"
                            :key "home"
                            :onClick #(js/window.location.reload)}
                    "返回首页"]]}])

;; 主表单组件
(defn patient-form []
  (let [current-step @(rf/subscribe [::subs/current-step])
        submit-success? @(rf/subscribe [::subs/submit-success?])]
    (if submit-success?
      [success-page]
      [:div {:style {:maxWidth "800px" :margin "0 auto"}}
       [steps {:current current-step
               :style {:marginBottom "24px"}}
        [step {:title "基本信息"}]
        [step {:title "一般情况"}]
        [step {:title "病情摘要"}]
        [step {:title "并存疾病"}]
        [step {:title "麻醉计划"}]]

       (case current-step
         0 [basic-info-step]
         1 [general-condition-step]
         2 [medical-summary-step]
         3 [comorbidities-step]
         4 [anesthesia-plan-step]
         [basic-info-step])])))
