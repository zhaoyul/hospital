(ns hc.hospital.patient.views
  (:require
   [hc.hospital.patient.events :as events]
   [hc.hospital.patient.subs :as subs]
   [hc.hospital.utils :as utils]
   [re-frame.core :as rf]))

;; --- Progress Bar ---
(defn progress-bar-component [current-step total-steps step-titles]
  [:div.progress-bar
   [:div.progress-steps
    (for [i (range total-steps)]
      [:div {:class (str "progress-step" (if (= i current-step) " active" ""))
             :key (str "step-" i)
             :id (str "step" (inc i))
             :data-title (nth step-titles i nil)}
       (inc i)])]
   [:div#progress.progress-bar-fill
    {:style {:width (str (* (/ (inc current-step) total-steps) 100) "%")}}]])

;; --- Step Section Wrapper ---
(defn step-section-wrapper [title current-step step-index & children]
  [:div.section {:id (str "section" (inc step-index))
                 :style (if (= current-step step-index) {} {:display "none"})}
   [:h2.section-title title]
   (into [:<>] children)])

;; --- Form Navigation ---
(defn form-navigation [current-step total-steps submitting?] ; total-steps is count
  (let [max-step-idx (dec total-steps)]
    [:div.nav-buttons
     (when (pos? current-step)
       [:button#prevBtn.btn-secondary
        {:type "button"
         :onClick #(rf/dispatch [::events/prev-step])}
        "上一步"])
     [:button#nextBtn.btn-primary
      {:type "button"
       :style {:width (if (= current-step 0) "100%" "48%")}
       :disabled submitting?
       :onClick (if (< current-step max-step-idx)
                  #(rf/dispatch [::events/next-step])
                  #(rf/dispatch [::events/validate-and-submit]))}
      (if (< current-step max-step-idx)
        "下一步"
        (if submitting? "提交中..." "提交"))]]))

;; --- UI Components ---
(defn ui-input-item [{:keys [label value placeholder errors field-key data-path type unit data-index extra]
                      :or {type "text"}}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index}
      label
      (when unit [:span.unit (str " " unit)])]
     [:div {:style {:display "flex" :gap "8px"}}
      [:input.form-input {:type type
                          :id field-id
                          :value (if (nil? value) "" value)
                          :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                          :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}]
      (when extra extra)]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-input-number-item [{:keys [label value placeholder errors field-key data-path min max step unit data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index}
      label
      (when unit [:span.unit (str " " unit)])]
     [:input.form-input
      {:type "number"
       :id field-id
       :value (if (nil? value) "" value)
       :min min
       :max max
       :step step
       :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
       :onChange #(let [raw-value (-> % .-target .-value)
                        parsed-value (when (not-empty raw-value)
                                       (let [num (js/parseFloat raw-value)]
                                         (if (js/isNaN num) nil num)))]
                    (rf/dispatch [::events/update-form-field full-path parsed-value]))}]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-radio-group-item [{:keys [label value errors field-key data-path options data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label]
     [:div.radio-group
      (for [{opt-val :value opt-label :label} options]
        ^{:key (str opt-val opt-label)}
        [:label.radio-label
         [:input {:type "radio"
                  :name group-name
                  :value opt-val
                  :checked (= value opt-val)
                  :onChange #(rf/dispatch [::events/update-form-field full-path opt-val])}]
         [:span {} (str " " opt-label)]])]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-boolean-radio-item [{:keys [label bool-value errors field-key data-path data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        group-name (str (name field-key) "-" (hash data-path) "-bool")] ; Ensure unique name for boolean radio
    [:div {:class (if error-msg "form-group error" "form-group")} ; Kept form-group for consistency, can be reviewed
     [:label.form-label {:data-index data-index} label]
     [:div.radio-group
      [:label.radio-label
       [:input {:type "radio"
                :name group-name
                :value "yes" ; Standardized to "yes" for true
                :checked (true? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path true])}]
       [:span " 有"]]
      [:label.radio-label
       [:input {:type "radio"
                :name group-name
                :value "no" ; Standardized to "no" for false
                :checked (false? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path false])}]
       [:span " 无"]]]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-date-picker-item [{:keys [label value placeholder errors field-key data-path showTime data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))
        input-type (if showTime "datetime-local" "date")
        current-value (utils/format-date value (if (= input-type "datetime-local") "YYYY-MM-DDTHH:mm" "YYYY-MM-DD"))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index} label]
     [:input.form-input {:type input-type
                         :id field-id
                         :value current-value
                         :placeholder (or placeholder (str "请选择" (if (string? label) label "")))
                         :onChange #(let [date-string (-> % .-target .-value)
                                          final-value (if (empty? date-string) nil date-string)]
                                      (rf/dispatch [::events/update-form-field full-path final-value]))}]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-file-input-item [{:keys [label value errors field-key data-path data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))
        display-value (if (and value (not= value "-") (seq value))
                        (str "已选择: " value)
                        "选择文件")]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label]
     [:label.file-upload-label {:for field-id}
      [:i.fas.fa-upload.mr-2]
      display-value]
     [:input.file-upload
      {:type "file"
       :id field-id
       :style {:display "none"}
       :onChange #(let [file-name (if (-> % .-target .-files .-length (> 0))
                                    (-> % .-target .-files (.item 0) .-name)
                                    nil)]
                    (rf/dispatch [::events/update-form-field full-path (or file-name "-")]))}]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-condition-item [{:keys [label base-value detail-value errors field-key data-path data-index placeholder]}]
  (let [base-path (conj data-path field-key)
        detail-path (conj data-path (keyword (str (name field-key) "-detail")))
        error-msg (or (get-in errors base-path) (get-in errors detail-path))
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label]
     [:div.radio-group
      [:label.radio-label
       [:input {:type "radio" :name group-name :value "normal" :checked (= base-value "normal")
                :onChange #(rf/dispatch [::events/update-form-field base-path "normal"])}]
       [:span " 正常"]]
      [:label.radio-label
       [:input {:type "radio" :name group-name :value "abnormal" :checked (= base-value "abnormal")
                :onChange #(rf/dispatch [::events/update-form-field base-path "abnormal"])}]
       [:span " 异常"]]]
     (when (= base-value "abnormal")
       [:input.form-input.mt-2 {:type "text"
                                :value (if (nil? detail-value) "" detail-value)
                                :placeholder (or placeholder "如有异常，请说明")
                                :onChange #(rf/dispatch [::events/update-form-field detail-path (-> % .-target .-value)])}])
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-select-item [{:keys [label value placeholder errors field-key data-path options data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index} label]
     [:select.form-input {:id field-id
                          :value (or value "")
                          :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}
      [:option {:value "" :disabled true} (or placeholder "请选择")]
      (for [{opt-val :value opt-label :label} options] ; Changed :keys to :keys [label value] for clarity
        ^{:key opt-val} ; Assuming opt-val is unique for key
        [:option {:value opt-val} opt-label])]
     (when error-msg
       [:div.error-message error-msg])]))

;; --- Refactored Helper Components ---
(defn ui-conditional-group
  "A group with a boolean radio question, and conditional children if the answer is true.
  Wraps content in :div.form-group-container."
  [{:keys [label bool-value errors data-path field-key data-index]} & children-hiccup]
  [:div.form-group-container
   [ui-boolean-radio-item {:label label
                           :bool-value bool-value
                           :errors errors
                           :data-path data-path
                           :field-key field-key
                           :data-index data-index}]
   (when bool-value
     (if (and children-hiccup (= 1 (count children-hiccup)))
       (first children-hiccup)
       (when (seq children-hiccup)
         (into [:<>] children-hiccup))))])

(defn ui-comorbidity-item
  "An item for a comorbidity, typically a boolean 'has' and 'details' text input.
  Wraps content in :div.form-group-container."
  [{:keys [label has-value details-value errors base-path has-field-key details-field-key data-index details-placeholder]
    :or {has-field-key :has details-field-key :details details-placeholder "请填写详情"}}]
  [:div.form-group-container
   [ui-boolean-radio-item {:label label
                           :bool-value has-value
                           :errors errors
                           :data-path base-path
                           :field-key has-field-key
                           :data-index data-index}]
   (when has-value
     [ui-input-item {:label nil ; Details field typically doesn't have its own visible label here
                     :value details-value
                     :errors errors
                     :data-path base-path
                     :field-key details-field-key
                     :placeholder details-placeholder}])])


;; --- Form Steps ---
(defn basic-info-step []
  (let [basic-info @(rf/subscribe [::subs/basic-info])
        errors @(rf/subscribe [::subs/form-errors])
        outpatient-number-path [:basic-info :outpatient-number]]
    [:<>
     [ui-input-item {:label "门诊号" :value (:outpatient-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :outpatient-number
                     :placeholder "请输入门诊号" :data-index "1.1"
                     :extra [:button.btn-scan
                             {:onClick (fn []
                                         (set! (.-onScanSuccessCallback js/window)
                                               (fn [scanned-value]
                                                 (rf/dispatch [::events/update-form-field outpatient-number-path scanned-value])
                                                 (set! (.-onScanSuccessCallback js/window) nil)))
                                         (js/startScan))}
                             [:i {:class "fas fa-qrcode"}]]}]
     [ui-input-item {:label "姓名" :value (:name basic-info) :errors errors
                     :data-path [:basic-info] :field-key :name
                     :placeholder "请输入姓名" :data-index "1.2"}]
     [ui-input-item {:label "身份证号" :value (:id-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :id-number
                     :placeholder "请输入身份证号" :data-index "1.3"}]
     [ui-input-item {:label "手机号" :value (:phone basic-info) :errors errors
                     :data-path [:basic-info] :field-key :phone
                     :placeholder "请输入手机号" :data-index "1.4"}]
     [ui-radio-group-item {:label "性别" :value (:gender basic-info) :errors errors
                           :data-path [:basic-info] :field-key :gender
                           :options [{:label "男" :value "male"} {:label "女" :value "female"}]
                           :data-index "1.5"}]
     [ui-input-number-item {:label "年龄" :value (:age basic-info) :errors errors
                            :data-path [:basic-info] :field-key :age
                            :placeholder "请输入年龄" :data-index "1.6"}]
     [ui-select-item {:label "院区" :value (:hospital-district basic-info) :errors errors
                      :data-path [:basic-info] :field-key :hospital-district
                      :options [{:label "总院" :value "main"} {:label "积水潭院区" :value "jst"}]
                      :placeholder "请选择院区" :data-index "1.7"}]]))

(defn medical-summary-step "病情摘要步骤" []
  (let [summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-conditional-group {:label "过敏史" :bool-value (:allergy-history summary) :errors errors
                            :data-path [:medical-summary] :field-key :allergy-history :data-index "2.1"}
      [ui-input-item {:label "过敏源" :value (:allergen summary) :errors errors
                      :data-path [:medical-summary] :field-key :allergen
                      :placeholder "请输入过敏源"}] ; data-index for sub-items if needed, or rely on parent
      [ui-date-picker-item {:label "过敏时间" :value (:allergy-date summary) :errors errors
                            :data-path [:medical-summary] :field-key :allergy-date
                            :placeholder "请选择过敏时间"}]]

     [ui-conditional-group {:label "吸烟史" :bool-value (:smoking-history summary) :errors errors
                            :data-path [:medical-summary] :field-key :smoking-history :data-index "2.2"}
      [ui-input-number-item {:label "吸烟年数" :value (:smoking-years summary) :errors errors
                             :data-path [:medical-summary] :field-key :smoking-years
                             :placeholder "请输入吸烟年数"}]
      [ui-input-number-item {:label "每天吸烟支数" :value (:cigarettes-per-day summary) :errors errors
                             :data-path [:medical-summary] :field-key :cigarettes-per-day
                             :placeholder "请输入每天吸烟支数"}]]

     [ui-conditional-group {:label "饮酒史" :bool-value (:drinking-history summary) :errors errors
                            :data-path [:medical-summary] :field-key :drinking-history :data-index "2.3"}
      [ui-input-number-item {:label "饮酒年数" :value (:drinking-years summary) :errors errors
                             :data-path [:medical-summary] :field-key :drinking-years
                             :placeholder "请输入饮酒年数"}]
      [ui-input-item {:label "每天饮酒量" :value (:alcohol-per-day summary) :errors errors
                      :data-path [:medical-summary] :field-key :alcohol-per-day
                      :placeholder "请输入每天饮酒量(如 白酒2两 或 啤酒1瓶)"}]]]))

(defn coexisting-diseases-step []
  (let [comorbidities-data @(rf/subscribe [::subs/comorbidities])
        aux-exam-data @(rf/subscribe [::subs/auxiliary-examination])
        physical-exam-data @(rf/subscribe [::subs/physical-examination])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     ;; Comorbidities Section using ui-comorbidity-item
     [ui-comorbidity-item {:label "呼吸系统疾病"
                           :has-value (get-in comorbidities-data [:respiratory-disease :has])
                           :details-value (get-in comorbidities-data [:respiratory-disease :details])
                           :errors errors :base-path [:comorbidities :respiratory-disease] :data-index "3.1"}]
     [ui-comorbidity-item {:label "神经肌肉疾病"
                           :has-value (get-in comorbidities-data [:neuromuscular-disease :has])
                           :details-value (get-in comorbidities-data [:neuromuscular-disease :details])
                           :errors errors :base-path [:comorbidities :neuromuscular-disease] :data-index "3.2"}]
     [ui-comorbidity-item {:label "心血管疾病"
                           :has-value (get-in comorbidities-data [:cardiovascular-disease :has])
                           :details-value (get-in comorbidities-data [:cardiovascular-disease :details])
                           :errors errors :base-path [:comorbidities :cardiovascular-disease] :data-index "3.3"}]
     [ui-comorbidity-item {:label "肝脏疾病"
                           :has-value (get-in comorbidities-data [:liver-disease :has])
                           :details-value (get-in comorbidities-data [:liver-disease :details])
                           :errors errors :base-path [:comorbidities :liver-disease] :data-index "3.4"}]
     [ui-comorbidity-item {:label "内分泌疾病"
                           :has-value (get-in comorbidities-data [:endocrine-disease :has])
                           :details-value (get-in comorbidities-data [:endocrine-disease :details])
                           :errors errors :base-path [:comorbidities :endocrine-disease] :data-index "3.5"}]
     [ui-comorbidity-item {:label "肾脏疾病"
                           :has-value (get-in comorbidities-data [:kidney-disease :has])
                           :details-value (get-in comorbidities-data [:kidney-disease :details])
                           :errors errors :base-path [:comorbidities :kidney-disease] :data-index "3.6"}]
     [ui-comorbidity-item {:label "神经精神疾病"
                           :has-value (get-in comorbidities-data [:neuropsychiatric-disease :has])
                           :details-value (get-in comorbidities-data [:neuropsychiatric-disease :details])
                           :errors errors :base-path [:comorbidities :neuropsychiatric-disease] :data-index "3.7"}]
     [ui-comorbidity-item {:label "关节骨骼系统疾病"
                           :has-value (get-in comorbidities-data [:skeletal-system-disease :has])
                           :details-value (get-in comorbidities-data [:skeletal-system-disease :details])
                           :errors errors :base-path [:comorbidities :skeletal-system-disease] :data-index "3.8"}]
     [ui-comorbidity-item {:label "凝血功能"
                           :has-value (get-in comorbidities-data [:coagulation-function :has])
                           :details-value (get-in comorbidities-data [:coagulation-function :details])
                           :errors errors :base-path [:comorbidities :coagulation-function] :data-index "3.9"}]
     [ui-comorbidity-item {:label "既往麻醉、手术史"
                           :has-value (get-in comorbidities-data [:past-anesthesia-surgery :has])
                           :details-value (get-in comorbidities-data [:past-anesthesia-surgery :details])
                           :errors errors :base-path [:comorbidities :past-anesthesia-surgery] :data-index "3.10"}]
     [ui-comorbidity-item {:label "家族恶性高热史"
                           :has-value (get-in comorbidities-data [:family-malignant-hyperthermia :has])
                           :details-value (get-in comorbidities-data [:family-malignant-hyperthermia :details])
                           :errors errors :base-path [:comorbidities :family-malignant-hyperthermia] :data-index "3.11"}]

     ;; Special Medications section using ui-conditional-group
     [ui-conditional-group {:label "特殊用药史" :bool-value (get-in comorbidities-data [:special-medications :used])
                            :errors errors
                            :data-path [:comorbidities :special-medications] :field-key :used
                            :data-index "3.12"}
      [ui-input-item {:label "药物名称" :value (get-in comorbidities-data [:special-medications :details])
                      :errors errors
                      :data-path [:comorbidities :special-medications] :field-key :details
                      :placeholder "如有，请填写药物名称"}]
      [ui-date-picker-item {:label "最后服药时间" :value (get-in comorbidities-data [:special-medications :last-time])
                            :errors errors
                            :data-path [:comorbidities :special-medications] :field-key :last-time
                            :showTime true :placeholder "年/月/日 --:--"}]]

     ;; Physical Examination using ui-condition-item
     [ui-condition-item {:label "心脏" :base-value (:heart physical-exam-data) :detail-value (:heart-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :heart :data-index "3.13"}]
     [ui-condition-item {:label "肺脏" :base-value (:lungs physical-exam-data) :detail-value (:lungs-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :lungs :data-index "3.14"}]
     [ui-condition-item {:label "气道" :base-value (:airway physical-exam-data) :detail-value (:airway-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :airway :data-index "3.15"}]
     [ui-condition-item {:label "牙齿" :base-value (:teeth physical-exam-data) :detail-value (:teeth-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :teeth :data-index "3.16"}]
     [ui-condition-item {:label "脊柱四肢" :base-value (:spine-limbs physical-exam-data) :detail-value (:spine-limbs-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :spine-limbs :data-index "3.17"}]
     [ui-condition-item {:label "神经" :base-value (:nervous physical-exam-data) :detail-value (:nervous-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :nervous :data-index "3.18"}]

     [ui-input-item {:label "其它" :value (:other physical-exam-data) :errors errors
                     :data-path [:physical-examination] :field-key :other
                     :placeholder "请填写其他情况" :data-index "3.19"}]

     ;; Attachments Section
     [ui-file-input-item {:label "相关辅助检查检验结果" :value (:general-aux-report aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :general-aux-report :data-index "3.20"}]
     [ui-file-input-item {:label "胸片" :value (:chest-radiography aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :chest-radiography :data-index "3.21"}]
     [ui-file-input-item {:label "肺功能" :value (:pulmonary-function aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :pulmonary-function :data-index "3.22"}]
     [ui-file-input-item {:label "心脏彩超" :value (:cardiac-ultrasound aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :cardiac-ultrasound :data-index "3.23"}]
     [ui-file-input-item {:label "心电图" :value (:ecg aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :ecg :data-index "3.24"}]
     [ui-file-input-item {:label "其他" :value (:other aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :other :data-index "3.25"}]]))


;; Main patient form component
(defn patient-form []
  (let [current-step @(rf/subscribe [::subs/current-step])
        total-steps 3 ; Reflects the 3 steps defined below
        step-titles ["基本信息" "病情摘要" "麻醉评估"] ; Matches the 3 steps
        submitting? @(rf/subscribe [::subs/submitting?])]
    [:div.container
     [:div.card.mt-6
      [:h1.text-xl.font-bold.text-center.mb-4 {:style {:color "#1890ff" :font-size "22px"}}
       "麻醉评估问卷"]

      [progress-bar-component current-step total-steps step-titles]

      [:form#questionnaire-form {:onSubmit (fn [e] (.preventDefault e))}

       [step-section-wrapper "第一部分：基本信息" current-step 0 [basic-info-step]]
       [step-section-wrapper "第二部分：病情摘要" current-step 1 [medical-summary-step]]
       [step-section-wrapper "第三部分：麻醉评估" current-step 2 [coexisting-diseases-step]]

       [form-navigation current-step total-steps submitting?]

       (when @(rf/subscribe [::subs/patient-form-submit-success?])
         [:div.success-overlay
          [:div.success-icon "✓"]
          [:div.success-message "提交成功"]])]]]))
