(ns hc.hospital.patient.views
  (:require [re-frame.core :as rf]
            [hc.hospital.patient.events :as events]
            [hc.hospital.patient.subs :as subs]
            [hc.hospital.utils :as utils]))

;; --- Progress Bar ---
(defn progress-bar-component [current-step total-steps]
  [:div {:class "progress-container"}
   [:div {:class "progress-bar"
          :style {:width (str (* (/ (inc current-step) total-steps) 100) "%")}}]
   [:div {:class "progress-text"}
    (str "进度 " (inc current-step) "/" total-steps)]])

;; --- Step Section Wrapper ---
(defn step-section-wrapper [title current-step step-index & children]
  [:div {:class "section"
         :id (str "section" (inc step-index))
         :style (if (= current-step step-index) {} {:display "none"})}
   [:h2 {:class "section-title"} title]
   (into [:<>] children)])

;; --- Form Navigation ---
;; Corrected version from previous interaction
(defn form-navigation [current-step max-step-idx submitting?]
  [:div {:class "navigation-buttons" :style {:marginTop "24px" :textAlign "right"}}
   (when (> current-step 0)
     [:button {:type "button"
               :class "btn-secondary"
               :style {:marginRight "10px"
                       :padding "10px 20px" :border "none" :borderRadius "4px" :cursor "pointer"
                       :backgroundColor "#f0f0f0" :color "#333"}
               :onClick #(rf/dispatch [::events/prev-step])}
      "上一步"])
   (if (< current-step max-step-idx)
     [:button {:type "button"
               :class "btn-primary"
               :style {:padding "10px 20px" :border "none" :borderRadius "4px" :cursor "pointer"
                       :backgroundColor "#1890ff" :color "white"}
               :onClick #(rf/dispatch [::events/next-step])}
      "下一步"]
     [:button {:type "button" ; Changed from submit to button, form onSubmit handles submission call
               :class "btn-primary"
               :style {:padding "10px 20px" :border "none" :borderRadius "4px" :cursor "pointer"
                       :backgroundColor "#1890ff" :color "white"}
               :disabled submitting?
               :onClick #(rf/dispatch [::events/validate-and-submit])}
      (if submitting? "提交中..." "提交")])])

;; --- Custom Divider ---
(defn custom-divider
  ([] [:hr {:class "section-divider"}])
  ([text]
   (if (empty? text)
     [custom-divider]
     [:h3 {:class "subsection-title"} text])))


;; --- New Form UI Helper Components ---
(defn ui-input-item [{:keys [label value placeholder errors field-key data-path type unit]
                      :or {type "text"}}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id}
      label
      (when unit [:span {:class "unit"} (str " " unit)])]
     [:input {:type type
              :id field-id
              :class "form-input"
              :value (if (nil? value) "" value)
              :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
              :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-text-area-item [{:keys [label value placeholder errors field-key data-path rows]
                          :or {rows 3}}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id} label]
     [:textarea {:id field-id
                 :class "form-input"
                 :value (if (nil? value) "" value)
                 :rows rows
                 :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                 :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-input-number-item [{:keys [label value placeholder errors field-key data-path min max step unit]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id}
      label
      (when unit [:span {:class "unit"} (str " " unit)])]
     [:input {:type "number"
              :id field-id
              :class "form-input"
              :value (if (nil? value) "" value)
              :min min
              :max max
              :step step
              :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
              :onChange #(let [raw-value (-> % .-target .-value)
                               parsed-value (if (not-empty raw-value)
                                              (let [num (js/parseFloat raw-value)]
                                                (if (js/isNaN num) nil num)) ; Return nil if parsing fails
                                              nil)]  ; Treat empty string as nil for numbers
                           (rf/dispatch [::events/update-form-field full-path parsed-value]))}]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-select-item [{:keys [label value placeholder errors field-key data-path options]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id} label]
     [:select {:id field-id
               :class "form-input"
               :value (if (nil? value) "" value)
               :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}
      [:option {:value ""} (or placeholder (str "请选择" (if (string? label) label "")))]
      (for [{opt-val :value opt-label :label} options]
        ^{:key (str opt-val opt-label)}
        [:option {:value opt-val} opt-label])]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-radio-group-item [{:keys [label value errors field-key data-path options]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label"} label]
     [:div {:class "radio-group"}
      (for [{opt-val :value opt-label :label} options]
        ^{:key (str opt-val opt-label)}
        [:label {:class "radio-label"}
         [:input {:type "radio"
                  :name group-name
                  :value opt-val
                  :checked (= value opt-val)
                  :onChange #(rf/dispatch [::events/update-form-field full-path opt-val])}]
         (str " " opt-label)])]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-boolean-radio-item [{:keys [label bool-value errors field-key data-path]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label"} label]
     [:div {:class "radio-group"}
      [:label {:class "radio-label"}
       [:input {:type "radio"
                :name group-name
                :value "yes"
                :checked (true? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path true])}]
       " 有"]
      [:label {:class "radio-label"}
       [:input {:type "radio"
                :name group-name
                :value "no"
                :checked (false? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path false])}]
       " 无"]]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))

(defn ui-date-picker-item [{:keys [label value placeholder errors field-key data-path showTime]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))
        input-type (if showTime "datetime-local" "date")
        current-value (utils/format-date value (if (= input-type "datetime-local") "YYYY-MM-DDTHH:mm" "YYYY-MM-DD"))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id} label]
     [:input {:type input-type
              :id field-id
              :class "form-input"
              :value current-value
              :placeholder (or placeholder (str "请选择" (if (string? label) label "")))
              :onChange #(let [date-string (-> % .-target .-value)
                               final-value (if (empty? date-string) nil date-string)]
                           (rf/dispatch [::events/update-form-field full-path final-value]))}]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))


(defn ui-file-input-item [{:keys [label value errors field-key data-path]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))
        display-value (if (and value (not= value "-") (not (empty? value)))
                        (str "已选择: " value)
                        "选择文件")]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label {:class "form-label" :for field-id} label]
     [:label {:class "file-upload-label" :for field-id} ; Make sure 'for' matches input id
      [:i {:class "fas fa-upload mr-2"}]
      display-value]
     [:input {:type "file"
              :id field-id
              :class "file-upload" ; This input will be visually hidden by CSS
              :style {:display "none"} ; Explicitly hide if CSS isn't loaded/working
              :onChange #(let [file-name (if (-> % .-target .-files .-length (> 0))
                                            (-> % .-target .-files (.item 0) .-name)
                                            nil)]
                           (rf/dispatch [::events/update-form-field full-path (or file-name "-")]))}]
     (when error-msg
       [:div {:class "error-message"} error-msg])]))


;; --- Form Steps ---

;; 基本信息步骤
(defn basic-info-step []
  (let [basic-info @(rf/subscribe [::subs/basic-info])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-input-item {:label "门诊号" :value (:outpatient-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :outpatient-number
                     :placeholder "请输入门诊号"}]
     [ui-input-item {:label "姓名" :value (:name basic-info) :errors errors
                     :data-path [:basic-info] :field-key :name
                     :placeholder "请输入姓名"}]
     [ui-radio-group-item {:label "性别" :value (:gender basic-info) :errors errors
                           :data-path [:basic-info] :field-key :gender
                           :options [{:value "male" :label "男"}
                                     {:value "female" :label "女"}
                                     {:value "other" :label "其他"}]}]
     [ui-input-number-item {:label "年龄" :value (:age basic-info) :errors errors
                            :data-path [:basic-info] :field-key :age
                            :min 0 :max 150 :unit "岁"
                            :placeholder "请输入年龄"}]
     [ui-input-item {:label "床号" :value (:bed-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :bed-number
                     :placeholder "请输入床号"}]
     [ui-input-item {:label "住院号" :value (:hospitalization-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :hospitalization-number
                     :placeholder "请输入住院号"}]
     [ui-input-item {:label "联系电话" :value (:contact-phone basic-info) :errors errors
                     :data-path [:basic-info] :field-key :contact-phone :type "tel"
                     :placeholder "请输入联系电话"}]
     [ui-input-item {:label "身份证号" :value (:id-card-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :id-card-number
                     :placeholder "请输入身份证号"}]
     [ui-input-item {:label "病区" :value (:ward basic-info) :errors errors
                     :data-path [:basic-info] :field-key :ward
                     :placeholder "请输入病区信息"}]
     [ui-input-item {:label "电子健康卡号" :value (:health-card-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :health-card-number
                     :placeholder "请输入电子健康卡号"}]
     [ui-text-area-item {:label "术前诊断" :value (:pre-op-diagnosis basic-info) :errors errors
                         :data-path [:basic-info] :field-key :pre-op-diagnosis
                         :rows 2 :placeholder "请输入术前诊断"}]
     [ui-text-area-item {:label "拟施手术" :value (:planned-surgery basic-info) :errors errors
                         :data-path [:basic-info] :field-key :planned-surgery
                         :rows 2 :placeholder "请输入拟施手术"}]]))

;; 一般情况步骤
(defn general-condition-step []
  (let [general-condition @(rf/subscribe [::subs/general-condition])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-input-number-item {:label "身高" :value (:height general-condition) :errors errors
                            :data-path [:general-condition] :field-key :height
                            :min 50 :max 250 :unit "cm"
                            :placeholder "请输入身高"}]
     [ui-input-number-item {:label "体重" :value (:weight general-condition) :errors errors
                            :data-path [:general-condition] :field-key :weight
                            :min 1 :max 300 :unit "kg"
                            :placeholder "请输入体重"}]
     (let [bp-path [:general-condition :blood-pressure]
           systolic-val (get-in general-condition [:blood-pressure :systolic])
           diastolic-val (get-in general-condition [:blood-pressure :diastolic])
           systolic-error (get-in errors (conj bp-path :systolic))
           diastolic-error (get-in errors (conj bp-path :diastolic))
           bp-error-msg (or systolic-error diastolic-error)]
       [:div {:class (if bp-error-msg "form-group error" "form-group")}
        [:label {:class "form-label"} "血压 " [:span {:class "unit"} "mmHg"]]
        [:div {:style {:display "flex" :alignItems "center"}}
         [:input {:type "number" :class "form-input" :style {:flex "1" :marginRight "5px"}
                  :value (if (nil? systolic-val) "" systolic-val)
                  :placeholder "收缩压"
                  :onChange #(let [val (-> % .-target .-value)] (rf/dispatch [::events/update-form-field (conj bp-path :systolic) (if (empty? val) nil (js/parseFloat val))]))}]
         [:span {:style {:padding "0 5px"}} "/"]
         [:input {:type "number" :class "form-input" :style {:flex "1" :marginLeft "5px"}
                  :value (if (nil? diastolic-val) "" diastolic-val)
                  :placeholder "舒张压"
                  :onChange #(let [val (-> % .-target .-value)] (rf/dispatch [::events/update-form-field (conj bp-path :diastolic) (if (empty? val) nil (js/parseFloat val))]))}]]
        (when bp-error-msg [:div {:class "error-message"} bp-error-msg])])

     [ui-input-number-item {:label "脉搏" :value (:pulse general-condition) :errors errors
                            :data-path [:general-condition] :field-key :pulse
                            :min 30 :max 200 :unit "次/分"
                            :placeholder "请输入脉搏"}]
     [ui-input-number-item {:label "呼吸" :value (:respiration general-condition) :errors errors
                            :data-path [:general-condition] :field-key :respiration
                            :min 5 :max 60 :unit "次/分"
                            :placeholder "请输入呼吸频率"}]
     [ui-input-number-item {:label "体温" :value (:temperature general-condition) :errors errors
                            :data-path [:general-condition] :field-key :temperature
                            :step 0.1 :min 35 :max 42 :unit "℃"
                            :placeholder "请输入体温"}]
     [ui-input-number-item {:label "血氧饱和度 (SpO2)" :value (:spo2 general-condition) :errors errors
                              :data-path [:general-condition] :field-key :spo2
                              :min 70 :max 100 :unit "%"
                              :placeholder "请输入血氧饱和度"}]
     [ui-select-item {:label "精神状态" :value (:mental-state general-condition) :errors errors
                      :data-path [:general-condition] :field-key :mental-state
                      :options [{:value "良好" :label "良好"}
                                {:value "一般" :label "一般"}
                                {:value "较差" :label "较差"}]}]
     [ui-select-item {:label "活动能力" :value (:activity-ability general-condition) :errors errors
                      :data-path [:general-condition] :field-key :activity-ability
                      :options [{:value "正常" :label "正常"}
                                {:value "轻度受限" :label "轻度受限"}
                                {:value "中度受限" :label "中度受限"}
                                {:value "重度受限" :label "重度受限"}
                                {:value "卧床不起" :label "卧床不起"}]}]
     ]))


;; 病情摘要步骤 (was medical_summary_step, mapping to db.cljs :medical-summary)
(defn medical-summary-step []
  (let [medical-summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
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
        [ui-date-picker-item {:label "过敏时间" ; Assuming this is a date, not datetime
                              :value (:allergy-time medical-summary)
                              :errors errors
                              :data-path [:medical-summary]
                              :field-key :allergy-time
                              :placeholder "最近发生过敏时间"}]])
     [custom-divider]
     [ui-boolean-radio-item {:label "吸烟史"
                             :bool-value (:smoking-history medical-summary)
                             :errors errors
                             :data-path [:medical-summary]
                             :field-key :smoking-history}]
     (when (:smoking-history medical-summary)
       [:<>
        [ui-input-number-item {:label "吸烟年数"
                               :value (:smoking-years medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :smoking-years
                               :min 0 :max 100 :unit "年"
                               :placeholder "请输入年数"}]
        [ui-input-number-item {:label "每天吸烟支数"
                               :value (:smoking-per-day medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :smoking-per-day
                               :min 0 :max 100 :unit "支"
                               :placeholder "请输入支数"}]])
     [custom-divider]
     [ui-boolean-radio-item {:label "饮酒史"
                             :bool-value (:drinking-history medical-summary)
                             :errors errors
                             :data-path [:medical-summary]
                             :field-key :drinking-history}]
     (when (:drinking-history medical-summary)
       [:<>
        [ui-input-number-item {:label "饮酒年数"
                               :value (:drinking-years medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :drinking-years
                               :min 0 :max 100 :unit "年"
                               :placeholder "请输入年数"}]
        [ui-input-number-item {:label "每天饮酒量"
                               :value (:drinking-ml-per-day medical-summary)
                               :errors errors
                               :data-path [:medical-summary]
                               :field-key :drinking-ml-per-day
                               :min 0 :max 3000 :unit "ml"
                               :placeholder "请输入饮酒量"}]])]))

;; 并存疾病及检查步骤 (was comorbidities_step)
(defn comorbidities-step []
  (let [comorbidities @(rf/subscribe [::subs/comorbidities])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [custom-divider "主要疾病史"]
     [ui-text-area-item {:label "呼吸系统疾病" :value (:respiratory-disease comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :respiratory-disease
                         :placeholder "请描述呼吸系统疾病情况"}]
     [ui-text-area-item {:label "心血管疾病" :value (:cardiovascular-disease comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :cardiovascular-disease
                         :placeholder "请描述心血管疾病情况"}]
     [ui-text-area-item {:label "内分泌疾病" :value (:endocrine-disease comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :endocrine-disease
                         :placeholder "请描述内分泌疾病情况"}]
     [ui-text-area-item {:label "神经精神疾病" :value (:neuropsychiatric-disease comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :neuropsychiatric-disease
                         :placeholder "请描述神经精神疾病情况"}]
     [ui-text-area-item {:label "关节骨骼系统" :value (:skeletal-system comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :skeletal-system
                         :placeholder "请描述关节骨骼系统情况"}]
     [ui-text-area-item {:label "家族恶性高热史" :value (:family-malignant-hyperthermia comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :family-malignant-hyperthermia
                         :placeholder "请描述家族恶性高热史情况"}]
     [ui-text-area-item {:label "既往麻醉、手术史" :value (:past-anesthesia-surgery comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :past-anesthesia-surgery
                         :rows 3 :placeholder "请描述既往麻醉、手术史情况"}]

     [custom-divider "特殊药物使用情况"]
     [ui-text-area-item {:label "使用的特殊药物" :value (get-in comorbidities [:special-medications :used]) :errors errors
                         :data-path [:comorbidities :special-medications] :field-key :used
                         :placeholder "请描述使用的特殊药物情况"}]
     [ui-date-picker-item {:label "最后一次用药时间" :value (get-in comorbidities [:special-medications :last-time]) :errors errors
                           :data-path [:comorbidities :special-medications] :field-key :last-time
                           :placeholder "选择最后一次用药时间" :showTime true}] ; showTime for datetime-local

     [custom-divider "体格检查"]
     [ui-text-area-item {:label "心脏" :value (:heart comorbidities) :errors errors ; Assuming these are direct fields in comorbidities now
                         :data-path [:comorbidities] :field-key :heart :placeholder "请描述心脏检查情况"}]
     [ui-text-area-item {:label "肺脏" :value (:lungs comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :lungs :placeholder "请描述肺脏检查情况"}]
     [ui-text-area-item {:label "气道" :value (:airway comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :airway :placeholder "请描述气道检查情况"}]
     [ui-text-area-item {:label "牙齿" :value (:teeth comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :teeth :placeholder "请描述牙齿检查情况"}]
     [ui-text-area-item {:label "脊柱四肢" :value (:spine-limbs comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :spine-limbs :placeholder "请描述脊柱四肢情况"}]
     [ui-text-area-item {:label "神经" :value (:nervous comorbidities) :errors errors
                         :data-path [:comorbidities] :field-key :nervous :placeholder "请描述神经检查情况"}]
     [ui-text-area-item {:label "其他体格检查" :value (:other comorbidities) :errors errors ; Renamed from :others to :other to match db
                         :data-path [:comorbidities] :field-key :other :placeholder "请描述其他体格检查情况"}]


     [custom-divider "相关辅助检查检验结果"]
     [ui-file-input-item {:label "胸片" :value (get-in comorbidities [:auxiliary-examination :chest-radiography]) :errors errors
                          :data-path [:comorbidities :auxiliary-examination] :field-key :chest-radiography}]
     [ui-file-input-item {:label "肺功能" :value (get-in comorbidities [:auxiliary-examination :pulmonary-function]) :errors errors
                          :data-path [:comorbidities :auxiliary-examination] :field-key :pulmonary-function}]
     [ui-file-input-item {:label "心脏彩超" :value (get-in comorbidities [:auxiliary-examination :cardiac-ultrasound]) :errors errors
                          :data-path [:comorbidities :auxiliary-examination] :field-key :cardiac-ultrasound}]
     [ui-file-input-item {:label "心电图" :value (get-in comorbidities [:auxiliary-examination :ecg]) :errors errors
                          :data-path [:comorbidities :auxiliary-examination] :field-key :ecg}]
     [ui-file-input-item {:label "其他检查" :value (get-in comorbidities [:auxiliary-examination :other]) :errors errors
                          :data-path [:comorbidities :auxiliary-examination] :field-key :other}]
     ]))

;; 麻醉计划步骤
(defn anesthesia-plan-step []
  (let [anesthesia-plan @(rf/subscribe [::subs/anesthesia-plan])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [custom-divider "评估与分级"]
     [ui-select-item {:label "ASA分级" :value (:asa-classification anesthesia-plan) :errors errors
                      :data-path [:assessment] :field-key :asa-classification ; Path corrected based on db.cljs
                      :options [{:value "I" :label "I级 - 健康人"}
                                {:value "II" :label "II级 - 轻度全身性疾病"}
                                {:value "III" :label "III级 - 严重全身性疾病"}
                                {:value "IV" :label "IV级 - 威胁生命的全身性疾病"}
                                {:value "V" :label "V级 - 濒死病人"}
                                {:value "VI" :label "VI级 - 脑死亡病人"}]}]
     [ui-select-item {:label "心功能分级(NYHA)" :value (:nyha-classification anesthesia-plan) :errors errors
                      :data-path [:assessment] :field-key :nyha-classification ; Path corrected
                      :options [{:value "I" :label "I级 - 无症状"}
                                {:value "II" :label "II级 - 轻度症状"}
                                {:value "III" :label "III级 - 明显症状"}
                                {:value "IV" :label "IV级 - 症状严重"}]}]

     [custom-divider "麻醉与监测"]
     [ui-select-item {:label "拟行麻醉方式" :value (:planned-anesthesia anesthesia-plan) :errors errors
                      :data-path [:assessment] :field-key :planned-anesthesia ; Path corrected
                      :options [{:value "全身麻醉" :label "全身麻醉"}
                                {:value "椎管内麻醉" :label "椎管内麻醉"}
                                {:value "神经阻滞麻醉" :label "神经阻滞麻醉"}
                                {:value "局部麻醉" :label "局部麻醉"}
                                {:value "静脉麻醉" :label "静脉麻醉"}
                                {:value "联合麻醉" :label "联合麻醉"}
                                {:value "其他" :label "其他"}]}]
     [ui-text-area-item {:label "监测项目" :value (:monitoring-items anesthesia-plan) :errors errors
                         :data-path [:assessment] :field-key :monitoring-items ; Path corrected
                         :placeholder "请填写监测项目"}]
     [ui-text-area-item {:label "特殊技术" :value (:special-techniques anesthesia-plan) :errors errors
                         :data-path [:assessment] :field-key :special-techniques ; Path corrected
                         :placeholder "请填写特殊技术"}]

     [custom-divider "术前医嘱与备注"]
     [ui-text-area-item {:label "术前麻醉医嘱" :value (get-in anesthesia-plan [:other-info :pre-op-fasting]) :errors errors
                         :data-path [:other-info] :field-key :pre-op-fasting
                         :placeholder "例如：术前禁食N小时，禁饮M小时"}]
     [ui-text-area-item {:label "术日晨继续应用药物" :value (get-in anesthesia-plan [:other-info :continue-medication]) :errors errors
                         :data-path [:other-info] :field-key :continue-medication
                         :placeholder "请填写术日晨需继续应用的药物"}]
     [ui-text-area-item {:label "术日晨停用药物" :value (get-in anesthesia-plan [:other-info :stop-medication]) :errors errors
                         :data-path [:other-info] :field-key :stop-medication
                         :placeholder "请填写术日晨需停用的药物"}]
     [ui-text-area-item {:label "麻醉中需注意的问题" :value (get-in anesthesia-plan [:other-info :anesthesia-notes]) :errors errors
                         :data-path [:other-info] :field-key :anesthesia-notes
                         :rows 3 :placeholder "请填写麻醉中需注意的问题"}]

     [custom-divider "签名与日期"]
     [ui-input-item {:label "麻醉医师签名" :value (get-in anesthesia-plan [:other-info :anesthesiologist-signature]) :errors errors
                     :data-path [:other-info] :field-key :anesthesiologist-signature
                     :placeholder "请输入麻醉医师姓名"}]
     [ui-date-picker-item {:label "评估日期" :value (get-in anesthesia-plan [:other-info :assessment-date]) :errors errors
                           :data-path [:other-info] :field-key :assessment-date
                           :placeholder "请选择日期"}]]))


;; --- Success Page ---
(defn success-page []
  [:div {:class "success-container" :style {:textAlign "center" :padding "40px"}}
   [:h1 {:class "success-title" :style {:fontSize "24px" :color "#4CAF50" :marginBottom "16px"}} "提交成功！"]
   [:p {:class "success-subtitle" :style {:fontSize "16px" :color "#555" :marginBottom "24px"}}
    "您的术前评估信息已成功提交，医生将会尽快审核。"]
   [:button {:type "button"
             :class "btn-primary"
             :style {:padding "12px 24px" :border "none" :borderRadius "4px" :cursor "pointer"
                     :backgroundColor "#1890ff" :color "white"}
             :onClick #(.reload js/window.location)}
    "返回首页"]])

;; --- Main Patient Form Component ---
(defn patient-form []
  (let [current-step @(rf/subscribe [::subs/current-step])
        submit-success? @(rf/subscribe [::subs/submit-success?])
        submitting? @(rf/subscribe [::subs/submitting?])
        total-steps 5 ; Basic Info, General Condition, Medical Summary, Comorbidities, Anesthesia Plan
        max-step-idx (dec total-steps)
        step-titles ["基本信息" "一般情况" "病情摘要" "并存疾病及检查" "麻醉计划"]]

    (if submit-success?
      [success-page]
      [:div {:class "form-container" :style {:maxWidth "800px" :margin "20px auto" :padding "20px" :boxShadow "0 0 10px rgba(0,0,0,0.1)" :borderRadius "8px"}}
       [progress-bar-component current-step total-steps]
       [:form {:id "patientAssessmentForm"
               :onSubmit (fn [event]
                           (.preventDefault event)
                           (rf/dispatch [::events/validate-and-submit]))}

        [step-section-wrapper (nth step-titles current-step) current-step current-step
         (case current-step
           0 [basic-info-step]
           1 [general-condition-step]
           2 [medical-summary-step]
           3 [comorbidities-step]
           4 [anesthesia-plan-step]
           [basic-info-step])] ; Default case

        [form-navigation current-step max-step-idx submitting?]]])))
