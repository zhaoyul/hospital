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
(defn ui-input-item [{:keys [label value placeholder errors field-key data-path type unit data-index extra required? pattern]
                      :or {type "text" required? false}}]
  (let [full-path (conj data-path field-key)
        error-msg (get-in errors full-path)
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
        error-msg (get-in errors full-path)
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
        error-msg (get-in errors full-path)
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
                :checked (not (true? bool-value)) ; Default to false if bool-value is nil or false
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
        error-msg (get-in errors full-path)
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


;; --- Form Steps ---
(defn basic-info-step []
  (let [basic-info @(rf/subscribe [::subs/basic-info])
        errors @(rf/subscribe [::subs/form-errors])
        outpatient-number-path [:基本信息 :门诊号]]
    [:<>
     [ui-input-item {:label "门诊号" :value (:门诊号 basic-info) :errors errors
                     :data-path [:基本信息] :field-key :门诊号
                     :placeholder "请输入门诊号" :data-index "1.1"
                     :extra [:button.btn-scan
                             {:onClick (fn []
                                         (set! (.-onScanSuccessCallback js/window)
                                               (fn [scanned-value]
                                                 (rf/dispatch [::events/update-form-field outpatient-number-path scanned-value])
                                                 (set! (.-onScanSuccessCallback js/window) nil)))
                                         (js/startScan))}
                             [:i {:class "fas fa-qrcode"}]]
                     :required? true}]
     [ui-input-item {:label "姓名" :value (:姓名 basic-info) :errors errors
                     :data-path [:基本信息] :field-key :姓名
                     :placeholder "请输入姓名" :data-index "1.2"
                     :required? true}]
     [ui-input-item {:label "身份证号" :value (:身份证号 basic-info) :errors errors
                     :data-path [:基本信息] :field-key :身份证号
                     :placeholder "请输入身份证号" :data-index "1.3"
                     :required? true}]
     [ui-input-item {:label "手机号" :value (:手机号 basic-info) :errors errors
                     :data-path [:基本信息] :field-key :手机号
                     :placeholder "请输入手机号（1开头的11位数字）" :data-index "1.4"
                     :type "tel" ;; 使用tel类型以便在移动设备上调出数字键盘
                     :pattern "^1[0-9]{10}$" ;; HTML5模式验证
                     :required? true}]
     [ui-radio-group-item {:label "性别" :value (:性别 basic-info) :errors errors
                           :data-path [:基本信息] :field-key :性别
                           :options [{:label "男" :value "男"} {:label "女" :value "女"}]
                           :data-index "1.5"
                           :required? true}]
     [ui-input-number-item {:label "年龄" :value (:年龄 basic-info) :errors errors
                            :data-path [:基本信息] :field-key :年龄
                            :placeholder "请输入年龄" :data-index "1.6"
                            :required? true}]
     [ui-select-item {:label "院区" :value (:院区 basic-info) :errors errors
                      :data-path [:基本信息] :field-key :院区
                      :options [{:label "总院" :value "main"} {:label "积水潭院区" :value "jst"}]
                      :placeholder "请选择院区" :data-index "1.7"
                      :required? true}]]))

(defn medical-summary-step "病情摘要步骤" []
  (let [summary @(rf/subscribe [::subs/medical-summary])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     [ui-conditional-group {:label "过敏史" :bool-value (:过敏史 summary) :errors errors
                            :data-path [:病情摘要] :field-key :过敏史 :data-index "2.1"}
      [ui-input-item {:label "过敏源" :value (:过敏源 summary) :errors errors
                      :data-path [:病情摘要] :field-key :过敏源
                      :placeholder "请输入过敏源"}] ; data-index for sub-items if needed, or rely on parent
      [ui-date-picker-item {:label "过敏时间" :value (:过敏时间 summary) :errors errors
                            :data-path [:病情摘要] :field-key :过敏时间
                            :placeholder "请选择过敏时间"}]]

     [ui-conditional-group {:label "吸烟史" :bool-value (:吸烟史 summary) :errors errors
                            :data-path [:病情摘要] :field-key :吸烟史 :data-index "2.2"}
      [ui-input-number-item {:label "吸烟年数" :value (:年数 summary) :errors errors
                             :data-path [:病情摘要] :field-key :年数
                             :placeholder "请输入吸烟年数"}]
      [ui-input-number-item {:label "每天吸烟支数" :value (:每天支数 summary) :errors errors
                             :data-path [:病情摘要] :field-key :每天支数
                             :placeholder "请输入每天吸烟支数"}]]

     [ui-conditional-group {:label "饮酒史" :bool-value (:饮酒史 summary) :errors errors
                            :data-path [:病情摘要] :field-key :饮酒史 :data-index "2.3"}
      [ui-input-number-item {:label "饮酒年数" :value (:年数 summary) :errors errors
                             :data-path [:病情摘要] :field-key :年数
                             :placeholder "请输入饮酒年数"}]
      [ui-input-item {:label "每天饮酒量" :value (:每天量 summary) :errors errors
                      :data-path [:病情摘要] :field-key :每天量
                      :placeholder "请输入每天饮酒量(如 白酒2两 或 啤酒1瓶)"}]]]))

(defn coexisting-diseases-step []
  (let [comorbidities-data @(rf/subscribe [::subs/comorbidities])
        aux-exam-data @(rf/subscribe [::subs/auxiliary-examination])
        physical-exam-data @(rf/subscribe [::subs/physical-examination])
        errors @(rf/subscribe [::subs/form-errors])]
    [:<>
     ;; Comorbidities Section using ui-comorbidity-item
     [ui-comorbidity-item {:label "呼吸系统疾病"
                           :has-value (get-in comorbidities-data [:呼吸系统疾病 :has])
                           :details-value (get-in comorbidities-data [:呼吸系统疾病 :详情])
                           :errors errors :base-path [:合并症 :呼吸系统疾病] :data-index "3.1"}]
     [ui-comorbidity-item {:label "神经肌肉疾病"
                           :has-value (get-in comorbidities-data [:神经肌肉疾病 :has])
                           :details-value (get-in comorbidities-data [:神经肌肉疾病 :详情])
                           :errors errors :base-path [:合并症 :神经肌肉疾病] :data-index "3.2"}]
     [ui-comorbidity-item {:label "心血管疾病"
                           :has-value (get-in comorbidities-data [:心血管疾病 :has])
                           :details-value (get-in comorbidities-data [:心血管疾病 :详情])
                           :errors errors :base-path [:合并症 :心血管疾病] :data-index "3.3"}]
     [ui-comorbidity-item {:label "肝脏疾病"
                           :has-value (get-in comorbidities-data [:肝脏疾病 :has])
                           :details-value (get-in comorbidities-data [:肝脏疾病 :详情])
                           :errors errors :base-path [:合并症 :肝脏疾病] :data-index "3.4"}]
     [ui-comorbidity-item {:label "内分泌疾病"
                           :has-value (get-in comorbidities-data [:内分泌疾病 :has])
                           :details-value (get-in comorbidities-data [:内分泌疾病 :详情])
                           :errors errors :base-path [:合并症 :内分泌疾病] :data-index "3.5"}]
     [ui-comorbidity-item {:label "肾脏疾病"
                           :has-value (get-in comorbidities-data [:肾脏疾病 :has])
                           :details-value (get-in comorbidities-data [:肾脏疾病 :详情])
                           :errors errors :base-path [:合并症 :肾脏疾病] :data-index "3.6"}]
     [ui-comorbidity-item {:label "神经精神疾病"
                           :has-value (get-in comorbidities-data [:神经精神疾病 :has])
                           :details-value (get-in comorbidities-data [:神经精神疾病 :详情])
                           :errors errors :base-path [:合并症 :神经精神疾病] :data-index "3.7"}]
     [ui-comorbidity-item {:label "关节骨骼系统疾病"
                           :has-value (get-in comorbidities-data [:关节骨骼系统疾病 :has])
                           :details-value (get-in comorbidities-data [:关节骨骼系统疾病 :详情])
                           :errors errors :base-path [:合并症 :关节骨骼系统疾病] :data-index "3.8"}]
     [ui-comorbidity-item {:label "凝血功能"
                           :has-value (get-in comorbidities-data [:凝血功能 :has])
                           :details-value (get-in comorbidities-data [:凝血功能 :详情])
                           :errors errors :base-path [:合并症 :凝血功能] :data-index "3.9"}]
     [ui-comorbidity-item {:label "既往麻醉、手术史"
                           :has-value (get-in comorbidities-data [:既往麻醉手术史 :has])
                           :details-value (get-in comorbidities-data [:既往麻醉手术史 :详情])
                           :errors errors :base-path [:合并症 :既往麻醉手术史] :data-index "3.10"}]
     [ui-comorbidity-item {:label "家族恶性高热史"
                           :has-value (get-in comorbidities-data [:家族恶性高热史 :has])
                           :details-value (get-in comorbidities-data [:家族恶性高热史 :详情])
                           :errors errors :base-path [:合并症 :家族恶性高热史] :data-index "3.11"}]

     ;; Special Medications section using ui-conditional-group
     [ui-conditional-group {:label "特殊用药史" :bool-value (get-in comorbidities-data [:特殊用药史 :使用过])
                            :errors errors
                            :data-path [:合并症 :特殊用药史] :field-key :使用过
                            :data-index "3.12"}
      [ui-input-item {:label "药物名称" :value (get-in comorbidities-data [:特殊用药史 :详情])
                      :errors errors
                      :data-path [:合并症 :特殊用药史] :field-key :详情
                      :placeholder "如有，请填写药物名称"}]
      [ui-date-picker-item {:label "最后服药时间" :value (get-in comorbidities-data [:特殊用药史 :最后时间])
                            :errors errors
                            :data-path [:合并症 :特殊用药史] :field-key :最后时间
                            :showTime true :placeholder "年/月/日 --:--"}]]

     ;; Physical Examination using ui-condition-item
     [ui-condition-item {:label "心脏" :base-value (:心脏 physical-exam-data) :detail-value (:心脏-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :心脏 :data-index "3.13"}]
     [ui-condition-item {:label "肺脏" :base-value (:肺脏 physical-exam-data) :detail-value (:肺脏-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :肺脏 :data-index "3.14"}]
     [ui-condition-item {:label "气道" :base-value (:气道 physical-exam-data) :detail-value (:气道-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :气道 :data-index "3.15"}]
     [ui-condition-item {:label "牙齿" :base-value (:牙齿 physical-exam-data) :detail-value (:牙齿-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :牙齿 :data-index "3.16"}]
     [ui-condition-item {:label "脊柱四肢" :base-value (:脊柱四肢 physical-exam-data) :detail-value (:脊柱四肢-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :脊柱四肢 :data-index "3.17"}]
     [ui-condition-item {:label "神经" :base-value (:神经 physical-exam-data) :detail-value (:神经-detail physical-exam-data)
                         :errors errors :data-path [:体格检查] :field-key :神经 :data-index "3.18"}]

     [ui-input-item {:label "其它" :value (:其它 physical-exam-data) :errors errors
                     :data-path [:体格检查] :field-key :其它
                     :placeholder "请填写其他情况" :data-index "3.19"}]

     ;; Attachments Section
     [ui-file-input-item {:label "相关辅助检查检验结果" :value (:相关辅助检查检验结果 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :相关辅助检查检验结果 :data-index "3.20"}]
     [ui-file-input-item {:label "胸片" :value (:胸片 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :胸片 :data-index "3.21"}]
     [ui-file-input-item {:label "肺功能" :value (:肺功能 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :肺功能 :data-index "3.22"}]
     [ui-file-input-item {:label "心脏彩超" :value (:心脏彩超 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :心脏彩超 :data-index "3.23"}]
     [ui-file-input-item {:label "心电图" :value (:心电图 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :心电图 :data-index "3.24"}]
     [ui-file-input-item {:label "其他" :value (:其它 aux-exam-data) :errors errors
                          :data-path [:辅助检查] :field-key :其它 :data-index "3.25"}]]))


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
