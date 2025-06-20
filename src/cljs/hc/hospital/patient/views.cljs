(ns hc.hospital.patient.views
  (:require
   [hc.hospital.patient.events :as events]
   [hc.hospital.patient.subs :as subs]
   [hc.hospital.specs.patient-questionnaire-spec :as pq-spec]
   [hc.hospital.utils :as utils]
   [malli.core :as m]
   [re-frame.core :as rf]
   [clojure.string :as str]))

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
(defn- normalize-error-msg [msg]
  (cond
    (string? msg) msg
    (coll? msg) (str/join "; " (flatten msg))
    :else (when msg (str msg))))

(defn ui-input-item [{:keys [label value placeholder errors field-key data-path type unit data-index extra required? pattern]
                     :or {type "text" required? false}}]
  (let [full-path (conj data-path field-key)
        error-msg (normalize-error-msg (get-in errors full-path))
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index}
      label
      (when required? [:span {:style {:color "red" :margin-left "2px"}} "*"])
      (when unit [:span.unit (str " " unit)])]
     [:div {:style {:display "flex" :gap "8px"}}
      [:input.form-input (merge 
                          {:type type
                           :id field-id
                           :value (if (nil? value) "" value)
                           :placeholder (or placeholder (str "请输入" (if (string? label) label "")))
                           :onChange #(rf/dispatch [::events/update-form-field full-path (-> % .-target .-value)])}
                          ;; 有pattern时添加模式验证
                          (when pattern {:pattern pattern}))]
      (when extra extra)]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-input-number-item [{:keys [label value placeholder errors field-key data-path min max step unit data-index required?]
                             :or {required? false}}]
  (let [full-path (conj data-path field-key)
        error-msg (normalize-error-msg (get-in errors full-path))
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index}
      label
      (when required? [:span {:style {:color "red" :margin-left "2px"}} "*"])
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

(defn ui-radio-group-item [{:keys [label value errors field-key data-path options data-index required?]
                           :or {required? false}}]
  (let [full-path (conj data-path field-key)
        error-msg (normalize-error-msg (get-in errors full-path))
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} 
      label
      (when required? [:span {:style {:color "red" :margin-left "2px"}} "*"])]
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
        error-msg (normalize-error-msg (get-in errors full-path))
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
                :checked (not (true? bool-value)) ; Default to false if bool-value is nil or false
                :onChange #(rf/dispatch [::events/update-form-field full-path false])}]
       [:span " 无"]]]
     (when error-msg
       [:div.error-message error-msg])]))

(defn ui-date-picker-item [{:keys [label value placeholder errors field-key data-path showTime data-index]}]
  (let [full-path (conj data-path field-key)
        error-msg (normalize-error-msg (get-in errors full-path))
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
        error-msg (normalize-error-msg (get-in errors full-path))
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
        error-msg (normalize-error-msg (or (get-in errors base-path) (get-in errors detail-path)))
        group-name (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:data-index data-index} label]
     [:div.radio-group
      [:label.radio-label
       [:input {:type "radio" :name group-name :value "normal" :checked (not= base-value "abnormal") ; Default to normal
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

(defn ui-select-item [{:keys [label value placeholder errors field-key data-path options data-index required?]
                     :or {required? false}}]
  (let [full-path (conj data-path field-key)
        error-msg (normalize-error-msg (get-in errors full-path))
        field-id (str (name field-key) "-" (hash data-path))]
    [:div {:class (if error-msg "form-group error" "form-group")}
     [:label.form-label {:for field-id :data-index data-index} 
      label
      (when required? [:span {:style {:color "red" :margin-left "2px"}} "*"])]
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
    :or {has-field-key :has details-field-key :详情 details-placeholder "请填写详情"}}]
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

;; --- Spec Based Utilities ---
(defn keyword->label [k]
  (-> k name (str/replace "-" "")))

(defn- required-field? [path schema]
  "判断字段是否为必填。仅在第一部分(基本信息)中所有字段均必填。"
  (and (= (first path) :基本信息)
       (not= :maybe (m/type (m/deref (m/schema schema))))))

(defn enum-options [schema]
  (when (= :enum (m/type schema))
    (mapv (fn [opt] {:value (if (keyword? opt) (name opt) opt)
                     :label (if (keyword? opt) (name opt) (str opt))})
          (m/children schema))))

(defn comorbidity-map-schema? [schema]
  (and (= :map (m/type schema))
       (= [:有无 :详情] (map first (m/entries schema)))))

(defn boolean-map-schema? [schema]
  (and (= :map (m/type schema))
       (= :有无 (ffirst (m/entries schema)))))

(defn status-map-schema? [schema]
  (and (= :map (m/type schema))
       (= :状态 (ffirst (m/entries schema)))))

(declare render-item)

(defn render-map-spec [form-data errors path spec]
  (into [:<>]
        (mapv (fn [[k s]] (render-item form-data errors path k s))
              (m/entries spec))))

(defn render-item [form-data errors path field-key schema]
  (let [schema (m/deref (m/schema schema))]
    (cond
      (= :maybe (m/type schema))
      (render-item form-data errors path field-key (first (m/children schema)))

      (= :map (m/type schema))
      (let [entries (m/entries schema)
            field-path (conj path field-key)]
        (cond
          (comorbidity-map-schema? schema)
          (let [has-val (get-in form-data (conj field-path :有无))
                detail-val (get-in form-data (conj field-path :详情))]
            [ui-comorbidity-item {:label (keyword->label field-key)
                                 :has-value has-val
                                 :details-value detail-val
                                 :errors errors
                                 :base-path field-path}])

          (boolean-map-schema? schema)
          (let [bool-val (get-in form-data (conj field-path :有无))
                children (mapv (fn [[k sch]] (render-item form-data errors field-path k sch))
                               (rest entries))]
            (into [ui-conditional-group {:label (keyword->label field-key)
                                         :bool-value bool-val
                                         :errors errors
                                         :data-path field-path
                                         :field-key :有无}]
                  children))

          (status-map-schema? schema)
          (let [base-val (get-in form-data (conj field-path :状态))
                detail-val (get-in form-data (conj field-path :描述))]
            [ui-condition-item {:label (keyword->label field-key)
                               :base-value base-val
                               :detail-value detail-val
                               :errors errors
                               :data-path field-path
                               :field-key :状态
                               :placeholder "如有异常，请说明"}])

          :else
          (into [:div.form-group-container
                 [:h4.section-subtitle (keyword->label field-key)]]
                (mapv (fn [[k sch]] (render-item form-data errors field-path k sch))
                      entries))))

      (= :string (m/type schema))
      [ui-input-item {:label (keyword->label field-key)
                      :value (get-in form-data (conj path field-key))
                      :errors errors
                      :data-path path
                      :field-key field-key
                      :required? (required-field? path schema)}]

      (= :re (m/type schema))
      (let [regex (first (m/children schema))]
        [ui-input-item {:label (keyword->label field-key)
                        :value (get-in form-data (conj path field-key))
                        :errors errors
                        :data-path path
                        :field-key field-key
                        :pattern (.-source regex)
                        :required? (required-field? path schema)}])

      (#{:int :double :float} (m/type schema))
      [ui-input-number-item {:label (keyword->label field-key)
                             :value (get-in form-data (conj path field-key))
                             :errors errors
                             :data-path path
                             :field-key field-key
                             :required? (required-field? path schema)}]

      (= :enum (m/type schema))
      (let [options (enum-options schema)]
        (if (> (count options) 3)
          [ui-select-item {:label (keyword->label field-key)
                           :value (get-in form-data (conj path field-key))
                           :errors errors
                           :data-path path
                           :field-key field-key
                           :options options
                           :required? (required-field? path schema)}]
          [ui-radio-group-item {:label (keyword->label field-key)
                                :value (get-in form-data (conj path field-key))
                                :errors errors
                                :data-path path
                                :field-key field-key
                                :options options
                                :required? (required-field? path schema)}]))

      (= :boolean (m/type schema))
      [ui-boolean-radio-item {:label (keyword->label field-key)
                              :bool-value (get-in form-data (conj path field-key))
                              :errors errors
                              :data-path path
                              :field-key field-key
                              :required? (required-field? path schema)}]

      :else
      [:p (str "未支持的字段类型:" (m/type schema))])))

;; --- Form Steps ---
(defn basic-info-step []
  (let [form @(rf/subscribe [::subs/patient-form])
        errors @(rf/subscribe [::subs/form-errors])]
    [render-map-spec form errors [:基本信息] pq-spec/PatientBasicInfoSpec]))

(defn medical-summary-step []
  (let [form @(rf/subscribe [::subs/patient-form])
        errors @(rf/subscribe [::subs/form-errors])]
    [render-map-spec form errors [:病情摘要] pq-spec/PatientMedicalSummarySpec]))

(defn coexisting-diseases-step []
  (let [form @(rf/subscribe [::subs/patient-form])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [render-map-spec form errors [:合并症] pq-spec/PatientComorbiditiesSpec]
     [render-map-spec form errors [:体格检查] pq-spec/PatientPhysicalExamSpec]
     [render-map-spec form errors [:辅助检查] pq-spec/PatientAuxiliaryExamSpec]]))




;; Main patient form component
(defn patient-form []
  (let [current-step @(rf/subscribe [::subs/current-step])
        total-steps 3 ; Reflects the 3 steps defined below
        step-titles ["基本信息（必填）" "病情摘要" "麻醉评估"] ; Matches the 3 steps
        submitting? @(rf/subscribe [::subs/submitting?])]
    [:div.container
     [:div.card.mt-6
      [:h1.text-xl.font-bold.text-center.mb-4 {:style {:color "#1890ff" :font-size "22px"}}
       "麻醉评估问卷"]

      [progress-bar-component current-step total-steps step-titles]

      [:form#questionnaire-form {:onSubmit (fn [e] (.preventDefault e))}

       [step-section-wrapper "第一部分：基本信息（必填）" current-step 0 [basic-info-step]]
       [step-section-wrapper "第二部分：病情摘要" current-step 1 [medical-summary-step]]
       [step-section-wrapper "第三部分：麻醉评估" current-step 2 [coexisting-diseases-step]]

       [form-navigation current-step total-steps submitting?]

       (when @(rf/subscribe [::subs/patient-form-submit-success?])
         [:div.success-overlay
          [:div.success-icon "✓"]
          [:div.success-message "提交成功"]])]]]))
