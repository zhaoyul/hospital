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

;; --- 新的表单 UI 帮助组件 ---
(defn ui-input-item [{:keys [label value placeholder errors field-key data-path]}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [input {:value value
             :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
             :onChange #(rf/dispatch [::events/update-form-field full-path (.. % -target -value)])}]]))

(defn ui-text-area-item [{:keys [label value placeholder errors field-key data-path rows]
                          :or {rows 2}}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [text-area {:value value
                 :rows rows
                 :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                 :onChange #(rf/dispatch [::events/update-form-field full-path (.. % -target -value)])}]]))

(defn ui-input-number-item [{:keys [label value placeholder errors field-key data-path min max step style addonAfter]
                             :or {style {:width "100%"}}}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [input-number {:value value
                    :min min
                    :max max
                    :step step
                    :style style
                    :addonAfter addonAfter
                    :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                    :onChange #(rf/dispatch [::events/update-form-field full-path %])}]]))

(defn ui-select-item [{:keys [label value placeholder errors field-key data-path options style]
                       :or {style {:width "100%"}}}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     (into [select {:value value
                     :style style
                     :placeholder (or placeholder (str "请选择" (if (string? label) label "")))
                     :onChange #(rf/dispatch [::events/update-form-field full-path %])}]
           (for [{opt-val :value opt-label :label} options] ; Changed opt-value to opt-val to avoid conflict
             [option {:value opt-val} opt-label]))]))

(defn ui-radio-group-item [{:keys [label value errors field-key data-path options onChange-fn]}]
  (let [full-path (conj data-path field-key)
        change-handler (or onChange-fn
                           #(rf/dispatch [::events/update-form-field full-path (.. % -target -value)]))]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     (into [radio-group {:value value
                         :onChange change-handler}]
           (for [{opt-val :value opt-label :label} options] ; Changed opt-value to opt-val
             [radio {:value opt-val} opt-label]))]))

(defn ui-boolean-radio-item [{:keys [label bool-value errors field-key data-path]}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [row {:gutter 16}
      [col {:span 24}
       [radio-group {:value (if bool-value "yes" "no")
                     :onChange #(rf/dispatch [::events/toggle-boolean-field full-path (= (.. % -target -value) "yes")])}
        [radio {:value "no"} "无"]
        [radio {:value "yes"} "有"]]]]]))

(defn ui-boolean-switch-item [{:keys [label bool-value errors field-key data-path]}]
  (let [full-path (conj data-path field-key)]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [row {:gutter 16}
      [col {:span 4}
       [switch {:checked bool-value
                :onChange #(rf/dispatch [::events/toggle-boolean-field full-path])}]]
      [col {:span 20}
       [:span (if bool-value "有" "无")]]]]))

(defn ui-date-picker-item [{:keys [label value placeholder errors field-key data-path style format showTime]
                            :or {style {:width "100%"}
                                 format "YYYY-MM-DD"}}]
  (let [full-path (conj data-path field-key)
        date-value (when value (try (dayjs value) (catch :default _ nil)))]
    [form-item {:label label
                :validateStatus (when (get-in errors full-path) "error")
                :help (get-in errors full-path)}
     [date-picker {:value date-value
                   :placeholder (or placeholder (str "请选择" (if (string? label) label "")))
                   :style style
                   :format format
                   :showTime showTime
                   :onChange #(rf/dispatch [::events/update-form-field
                                            full-path
                                            (when %1 (.format %1 format))])}]]))
;; --- 结束新的表单 UI 帮助组件 ---

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
        [ui-input-item {:label "门诊号" :value (:outpatient-number basic-info) :errors errors
                        :data-path [:basic-info] :field-key :outpatient-number}]]
       [col {:span 12}
        [ui-input-item {:label "姓名" :value (:name basic-info) :errors errors
                        :data-path [:basic-info] :field-key :name}]]]

      [row {:gutter 16}
       [col {:span 12}
        [ui-radio-group-item {:label "性别" :value (:gender basic-info) :errors errors
                              :data-path [:basic-info] :field-key :gender
                              :options [{:value "male" :label "男"}
                                        {:value "female" :label "女"}]}]]
       [col {:span 12}
        [ui-input-number-item {:label "年龄" :value (:age basic-info) :errors errors
                               :data-path [:basic-info] :field-key :age
                               :min 1 :max 120}]]]

      [row {:gutter 16}
       [col {:span 12}
        [ui-input-item {:label "病区" :value (:ward basic-info) :errors errors
                        :data-path [:basic-info] :field-key :ward
                        :placeholder "请输入病区信息"}]]
       [col {:span 12}
        [ui-input-item {:label "电子健康卡号" :value (:health-card-number basic-info) :errors errors
                        :data-path [:basic-info] :field-key :health-card-number
                        :placeholder "请输入电子健康卡号"}]]]

      [row {:gutter 16}
       [col {:span 24}
        [ui-text-area-item {:label "术前诊断" :value (:pre-op-diagnosis basic-info) :errors errors
                            :data-path [:basic-info] :field-key :pre-op-diagnosis
                            :rows 2}]]]

      [row {:gutter 16}
       [col {:span 24}
        [ui-text-area-item {:label "拟施手术" :value (:planned-surgery basic-info) :errors errors
                            :data-path [:basic-info] :field-key :planned-surgery
                            :rows 2}]]]

      [form-navigation current-step 4 submitting?]]])) ; max-step 修正为 4

;; 病史步骤
(defn medical-history-step []
  (let [medical-summary @(rf/subscribe [::subs/medical-summary]) ; Assuming this sub is correct for this step
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "病史信息"
     [form {:layout "vertical"}
      [ui-boolean-switch-item {:label "过敏史"
                               :bool-value (:allergy-history medical-summary)
                               :errors errors
                               :data-path [:medical-summary] ; Path matches the sub
                               :field-key :allergy-history}]

      (when (:allergy-history medical-summary)
        [:<>
         [ui-input-item {:label "过敏原"
                         :value (:allergens medical-summary)
                         :errors errors
                         :data-path [:medical-summary]
                         :field-key :allergens}]
         [ui-input-item {:label "最近发生过敏时间"
                         :value (:allergy-time medical-summary)
                         :errors errors
                         :data-path [:medical-summary]
                         :field-key :allergy-time}]])

      [divider]

      [ui-boolean-switch-item {:label "吸烟史"
                               :bool-value (:smoking-history medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :smoking-history}]

      (when (:smoking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [ui-input-number-item {:label "吸烟年数"
                                  :value (:smoking-years medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :smoking-years
                                  :min 0 :max 100}]]
          [col {:span 12}
           [ui-input-number-item {:label "每天吸烟支数"
                                  :value (:smoking-per-day medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :smoking-per-day
                                  :min 0 :max 100}]]]])
      [form-navigation current-step 4 submitting?]]])) ; max-step 修正为 4

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
        [ui-input-number-item {:label "身高 (cm)" :value (:height general-condition) :errors errors
                               :data-path [:general-condition] :field-key :height
                               :min 50 :max 250}]]
       [col {:span 12}
        [ui-input-number-item {:label "体重 (kg)" :value (:weight general-condition) :errors errors
                               :data-path [:general-condition] :field-key :weight
                               :min 1 :max 300}]]]

      [row {:gutter 16}
       [col {:span 12}
        [ui-select-item {:label "精神状态" :value (:mental-state general-condition) :errors errors
                         :data-path [:general-condition] :field-key :mental-state
                         :options [{:value "良好" :label "良好"}
                                   {:value "一般" :label "一般"}
                                   {:value "较差" :label "较差"}]}]]
       [col {:span 12}
        [ui-select-item {:label "活动能力" :value (:activity-ability general-condition) :errors errors
                         :data-path [:general-condition] :field-key :activity-ability
                         :options [{:value "正常" :label "正常"}
                                   {:value "轻度受限" :label "轻度受限"}
                                   {:value "中度受限" :label "中度受限"}
                                   {:value "重度受限" :label "重度受限"}
                                   {:value "卧床不起" :label "卧床不起"}]}]]]
      ;; 血压字段结构特殊，暂时保留原样或考虑更专门的组件
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
        [ui-input-number-item {:label "脉搏 (次/分)" :value (:pulse general-condition) :errors errors
                               :data-path [:general-condition] :field-key :pulse
                               :min 30 :max 200}]]
       [col {:span 8}
        [ui-input-number-item {:label "呼吸 (次/分)" :value (:respiration general-condition) :errors errors
                               :data-path [:general-condition] :field-key :respiration
                               :min 5 :max 60}]]
       [col {:span 8}
        [ui-input-number-item {:label "体温 (℃)" :value (:temperature general-condition) :errors errors
                               :data-path [:general-condition] :field-key :temperature
                               :step 0.1 :min 35 :max 42}]]]

      [ui-input-number-item {:label "血氧饱和度 (SpO2) %" :value (:spo2 general-condition) :errors errors
                             :data-path [:general-condition] :field-key :spo2
                             :min 70 :max 100}]

      [form-navigation current-step 4 submitting?]]])) ; max-step 修正为 4

;; 病情摘要步骤
(defn medical-summary-step []
  (let [medical-summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])
        current-step @(rf/subscribe [::subs/current-step])
        submitting? @(rf/subscribe [::subs/submitting?])]
    [step-container "病情摘要"
     [form {:layout "vertical"}
      [ui-boolean-radio-item {:label "过敏史"
                              :bool-value (:allergy-history medical-summary)
                              :errors errors
                              :data-path [:medical-summary]
                              :field-key :allergy-history}]

      (when (:allergy-history medical-summary)
        [:<>
         [ui-input-item {:label "过敏原"
                         :value (:allergens medical-summary)
                         :errors errors
                         :data-path [:medical-summary]
                         :field-key :allergens
                         :placeholder "请输入过敏原"}]
         [ui-date-picker-item {:label "过敏时间"
                               :value (:allergy-time medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :allergy-time
                               :placeholder "最近发生过敏时间"
                               :format "YYYY-MM-DD"}]])
      [divider]

      [ui-boolean-radio-item {:label "吸烟史"
                              :bool-value (:smoking-history medical-summary)
                              :errors errors
                              :data-path [:medical-summary]
                              :field-key :smoking-history}]

      (when (:smoking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [ui-input-number-item {:label "吸烟年数"
                                  :value (:smoking-years medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :smoking-years
                                  :min 0 :max 100
                                  :placeholder "请输入年数"}]]
          [col {:span 12}
           [ui-input-number-item {:label "每天吸烟支数"
                                  :value (:smoking-per-day medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :smoking-per-day
                                  :min 0 :max 100
                                  :placeholder "请输入支数"}]]]])
      [divider]

      [ui-boolean-radio-item {:label "饮酒史"
                              :bool-value (:drinking-history medical-summary)
                              :errors errors
                              :data-path [:medical-summary]
                              :field-key :drinking-history}]

      (when (:drinking-history medical-summary)
        [:<>
         [row {:gutter 16}
          [col {:span 12}
           [ui-input-number-item {:label "饮酒年数"
                                  :value (:drinking-years medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :drinking-years
                                  :min 0 :max 100
                                  :placeholder "请输入年数"}]]
          [col {:span 12}
           [ui-input-number-item {:label "每天饮酒量 (ml)"
                                  :value (:drinking-ml-per-day medical-summary)
                                  :errors errors
                                  :data-path [:medical-summary]
                                  :field-key :drinking-ml-per-day
                                  :min 0 :max 3000
                                  :placeholder "请输入饮酒量"}]]]])

      [form-navigation current-step 4 submitting?]]]))

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
      [ui-text-area-item {:label "呼吸系统疾病" :value (:respiratory-disease comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :respiratory-disease
                          :placeholder "请描述呼吸系统疾病情况"}]]
     [col {:span 12}
      [ui-text-area-item {:label "心血管疾病" :value (:cardiovascular-disease comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :cardiovascular-disease
                          :placeholder "请描述心血管疾病情况"}]]]

    [row {:gutter 16}
     [col {:span 12}
      [ui-text-area-item {:label "内分泌疾病" :value (:endocrine-disease comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :endocrine-disease
                          :placeholder "请描述内分泌疾病情况"}]]
     [col {:span 12}
      [ui-text-area-item {:label "神经精神疾病" :value (:neuropsychiatric-disease comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :neuropsychiatric-disease
                          :placeholder "请描述神经精神疾病情况"}]]]

    [row {:gutter 16}
     [col {:span 12}
      [ui-text-area-item {:label "关节骨骼系统" :value (:skeletal-system comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :skeletal-system
                          :placeholder "请描述关节骨骼系统情况"}]]
     [col {:span 12}
      [ui-text-area-item {:label "家族恶性高热史" :value (:family-malignant-hyperthermia comorbidities) :errors errors
                          :data-path [:comorbidities] :field-key :family-malignant-hyperthermia
                          :placeholder "请描述家族恶性高热史情况"}]]]

    [ui-text-area-item {:label "既往麻醉、手术史" :value (:past-anesthesia-surgery comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :past-anesthesia-surgery
                        :rows 3 :placeholder "请描述既往麻醉、手术史情况"}]

    [row {:gutter 16}
     [col {:span 12}
      [ui-text-area-item {:label "使用的特殊药物" :value (get-in comorbidities [:special-medications :used]) :errors errors
                          :data-path [:comorbidities :special-medications] :field-key :used
                          :placeholder "请描述使用的特殊药物情况"}]]
     [col {:span 12}
      [ui-date-picker-item {:label "最后一次用药时间" :value (get-in comorbidities [:special-medications :last-time]) :errors errors
                            :data-path [:comorbidities :special-medications] :field-key :last-time
                            :placeholder "最后一次用药时间" :format "YYYY-MM-DD HH:mm" :showTime {:format "HH:mm"}}]]]
    [divider]

    [ui-text-area-item {:label "心脏" :value (:heart comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :heart :placeholder "请描述心脏情况"}]
    [ui-text-area-item {:label "肺脏" :value (:lung comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :lung :placeholder "请描述肺脏情况"}]
    [ui-text-area-item {:label "气道" :value (:airway comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :airway :placeholder "请描述气道情况"}]
    [ui-text-area-item {:label "牙齿" :value (:teeth comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :teeth :placeholder "请描述牙齿情况"}]
    [ui-text-area-item {:label "脊柱四肢" :value (:spine-limbs comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :spine-limbs :placeholder "请描述脊柱四肢情况"}]
    [ui-text-area-item {:label "其它" :value (:others comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :others :placeholder "请描述其它情况"}]
    [ui-text-area-item {:label "神经" :value (:nerve comorbidities) :errors errors
                        :data-path [:comorbidities] :field-key :nerve :placeholder "请描述神经情况"}]

    [divider {:orientation "left"} "相关辅助检查检验结果"]

    [ui-text-area-item {:label "胸片" :value (get-in comorbidities [:auxiliary-exam :chest-radiograph]) :errors errors
                        :data-path [:comorbidities :auxiliary-exam] :field-key :chest-radiograph
                        :placeholder "请描述胸片检查结果"}]
    [ui-text-area-item {:label "肺功能" :value (get-in comorbidities [:auxiliary-exam :pulmonary-function]) :errors errors
                        :data-path [:comorbidities :auxiliary-exam] :field-key :pulmonary-function
                        :placeholder "请描述肺功能检查结果"}]
    [ui-text-area-item {:label "心脏彩超" :value (get-in comorbidities [:auxiliary-exam :cardiac-ultrasound]) :errors errors
                        :data-path [:comorbidities :auxiliary-exam] :field-key :cardiac-ultrasound
                        :placeholder "请描述心脏彩超检查结果"}]
    [ui-text-area-item {:label "心电图" :value (get-in comorbidities [:auxiliary-exam :ecg]) :errors errors
                        :data-path [:comorbidities :auxiliary-exam] :field-key :ecg
                        :placeholder "请描述心电图检查结果"}]
    [ui-text-area-item {:label "其他" :value (get-in comorbidities [:auxiliary-exam :other]) :errors errors
                        :data-path [:comorbidities :auxiliary-exam] :field-key :other
                        :placeholder "请描述其他检查结果"}]

    [form-navigation current-step 4 submitting?]]])) ; max-step 修正为 4

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
        [ui-select-item {:label "ASA分级" :value (:asa-grade anesthesia-plan) :errors errors
                         :data-path [:anesthesia-plan] :field-key :asa-grade
                         :options [{:value "I" :label "I级 - 健康人"}
                                   {:value "II" :label "II级 - 轻度全身性疾病"}
                                   {:value "III" :label "III级 - 严重全身性疾病"}
                                   {:value "IV" :label "IV级 - 威胁生命的全身性疾病"}
                                   {:value "V" :label "V级 - 濒死病人"}
                                   {:value "VI" :label "VI级 - 脑死亡病人"}]}]]
       [col {:span 12}
        [ui-select-item {:label "心功能分级(NYHA)" :value (:heart-function anesthesia-plan) :errors errors
                         :data-path [:anesthesia-plan] :field-key :heart-function
                         :options [{:value "I" :label "I级 - 无症状"}
                                   {:value "II" :label "II级 - 轻度症状"}
                                   {:value "III" :label "III级 - 明显症状"}
                                   {:value "IV" :label "IV级 - 症状严重"}]}]]]

      [ui-select-item {:label "拟行麻醉方式" :value (:anesthesia-method anesthesia-plan) :errors errors
                       :data-path [:anesthesia-plan] :field-key :anesthesia-method
                       :options [{:value "全身麻醉" :label "全身麻醉"}
                                 {:value "椎管内麻醉" :label "椎管内麻醉"}
                                 {:value "神经阻滞麻醉" :label "神经阻滞麻醉"}
                                 {:value "局部麻醉" :label "局部麻醉"}
                                 {:value "静脉麻醉" :label "静脉麻醉"}
                                 {:value "联合麻醉" :label "联合麻醉"}]}]

      [ui-text-area-item {:label "监测项目" :value (:monitoring-items anesthesia-plan) :errors errors
                          :data-path [:anesthesia-plan] :field-key :monitoring-items
                          :placeholder "请填写监测项目"}]
      [ui-text-area-item {:label "特殊技术" :value (:special-techniques anesthesia-plan) :errors errors
                          :data-path [:anesthesia-plan] :field-key :special-techniques
                          :placeholder "请填写特殊技术"}]

      [divider {:orientation "left"} "其他"]

      [form-item {:label "术前麻醉医嘱"} ; This is a group label, specific structure below
       [row {:gutter 16}
        [col {:span 12}
         [ui-input-number-item {:label "术前禁食" :value (:pre-op-fasting anesthesia-plan) :errors errors
                                :data-path [:anesthesia-plan] :field-key :pre-op-fasting
                                :min 0 :max 24 :addonAfter "小时"}]]
        [col {:span 12}
         [ui-input-number-item {:label "术前禁饮" :value (:pre-op-water-restriction anesthesia-plan) :errors errors
                                :data-path [:anesthesia-plan] :field-key :pre-op-water-restriction
                                :min 0 :max 24 :addonAfter "小时"}]]]]]

     [ui-text-area-item {:label "术日晨继续应用药物" :value (:continue-medication anesthesia-plan) :errors errors
                         :data-path [:anesthesia-plan] :field-key :continue-medication
                         :placeholder "需进一步检查"}]
     [ui-text-area-item {:label "术日晨停用药物" :value (:discontinue-medication anesthesia-plan) :errors errors
                         :data-path [:anesthesia-plan] :field-key :discontinue-medication
                         :placeholder "需进一步会诊"}]
     [ui-text-area-item {:label "麻醉中需注意的问题" :value (:anesthesia-notes anesthesia-plan) :errors errors
                         :data-path [:anesthesia-plan] :field-key :anesthesia-notes
                         :rows 3 :placeholder "请填写麻醉中需注意的问题"}]

     [row {:gutter 16}
      [col {:span 12}
       [ui-input-item {:label "麻醉医师签名" :value (:physician-signature anesthesia-plan) :errors errors
                       :data-path [:anesthesia-plan] :field-key :physician-signature
                       :placeholder "请输入麻醉医师姓名"}]]
      [col {:span 12}
       [ui-date-picker-item {:label "评估日期" :value (:assessment-date anesthesia-plan) :errors errors
                             :data-path [:anesthesia-plan] :field-key :assessment-date
                             :placeholder "请选择日期" :format "YYYY-MM-DD"}]]]
     [form-navigation current-step 4 submitting?]]))

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
