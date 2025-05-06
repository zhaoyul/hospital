(ns hc.hospital.patient.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hc.hospital.patient.events :as events]
   [hc.hospital.patient.subs :as subs]
   [clojure.string :as str]
   [hc.hospital.utils :as utils]
   ["antd" :as antd]
   ["dayjs" :as dayjs]))

;; Ant Design 组件引用
(def Button (r/adapt-react-class (.-Button antd)))
(def Form (r/adapt-react-class (.-Form antd)))
(def FormItem (r/adapt-react-class (.-Item (.-Form antd))))
(def Input (r/adapt-react-class (.-Input antd)))
(def InputNumber (r/adapt-react-class (.-InputNumber antd)))
(def TextArea (r/adapt-react-class (.-TextArea (.-Input antd))))
(def Radio (r/adapt-react-class (.-Radio antd)))
(def RadioGroup (r/adapt-react-class (.-Group (.-Radio antd))))
(def Select (r/adapt-react-class (.-Select antd)))
(def Option (r/adapt-react-class (.-Option (.-Select antd))))
(def Steps (r/adapt-react-class (.-Steps antd)))
(def Step (r/adapt-react-class (.-Step (.-Steps antd))))
(def Row (r/adapt-react-class (.-Row antd)))
(def Col (r/adapt-react-class (.-Col antd)))
(def Alert (r/adapt-react-class (.-Alert antd)))
(def Space (r/adapt-react-class (.-Space antd)))
(def Result (r/adapt-react-class (.-Result antd)))
(def Card (r/adapt-react-class (.-Card antd)))
(def DatePicker (r/adapt-react-class (.-DatePicker antd)))
(def Checkbox (r/adapt-react-class (.-Checkbox antd)))
(def Divider (r/adapt-react-class (.-Divider antd)))
(def Typography (r/adapt-react-class (.-Typography antd)))
(def Title (r/adapt-react-class (.-Title (.-Typography antd))))
(def Switch (r/adapt-react-class (.-Switch antd)))

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
  [FormItem {:style {:marginTop "24px"}}
   [Space
    (when (> current-step 0)
      [Button {:onClick #(rf/dispatch [::events/prev-step])}
       "上一步"])
    (if (< current-step max-step)
      [Button {:type "primary"
               :onClick #(rf/dispatch [::events/next-step])}
       "下一步"]
      [Button {:type "primary"
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
     [Form {:layout "vertical"}
      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "门诊号"
                   :validateStatus (when (get-in errors [:basic-info :outpatient-number]) "error")
                   :help (get-in errors [:basic-info :outpatient-number])}
         [Input {:value (:outpatient-number basic-info)
                 :placeholder "请输入门诊号"
                 :onChange #(rf/dispatch [::events/update-form-field [:basic-info :outpatient-number] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "姓名"
                   :validateStatus (when (get-in errors [:basic-info :name]) "error")
                   :help (get-in errors [:basic-info :name])}
         [Input {:value (:name basic-info)
                 :placeholder "请输入姓名"
                 :onChange #(rf/dispatch [::events/update-form-field [:basic-info :name] (.. % -target -value)])}]]]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "性别"
                   :validateStatus (when (get-in errors [:basic-info :gender]) "error")
                   :help (get-in errors [:basic-info :gender])}
         [RadioGroup {:value (:gender basic-info)
                      :onChange #(rf/dispatch [::events/update-form-field [:basic-info :gender] (.. % -target -value)])}
          [Radio {:value "male"} "男"]
          [Radio {:value "female"} "女"]]]]
       [Col {:span 12}
        [FormItem {:label "年龄"
                   :validateStatus (when (get-in errors [:basic-info :age]) "error")
                   :help (get-in errors [:basic-info :age])}
         [InputNumber {:value (:age basic-info)
                       :min 1
                       :max 120
                       :style {:width "100%"}
                       :placeholder "请输入年龄"
                       :onChange #(rf/dispatch [::events/update-form-field [:basic-info :age] %])}]]]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "病区"}
         [Input {:value (:ward basic-info)
                :placeholder "请输入病区信息"
                :onChange #(rf/dispatch [::events/update-form-field [:basic-info :ward] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "电子健康卡号"}
         [Input {:value (:health-card-number basic-info)
                :placeholder "请输入电子健康卡号"
                :onChange #(rf/dispatch [::events/update-form-field [:basic-info :health-card-number] (.. % -target -value)])}]]]]

      [Row {:gutter 16}
       [Col {:span 24}
        [FormItem {:label "术前诊断"}
         [TextArea {:value (:pre-op-diagnosis basic-info)
                   :rows 2
                   :placeholder "请输入术前诊断"
                   :onChange #(rf/dispatch [::events/update-form-field [:basic-info :pre-op-diagnosis] (.. % -target -value)])}]]]]

      [Row {:gutter 16}
       [Col {:span 24}
        [FormItem {:label "拟施手术"}
         [TextArea {:value (:planned-surgery basic-info)
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
     [Form {:layout "vertical"}
      [FormItem {:label "过敏史"}
       [Row {:gutter 16}
        [Col {:span 4}
         [Switch {:checked (:allergy-history medical-summary)
                  :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :allergy-history]])}]]
        [Col {:span 20}
         [:span (if (:allergy-history medical-summary) "有" "无")]]]]

      (when (:allergy-history medical-summary)
        [:<>
         [FormItem {:label "过敏原"}
          [Input {:value (:allergens medical-summary)
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergens] (.. % -target -value)])}]]

         [FormItem {:label "最近发生过敏时间"}
          [Input {:value (:allergy-time medical-summary)
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergy-time] (.. % -target -value)])}]]])

      [Divider]

      [FormItem {:label "吸烟史"}
       [Row {:gutter 16}
        [Col {:span 4}
         [Switch {:checked (:smoking-history medical-summary)
                  :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :smoking-history]])}]]
        [Col {:span 20}
         [:span (if (:smoking-history medical-summary) "有" "无")]]]]

      (when (:smoking-history medical-summary)
        [:<>
         [Row {:gutter 16}
          [Col {:span 12}
           [FormItem {:label "吸烟年数"}
            [InputNumber {:value (:smoking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-years] %])}]]]
          [Col {:span 12}
           [FormItem {:label "每天吸烟支数"}
            [InputNumber {:value (:smoking-per-day medical-summary)
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
     [Form {:layout "vertical"}
      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "身高 (cm)"
                   :validateStatus (when (get-in errors [:general-condition :height]) "error")
                   :help (get-in errors [:general-condition :height])}
         [InputNumber {:value (:height general-condition)
                       :min 50
                       :max 250
                       :style {:width "100%"}
                       :onChange #(rf/dispatch [::events/update-form-field [:general-condition :height] %])}]]]
       [Col {:span 12}
        [FormItem {:label "体重 (kg)"
                   :validateStatus (when (get-in errors [:general-condition :weight]) "error")
                   :help (get-in errors [:general-condition :weight])}
         [InputNumber {:value (:weight general-condition)
                       :min 1
                       :max 300
                       :style {:width "100%"}
                       :onChange #(rf/dispatch [::events/update-form-field [:general-condition :weight] %])}]]]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "精神状态"
                   :validateStatus (when (get-in errors [:general-condition :mental-state]) "error")
                   :help (get-in errors [:general-condition :mental-state])}
         [Select {:value (:mental-state general-condition)
                  :style {:width "100%"}
                  :placeholder "请选择精神状态"
                  :onChange #(rf/dispatch [::events/update-form-field [:general-condition :mental-state] %])}
          [Option {:value "良好"} "良好"]
          [Option {:value "一般"} "一般"]
          [Option {:value "较差"} "较差"]]]]
       [Col {:span 12}
        [FormItem {:label "活动能力"
                   :validateStatus (when (get-in errors [:general-condition :activity-ability]) "error")
                   :help (get-in errors [:general-condition :activity-ability])}
         [Select {:value (:activity-ability general-condition)
                  :style {:width "100%"}
                  :placeholder "请选择活动能力"
                  :onChange #(rf/dispatch [::events/update-form-field [:general-condition :activity-ability] %])}
          [Option {:value "正常"} "正常"]
          [Option {:value "轻度受限"} "轻度受限"]
          [Option {:value "中度受限"} "中度受限"]
          [Option {:value "重度受限"} "重度受限"]
          [Option {:value "卧床不起"} "卧床不起"]]]]]]

     [FormItem {:label "血压 (mmHg)"
                :validateStatus (when (or (get-in errors [:general-condition :blood-pressure :systolic])
                                          (get-in errors [:general-condition :blood-pressure :diastolic])) "error")
                :help (or (get-in errors [:general-condition :blood-pressure :systolic])
                          (get-in errors [:general-condition :blood-pressure :diastolic]))}
      [Row {:gutter 8}
       [Col {:span 11}
        [InputNumber {:value (get-in general-condition [:blood-pressure :systolic])
                      :placeholder "收缩压"
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :blood-pressure :systolic] %])}]]
       [Col {:span 2 :style {:textAlign "center"}}
        [:span "/"]]
       [Col {:span 11}
        [InputNumber {:value (get-in general-condition [:blood-pressure :diastolic])
                      :placeholder "舒张压"
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :blood-pressure :diastolic] %])}]]]]

     [Row {:gutter 16}
      [Col {:span 8}
       [FormItem {:label "脉搏 (次/分)"
                  :validateStatus (when (get-in errors [:general-condition :pulse]) "error")
                  :help (get-in errors [:general-condition :pulse])}
        [InputNumber {:value (:pulse general-condition)
                      :min 30
                      :max 200
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :pulse] %])}]]]
      [Col {:span 8}
       [FormItem {:label "呼吸 (次/分)"
                  :validateStatus (when (get-in errors [:general-condition :respiration]) "error")
                  :help (get-in errors [:general-condition :respiration])}
        [InputNumber {:value (:respiration general-condition)
                      :min 5
                      :max 60
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :respiration] %])}]]]
      [Col {:span 8}
       [FormItem {:label "体温 (℃)"
                  :validateStatus (when (get-in errors [:general-condition :temperature]) "error")
                  :help (get-in errors [:general-condition :temperature])}
        [InputNumber {:value (:temperature general-condition)
                      :step 0.1
                      :min 35
                      :max 42
                      :style {:width "100%"}
                      :onChange #(rf/dispatch [::events/update-form-field [:general-condition :temperature] %])}]]]]

     [FormItem {:label "血氧饱和度 (SpO2) %"
                :validateStatus (when (get-in errors [:general-condition :spo2]) "error")
                :help (get-in errors [:general-condition :spo2])}
      [InputNumber {:value (:spo2 general-condition)
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
     [Form {:layout "vertical"}
      [FormItem {:label "过敏史"
                 :validateStatus (when (get-in errors [:medical-summary :allergy-history]) "error")
                 :help (get-in errors [:medical-summary :allergy-history])}
       [Row {:gutter 16}
        [Col {:span 4}
         [RadioGroup {:value (if (:allergy-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :allergy-history]
                                               (= (.. % -target -value) "yes")])}
          [Radio {:value "no"} "无"]
          [Radio {:value "yes"} "有"]]]]]

      (when (:allergy-history medical-summary)
        [:<>
         [FormItem {:label "过敏原"
                    :validateStatus (when (get-in errors [:medical-summary :allergens]) "error")
                    :help (get-in errors [:medical-summary :allergens])}
          [Input {:value (:allergens medical-summary)
                  :placeholder "请输入过敏原"
                  :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :allergens] (.. % -target -value)])}]]

         [FormItem {:label "过敏时间"
                    :validateStatus (when (get-in errors [:medical-summary :allergy-time]) "error")
                    :help (get-in errors [:medical-summary :allergy-time])}
          [DatePicker {:value (dayjs (:allergy-time medical-summary))
                       :placeholder "最近发生过敏时间"
                       :style {:width "100%"}
                       :format "YYYY-MM-DD"
                       :onChange #(rf/dispatch [::events/update-form-field
                                                [:medical-summary :allergy-time]
                                                (when %1 (.format %1 "YYYY-MM-DD"))])}]]])

      [Divider]

      [FormItem {:label "吸烟史"
                 :validateStatus (when (get-in errors [:medical-summary :smoking-history]) "error")
                 :help (get-in errors [:medical-summary :smoking-history])}
       [Row {:gutter 16}
        [Col {:span 4}
         [RadioGroup {:value (if (:smoking-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :smoking-history]
                                               (= (.. % -target -value) "yes")])}
          [Radio {:value "no"} "无"]
          [Radio {:value "yes"} "有"]]]]]

      (when (:smoking-history medical-summary)
        [:<>
         [Row {:gutter 16}
          [Col {:span 12}
           [FormItem {:label "吸烟年数"
                      :validateStatus (when (get-in errors [:medical-summary :smoking-years]) "error")
                      :help (get-in errors [:medical-summary :smoking-years])}
            [InputNumber {:value (:smoking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入年数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-years] %])}]]]
          [Col {:span 12}
           [FormItem {:label "每天吸烟支数"
                      :validateStatus (when (get-in errors [:medical-summary :smoking-per-day]) "error")
                      :help (get-in errors [:medical-summary :smoking-per-day])}
            [InputNumber {:value (:smoking-per-day medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入支数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :smoking-per-day] %])}]]]]])

      [Divider]

      [FormItem {:label "饮酒史"
                 :validateStatus (when (get-in errors [:medical-summary :drinking-history]) "error")
                 :help (get-in errors [:medical-summary :drinking-history])}
       [Row {:gutter 16}
        [Col {:span 4}
         [RadioGroup {:value (if (:drinking-history medical-summary) "yes" "no")
                      :onChange #(rf/dispatch [::events/toggle-boolean-field [:medical-summary :drinking-history]
                                               (= (.. % -target -value) "yes")])}
          [Radio {:value "no"} "无"]
          [Radio {:value "yes"} "有"]]]]]

      (when (:drinking-history medical-summary)
        [:<>
         [Row {:gutter 16}
          [Col {:span 12}
           [FormItem {:label "饮酒年数"
                      :validateStatus (when (get-in errors [:medical-summary :drinking-years]) "error")
                      :help (get-in errors [:medical-summary :drinking-years])}
            [InputNumber {:value (:drinking-years medical-summary)
                          :min 0
                          :max 100
                          :style {:width "100%"}
                          :placeholder "请输入年数"
                          :onChange #(rf/dispatch [::events/update-form-field [:medical-summary :drinking-years] %])}]]]
          [Col {:span 12}
           [FormItem {:label "每天饮酒量 (ml)"
                      :validateStatus (when (get-in errors [:medical-summary :drinking-ml-per-day]) "error")
                      :help (get-in errors [:medical-summary :drinking-ml-per-day])}
            [InputNumber {:value (:drinking-ml-per-day medical-summary)
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
     [Form {:layout "vertical"}
      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "呼吸系统疾病"}
         [TextArea {:value (:respiratory-disease comorbidities)
                    :rows 2
                    :placeholder "请描述呼吸系统疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :respiratory-disease] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "心血管疾病"}
         [TextArea {:value (:cardiovascular-disease comorbidities)
                    :rows 2
                    :placeholder "请描述心血管疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :cardiovascular-disease] (.. % -target -value)])}]]]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "内分泌疾病"}
         [TextArea {:value (:endocrine-disease comorbidities)
                    :rows 2
                    :placeholder "请描述内分泌疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :endocrine-disease] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "神经精神疾病"}
         [TextArea {:value (:neuropsychiatric-disease comorbidities)
                    :rows 2
                    :placeholder "请描述神经精神疾病情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :neuropsychiatric-disease] (.. % -target -value)])}]]]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "关节骨骼系统"}
         [TextArea {:value (:skeletal-system comorbidities)
                    :rows 2
                    :placeholder "请描述关节骨骼系统情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :skeletal-system] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "家族恶性高热史"}
         [TextArea {:value (:family-malignant-hyperthermia comorbidities)
                    :rows 2
                    :placeholder "请描述家族恶性高热史情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :family-malignant-hyperthermia] (.. % -target -value)])}]]]]

      [FormItem {:label "既往麻醉、手术史"}
       [TextArea {:value (:past-anesthesia-surgery comorbidities)
                  :rows 3
                  :placeholder "请描述既往麻醉、手术史情况"
                  :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :past-anesthesia-surgery] (.. % -target -value)])}]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "使用的特殊药物"}
         [TextArea {:value (get-in comorbidities [:special-medications :used])
                    :rows 2
                    :placeholder "请描述使用的特殊药物情况"
                    :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :special-medications :used] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "最后一次用药时间"}
         [DatePicker {:value (dayjs (get-in comorbidities [:special-medications :last-time]))
                      :placeholder "最后一次用药时间"
                      :style {:width "100%"}
                      :format "YYYY-MM-DD HH:mm"
                      :showTime {:format "HH:mm"}
                      :onChange #(rf/dispatch [::events/update-form-field
                                               [:comorbidities :special-medications :last-time]
                                               (when % (.format % "YYYY-MM-DD HH:mm"))])}]]]]]

     [Divider]

     [FormItem {:label "心脏"}
      [TextArea {:value (:heart comorbidities)
                 :rows 2
                 :placeholder "请描述心脏情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :heart] (.. % -target -value)])}]]

     [FormItem {:label "肺脏"}
      [TextArea {:value (:lung comorbidities)
                 :rows 2
                 :placeholder "请描述肺脏情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :lung] (.. % -target -value)])}]]

     [FormItem {:label "气道"}
      [TextArea {:value (:airway comorbidities)
                 :rows 2
                 :placeholder "请描述气道情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :airway] (.. % -target -value)])}]]

     [FormItem {:label "牙齿"}
      [TextArea {:value (:teeth comorbidities)
                 :rows 2
                 :placeholder "请描述牙齿情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :teeth] (.. % -target -value)])}]]

     [FormItem {:label "脊柱四肢"}
      [TextArea {:value (:spine-limbs comorbidities)
                 :rows 2
                 :placeholder "请描述脊柱四肢情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :spine-limbs] (.. % -target -value)])}]]

     [FormItem {:label "其它"}
      [TextArea {:value (:others comorbidities)
                 :rows 2
                 :placeholder "请描述其它情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :others] (.. % -target -value)])}]]

     [FormItem {:label "神经"}
      [TextArea {:value (:nerve comorbidities)
                 :rows 2
                 :placeholder "请描述神经情况"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :nerve] (.. % -target -value)])}]]

     [Divider {:orientation "left"} "相关辅助检查检验结果"]

     [FormItem {:label "胸片"}
      [TextArea {:value (get-in comorbidities [:auxiliary-exam :chest-radiograph])
                 :rows 2
                 :placeholder "请描述胸片检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :chest-radiograph] (.. % -target -value)])}]]

     [FormItem {:label "肺功能"}
      [TextArea {:value (get-in comorbidities [:auxiliary-exam :pulmonary-function])
                 :rows 2
                 :placeholder "请描述肺功能检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :pulmonary-function] (.. % -target -value)])}]]

     [FormItem {:label "心脏彩超"}
      [TextArea {:value (get-in comorbidities [:auxiliary-exam :cardiac-ultrasound])
                 :rows 2
                 :placeholder "请描述心脏彩超检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :cardiac-ultrasound] (.. % -target -value)])}]]

     [FormItem {:label "心电图"}
      [TextArea {:value (get-in comorbidities [:auxiliary-exam :ecg])
                 :rows 2
                 :placeholder "请描述心电图检查结果"
                 :onChange #(rf/dispatch [::events/update-form-field [:comorbidities :auxiliary-exam :ecg] (.. % -target -value)])}]]

     [FormItem {:label "其他"}
      [TextArea {:value (get-in comorbidities [:auxiliary-exam :other])
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
     [Form {:layout "vertical"}
      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "ASA分级"
                   :validateStatus (when (get-in errors [:anesthesia-plan :asa-grade]) "error")
                   :help (get-in errors [:anesthesia-plan :asa-grade])}
         [Select {:value (:asa-grade anesthesia-plan)
                  :style {:width "100%"}
                  :placeholder "请选择ASA分级"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :asa-grade] %])}
          [Option {:value "I"} "I级 - 健康人"]
          [Option {:value "II"} "II级 - 轻度全身性疾病"]
          [Option {:value "III"} "III级 - 严重全身性疾病"]
          [Option {:value "IV"} "IV级 - 威胁生命的全身性疾病"]
          [Option {:value "V"} "V级 - 濒死病人"]
          [Option {:value "VI"} "VI级 - 脑死亡病人"]]]]
       [Col {:span 12}
        [FormItem {:label "心功能分级(NYHA)"
                   :validateStatus (when (get-in errors [:anesthesia-plan :heart-function]) "error")
                   :help (get-in errors [:anesthesia-plan :heart-function])}
         [Select {:value (:heart-function anesthesia-plan)
                  :style {:width "100%"}
                  :placeholder "请选择心功能分级"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :heart-function] %])}
          [Option {:value "I"} "I级 - 无症状"]
          [Option {:value "II"} "II级 - 轻度症状"]
          [Option {:value "III"} "III级 - 明显症状"]
          [Option {:value "IV"} "IV级 - 症状严重"]]]]]

      [FormItem {:label "拟行麻醉方式"
                 :validateStatus (when (get-in errors [:anesthesia-plan :anesthesia-method]) "error")
                 :help (get-in errors [:anesthesia-plan :anesthesia-method])}
       [Select {:value (:anesthesia-method anesthesia-plan)
                :style {:width "100%"}
                :placeholder "请选择麻醉方式"
                :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :anesthesia-method] %])}
        [Option {:value "全身麻醉"} "全身麻醉"]
        [Option {:value "椎管内麻醉"} "椎管内麻醉"]
        [Option {:value "神经阻滞麻醉"} "神经阻滞麻醉"]
        [Option {:value "局部麻醉"} "局部麻醉"]
        [Option {:value "静脉麻醉"} "静脉麻醉"]
        [Option {:value "联合麻醉"} "联合麻醉"]]]

      [FormItem {:label "监测项目"
                 :validateStatus (when (get-in errors [:anesthesia-plan :monitoring-items]) "error")
                 :help (get-in errors [:anesthesia-plan :monitoring-items])}
       [TextArea {:value (:monitoring-items anesthesia-plan)
                  :rows 2
                  :placeholder "请填写监测项目"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :monitoring-items] (.. % -target -value)])}]]

      [FormItem {:label "特殊技术"
                 :validateStatus (when (get-in errors [:anesthesia-plan :special-techniques]) "error")
                 :help (get-in errors [:anesthesia-plan :special-techniques])}
       [TextArea {:value (:special-techniques anesthesia-plan)
                  :rows 2
                  :placeholder "请填写特殊技术"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :special-techniques] (.. % -target -value)])}]]

      [Divider {:orientation "left"} "其他"]

      [FormItem {:label "术前麻醉医嘱"}
       [Row {:gutter 16}
        [Col {:span 12}
         [FormItem {:label "术前禁食"
                    :validateStatus (when (get-in errors [:anesthesia-plan :pre-op-fasting]) "error")
                    :help (get-in errors [:anesthesia-plan :pre-op-fasting])}
          [InputNumber {:value (:pre-op-fasting anesthesia-plan)
                        :min 0
                        :max 24
                        :addonAfter "小时"
                        :style {:width "100%"}
                        :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :pre-op-fasting] %])}]]]
        [Col {:span 12}
         [FormItem {:label "术前禁饮"
                    :validateStatus (when (get-in errors [:anesthesia-plan :pre-op-water-restriction]) "error")
                    :help (get-in errors [:anesthesia-plan :pre-op-water-restriction])}
          [InputNumber {:value (:pre-op-water-restriction anesthesia-plan)
                        :min 0
                        :max 24
                        :addonAfter "小时"
                        :style {:width "100%"}
                        :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :pre-op-water-restriction] %])}]]]]]

      [FormItem {:label "术日晨继续应用药物"
                 :validateStatus (when (get-in errors [:anesthesia-plan :continue-medication]) "error")
                 :help (get-in errors [:anesthesia-plan :continue-medication])}
       [TextArea {:value (:continue-medication anesthesia-plan)
                  :rows 2
                  :placeholder "需进一步检查"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :continue-medication] (.. % -target -value)])}]]

      [FormItem {:label "术日晨停用药物"
                 :validateStatus (when (get-in errors [:anesthesia-plan :discontinue-medication]) "error")
                 :help (get-in errors [:anesthesia-plan :discontinue-medication])}
       [TextArea {:value (:discontinue-medication anesthesia-plan)
                  :rows 2
                  :placeholder "需进一步会诊"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :discontinue-medication] (.. % -target -value)])}]]

      [FormItem {:label "麻醉中需注意的问题"
                 :validateStatus (when (get-in errors [:anesthesia-plan :anesthesia-notes]) "error")
                 :help (get-in errors [:anesthesia-plan :anesthesia-notes])}
       [TextArea {:value (:anesthesia-notes anesthesia-plan)
                  :rows 3
                  :placeholder "请填写麻醉中需注意的问题"
                  :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :anesthesia-notes] (.. % -target -value)])}]]

      [Row {:gutter 16}
       [Col {:span 12}
        [FormItem {:label "麻醉医师签名"
                   :validateStatus (when (get-in errors [:anesthesia-plan :physician-signature]) "error")
                   :help (get-in errors [:anesthesia-plan :physician-signature])}
         [Input {:value (:physician-signature anesthesia-plan)
                 :placeholder "请输入麻醉医师姓名"
                 :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :physician-signature] (.. % -target -value)])}]]]
       [Col {:span 12}
        [FormItem {:label "评估日期"
                   :validateStatus (when (get-in errors [:anesthesia-plan :assessment-date]) "error")
                   :help (get-in errors [:anesthesia-plan :assessment-date])}
         [DatePicker {:value (dayjs (:assessment-date anesthesia-plan))
                      :placeholder "请选择日期"
                      :style {:width "100%"}
                      :format "YYYY-MM-DD"
                      :onChange #(rf/dispatch [::events/update-form-field [:anesthesia-plan :assessment-date] (when % (.format % "YYYY-MM-DD"))])}]]]]

      [form-navigation current-step 7 submitting?]]]))

;; 成功页面
(defn success-page []
  [Result {:status "success"
           :title "提交成功！"
           :subTitle "您的术前评估信息已成功提交，医生将会尽快审核。"
           :extra [[Button {:type "primary"
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
       [Steps {:current current-step
               :style {:marginBottom "24px"}}
        [Step {:title "基本信息"}]
        [Step {:title "一般情况"}]
        [Step {:title "病情摘要"}]
        [Step {:title "并存疾病"}]
        [Step {:title "麻醉计划"}]]

       (case current-step
         0 [basic-info-step]
         1 [general-condition-step]
         2 [medical-summary-step]
         3 [comorbidities-step]
         4 [anesthesia-plan-step]
         [basic-info-step])])))
