(ns hc.hospital.patient.views
  (:require [re-frame.core :as rf]
            [hc.hospital.patient.events :as events]
            [hc.hospital.patient.subs :as subs]
            [hc.hospital.utils :as utils]
            [clojure.string :as str]))

;; --- Progress Bar ---
(defn progress-bar-component [current-step total-steps step-titles]
  [:div.progress-bar
   [:div.progress-steps
    (for [i (range total-steps)]
      [:div {:class (str "progress-step" (if (= i current-step) " active" "")) ; current-step is 0-indexed
             :key (str "step-" i)
             :id (str "step" (inc i))
             :data-title (nth step-titles i nil)}
       (inc i)])]
   [:div#progress.progress-bar-fill
    {:style {:width (str (* (/ (inc current-step) total-steps) 100) "%")}
     ;; Removed inline styles for height, background-color, border-radius
     }]])

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

(defn ui-text-area-item [{:keys [label value placeholder errors field-key data-path rows data-index] ; Added data-index
                          :or {rows 3}}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index} label] ; Added :data-index
     [:textarea.form-input {:id field-id
                            :value (if (nil? value) "" value)
                            :rows rows
                            :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                            :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}]
     (when error-msg
       [:div.error-message  error-msg])]))

(defn ui-input-number-item [{:keys [label value placeholder errors field-key data-path min max step unit data-index]}] ; Added data-index
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index} ; Added :data-index
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
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label] ; Added :data-index
     [:div.radio-group
      [:label.radio-label
       [:input {:type "radio"
                :name group-name
                :value "yes"
                :checked (true? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path true])}]
       [:span " 有"]]
      [:label.radio-label
       [:input {:type "radio"
                :name group-name
                :value "no"
                :checked (false? bool-value)
                :onChange #(rf/dispatch [::events/update-form-field full-path false])}]
       [:span " 无"]]] ; Added span
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-date-picker-item [{:keys [label value placeholder errors field-key data-path showTime data-index]}] ; Added data-index
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
        display-value (if (and value (not= value "-") (seq value)) ; Changed (not (empty? value)) to (seq value)
                        (str "已选择: " value)
                        "选择文件")]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label] ; Main label for the field group
     [:label.file-upload-label {:for field-id} ; Visually styled label acting as button
      [:i.fas.fa-upload.mr-2]
      display-value]
     [:input.file-upload
      {:type "file"
       :id field-id
       :style {:display "none"} ; Explicitly hide if CSS isn't loaded/working
       :onChange #(let [file-name (if (-> % .-target .-files .-length (> 0))
                                    (-> % .-target .-files (.item 0) .-name)
                                    nil)]
                    (rf/dispatch [::events/update-form-field full-path (or file-name "-")]))}]
     (when error-msg
       [:div.error-message error-msg])]))

;; Component for items like "Heart condition" which have Normal/Abnormal radio and conditional details text
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


;; 定义 ui-select-item 组件
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
      (for [{:keys [label value]} options]
        ^{:key value}
        [:option {:value value} label])]
     (when error-msg
       [:div.error-message error-msg])]))


;; --- Form Steps ---

;; 更新基本信息部分以支持扫码功能和最新字段
(defn basic-info-step []
  (let [basic-info @(rf/subscribe [::subs/basic-info])
        errors @(rf/subscribe [::subs/form-errors])
        outpatient-number-path [:basic-info :outpatient-number]] ; 定义路径
    [:<>
     [ui-input-item {:label "门诊号" :value (:outpatient-number basic-info) :errors errors
                     :data-path [:basic-info] :field-key :outpatient-number
                     :placeholder "请输入门诊号" :data-index "1.1"
                     :extra [:button.btn-scan
                             {:onClick (fn []
                                         ;; 1. 设置全局回调函数
                                         (set! (.-onScanSuccessCallback js/window)
                                               (fn [scanned-value]
                                                 ;; 2. 分发事件更新表单字段
                                                 (rf/dispatch [::events/update-form-field outpatient-number-path scanned-value])
                                                 ;; 3. 清理回调函数 (可选但推荐)
                                                 (set! (.-onScanSuccessCallback js/window) nil)))
                                         ;; 4. 调用 JavaScript 扫描函数
                                         (js/startScan))}
                             "扫码"]}]
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

;; 一般情况步骤
(defn general-condition-step []
  (let [general-condition @(rf/subscribe [::subs/general-condition])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-input-number-item {:label "身高" :value (:height general-condition) :errors errors
                            :data-path [:general-condition] :field-key :height
                            :min 1 :max 300 :unit "cm" ; Adjusted min based on typical values
                            :placeholder "请输入身高" :data-index "2.1"}]
     [ui-input-number-item {:label "体重" :value (:weight general-condition) :errors errors
                            :data-path [:general-condition] :field-key :weight
                            :min 1 :max 500 :unit "kg" ; Adjusted max
                            :placeholder "请输入体重" :data-index "2.2"}]
     [ui-input-item {:label "精神状态" :value (:mental-state general-condition) :errors errors ;; Assuming mental-state is now text
                     :data-path [:general-condition] :field-key :mental-state
                     :placeholder "请输入精神状态" :data-index "2.3"}]
     [ui-input-item {:label "活动能力" :value (:activity-ability general-condition) :errors errors
                     :data-path [:general-condition] :field-key :activity-ability
                     :placeholder "请输入活动能力" :data-index "2.4"}]

     (let [bp-path [:general-condition :blood-pressure]
           systolic-val (get-in general-condition [:blood-pressure :systolic])
           diastolic-val (get-in general-condition [:blood-pressure :diastolic])
           ;; Using (seq coll) for non-empty check is idiomatic, but (not-empty string) is also fine and clear for strings.
           ;; The linter might have been for a direct (not (empty? ...)) check.
           bp-combined-val (when (or (not-empty systolic-val) (not-empty diastolic-val))
                             (str (or systolic-val "") "/" (or diastolic-val "")))
           bp-error (get-in errors bp-path) ;; Simplified error check
           field-id-bp (str "blood-pressure-" (hash bp-path))]
       [:div {:class (if bp-error "form-group error" "form-group")}
        [:label.form-label {:for field-id-bp :data-index "2.5"} "血压 " [:span.unit "mmHg"]]
        [:input.form-input {:type "text" :id field-id-bp :name "blood_pressure" :placeholder "例如：120/80"
                            :value bp-combined-val
                            :onChange #(let [value (-> % .-target .-value)
                                             parts (str/split value #"/")
                                             systolic (not-empty (first parts)) ; not-empty is fine here
                                             diastolic (not-empty (second parts))] ; not-empty is fine here
                                         (rf/dispatch [::events/update-form-field (conj bp-path :systolic) systolic])
                                         (rf/dispatch [::events/update-form-field (conj bp-path :diastolic) diastolic]))}]
        (when bp-error [:div.error-message (if (string? bp-error) bp-error "输入无效")])])

     [ui-input-number-item {:label "脉搏" :value (:pulse general-condition) :errors errors
                            :data-path [:general-condition] :field-key :pulse :unit "次/分"
                            :placeholder "请输入脉搏" :data-index "2.6"}]
     [ui-input-number-item {:label "呼吸" :value (:respiration general-condition) :errors errors
                            :data-path [:general-condition] :field-key :respiration :unit "次/分"
                            :placeholder "请输入呼吸频率" :data-index "2.7"}]
     [ui-input-number-item {:label "体温" :value (:temperature general-condition) :errors errors
                            :data-path [:general-condition] :field-key :temperature :unit "℃" :step 0.1
                            :placeholder "请输入体温" :data-index "2.8"}]
     [ui-input-number-item {:label "SpO2" :value (:spo2 general-condition) :errors errors
                            :data-path [:general-condition] :field-key :spo2 :unit "%"
                            :placeholder "请输入血氧饱和度" :data-index "2.9"}]]))

;; 病情摘要步骤
(defn medical-summary-step []
  (let [summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-boolean-radio-item {:label "过敏史" :bool-value (:allergy-history summary) :errors errors
                             :data-path [:medical-summary] :field-key :allergy-history :data-index "3.1"}]
     (when (:allergy-history summary)
       [:<>
        [ui-input-item {:label "过敏原" :value (:allergen summary) :errors errors
                        :data-path [:medical-summary] :field-key :allergen
                        :placeholder "请输入过敏原" :data-index "3.2"}]
        [ui-date-picker-item {:label "过敏时间" :value (:allergy-date summary) :errors errors
                              :data-path [:medical-summary] :field-key :allergy-date
                              :placeholder "请选择过敏时间" :data-index "3.3"}]])

     [ui-boolean-radio-item {:label "吸烟史" :bool-value (:smoking-history summary) :errors errors
                             :data-path [:medical-summary] :field-key :smoking-history :data-index "3.4"}]
     (when (:smoking-history summary)
       [:<>
        [ui-input-number-item {:label "吸烟年数" :value (:smoking-years summary) :errors errors
                               :data-path [:medical-summary] :field-key :smoking-years
                               :placeholder "请输入吸烟年数" :data-index "3.5"}]
        [ui-input-number-item {:label "每天吸烟支数" :value (:cigarettes-per-day summary) :errors errors
                               :data-path [:medical-summary] :field-key :cigarettes-per-day
                               :placeholder "请输入每天吸烟支数" :data-index "3.6"}]])

     [ui-boolean-radio-item {:label "饮酒史" :bool-value (:drinking-history summary) :errors errors
                             :data-path [:medical-summary] :field-key :drinking-history :data-index "3.7"}]
     (when (:drinking-history summary)
       [:<>
        [ui-input-number-item {:label "饮酒年数" :value (:drinking-years summary) :errors errors
                               :data-path [:medical-summary] :field-key :drinking-years
                               :placeholder "请输入饮酒年数" :data-index "3.8"}]
        [ui-input-item {:label "每天饮酒量" :value (:alcohol-per-day summary) :errors errors
                        :data-path [:medical-summary] :field-key :alcohol-per-day
                        :placeholder "请输入每天饮酒量(如 白酒2两 或 啤酒1瓶)" :data-index "3.9"}]])]))


;; 并存疾病及其他信息步骤
(defn coexisting-diseases-step []
  (let [comorbidities-data @(rf/subscribe [::subs/comorbidities])
        aux-exam-data @(rf/subscribe [::subs/auxiliary-examination])
        physical-exam-data @(rf/subscribe [::subs/physical-examination]) ; Added for condition items
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     ;; Comorbidities Section
     [ui-boolean-radio-item {:label "呼吸系统疾病" :bool-value (:respiratory-disease comorbidities-data)
                             :errors errors :data-path [:comorbidities] :field-key :respiratory-disease
                             :data-index "4.1"}]
     [ui-boolean-radio-item {:label "神经肌肉疾病" :bool-value (:neuromuscular-disease comorbidities-data) ; Assumes :neuromuscular-disease in db
                             :errors errors :data-path [:comorbidities] :field-key :neuromuscular-disease
                             :data-index "4.2"}]
     [ui-boolean-radio-item {:label "心血管疾病" :bool-value (:cardiovascular-disease comorbidities-data)
                             :errors errors :data-path [:comorbidities] :field-key :cardiovascular-disease
                             :data-index "4.3"}]
     [ui-boolean-radio-item {:label "肝脏疾病" :bool-value (:liver-disease comorbidities-data) ; Assumes :liver-disease in db
                             :errors errors :data-path [:comorbidities] :field-key :liver-disease ; Fixed typo: :comororbidities -> :comorbidities
                             :data-index "4.4"}]
     [ui-boolean-radio-item {:label "内分泌疾病" :bool-value (:endocrine-disease comorbidities-data)
                             :errors errors :data-path [:comorbidities] :field-key :endocrine-disease
                             :data-index "4.5"}]
     [ui-boolean-radio-item {:label "肾脏疾病" :bool-value (:kidney-disease comorbidities-data) ; Assumes :kidney-disease in db
                             :errors errors :data-path [:comorbidities] :field-key :kidney-disease
                             :data-index "4.6"}]
     [ui-boolean-radio-item {:label "神经精神疾病" :bool-value (:neuropsychiatric-disease comorbidities-data)
                             :errors errors :data-path [:comorbidities] :field-key :neuropsychiatric-disease
                             :data-index "4.7"}]
     [ui-boolean-radio-item {:label "关节骨骼系统" :bool-value (:skeletal-system comorbidities-data) ; DB key is :skeletal-system
                             :errors errors :data-path [:comorbidities] :field-key :skeletal-system
                             :data-index "4.8"}]
     [ui-boolean-radio-item {:label "既往麻醉、手术史" :bool-value (:past-anesthesia-surgery comorbidities-data) ; DB key is :past-anesthesia-surgery
                             :errors errors :data-path [:comorbidities] :field-key :past-anesthesia-surgery
                             :data-index "4.9"}]
     [ui-boolean-radio-item {:label "家族恶性高热史" :bool-value (:family-malignant-hyperthermia comorbidities-data)
                             :errors errors :data-path [:comorbidities] :field-key :family-malignant-hyperthermia
                             :data-index "4.10"}]

     ;; Special Medications - This section needs review for DB structure alignment
     ;; Current DB: [:comorbidities :special-medications {:used "..." :last-time "..."}]
     ;; The :bool-value for "使用的特殊药物" needs to correctly interpret the :used string (e.g. "yes", "no", "-") as a boolean.
     ;; The :value for "药物名称" needs to map to a new :details key under :special-medications.
     ;; The :value for "最后服药时间" needs to map to :last-time under :special-medications.
     [ui-boolean-radio-item {:label "使用的特殊药物"
                             :bool-value (let [used-val (get-in comorbidities-data [:special-medications :used])]
                                           (cond
                                             (true? used-val) true
                                             (false? used-val) false
                                             (= "yes" used-val) true ;; Example: adapt if DB stores strings
                                             :else false))
                             :errors errors :data-path [:comorbidities :special-medications] :field-key :used ;; Path updated
                             :data-index "4.11"}]
     (when (let [used-val (get-in comorbidities-data [:special-medications :used])]
             (or (true? used-val) (= "yes" used-val))) ;; Condition to show details
       [ui-input-item {:label "药物名称" :value (get-in comorbidities-data [:special-medications :details]) ;; Assumes :details key
                       :errors errors
                       :data-path [:comorbidities :special-medications] :field-key :details
                       :placeholder "如有，请填写药物名称"}])

     [ui-date-picker-item {:label "最后服药时间" :value (get-in comorbidities-data [:special-medications :last-time]) ;; Path updated
                           :errors errors
                           :data-path [:comorbidities :special-medications] :field-key :last-time
                           :showTime true :placeholder "年/月/日 --:--" :data-index "4.12"}]

     ;; Condition Items - Sourced from physical-examination data
     ;; Current DB for these items are flat strings. ui-condition-item expects base-value and detail-value.
     ;; This requires either DB structure change (e.g. :heart {:status "normal", :detail "..."})
     ;; or ui-condition-item to be adapted, or separate fields in DB for details.
     ;; For now, mapping base-value to the string, and detail-value to an assumed :field-detail key.
     [ui-condition-item {:label "心脏" :base-value (:heart physical-exam-data) :detail-value (:heart-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :heart :data-index "4.13"}]
     [ui-condition-item {:label "肺脏" :base-value (:lungs physical-exam-data) :detail-value (:lungs-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :lungs :data-index "4.14"}]
     [ui-condition-item {:label "气道" :base-value (:airway physical-exam-data) :detail-value (:airway-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :airway :data-index "4.15"}]
     [ui-condition-item {:label "牙齿" :base-value (:teeth physical-exam-data) :detail-value (:teeth-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :teeth :data-index "4.16"}]
     [ui-condition-item {:label "脊柱四肢" :base-value (:spine-limbs physical-exam-data) :detail-value (:spine-limbs-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :spine-limbs :data-index "4.17"}]
     [ui-condition-item {:label "神经" :base-value (:nervous physical-exam-data) :detail-value (:nervous-detail physical-exam-data)
                         :errors errors :data-path [:physical-examination] :field-key :nervous :data-index "4.18"}]

     [ui-input-item {:label "其它" :value (:other physical-exam-data) :errors errors ; From physical-examination
                     :data-path [:physical-examination] :field-key :other
                     :placeholder "请填写其他情况" :data-index "4.19"}]

     ;; Attachments Section - Sourced from auxiliary-examination data
     [ui-file-input-item {:label "相关辅助检查检验结果" :value (:general-aux-report aux-exam-data) :errors errors ; Assumes :general-aux-report in DB
                          :data-path [:auxiliary-examination] :field-key :general-aux-report :data-index "4.20"}]
     [ui-file-input-item {:label "胸片" :value (:chest-radiography aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :chest-radiography :data-index "4.21"}]
     [ui-file-input-item {:label "肺功能" :value (:pulmonary-function aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :pulmonary-function :data-index "4.22"}]
     [ui-file-input-item {:label "心脏彩超" :value (:cardiac-ultrasound aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :cardiac-ultrasound :data-index "4.23"}]
     [ui-file-input-item {:label "心电图" :value (:ecg aux-exam-data) :errors errors
                          :data-path [:auxiliary-examination] :field-key :ecg :data-index "4.24"}]
     [ui-file-input-item {:label "其他" :value (:other aux-exam-data) :errors errors ; DB key is :other for this context
                          :data-path [:auxiliary-examination] :field-key :other :data-index "4.25"}]]))


;; Main patient form component
(defn patient-form []
  (let [current-step @(rf/subscribe [::subs/current-step])
        total-steps 3 ; 更新为最新问卷的总步骤数
        step-titles ["基本信息" "病情摘要" "麻醉评估"] ; 更新为最新问卷的步骤标题
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

       [form-navigation current-step total-steps submitting?]]]]))
