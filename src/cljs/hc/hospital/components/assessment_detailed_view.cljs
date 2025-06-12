(ns hc.hospital.components.assessment-detailed-view
  "通用评估详细视图组件，负责渲染表单并处理提交。"
  (:require ["antd" :refer [Form Row Col]]
            ["react" :as React]
            [re-frame.core :as rf]
            [malli.core :as m]
            [hc.hospital.events :as events]
            [hc.hospital.form-utils :as form-utils]
            [hc.hospital.components.assessment-form-components :as afc]
            [hc.hospital.pages.assessment-form-generators :as afg]))

(defn assessment-section-detailed-view
  "根据 Malli Spec 生成评估表单。接受 section-key、spec、数据等参数，
   自动处理初始化、提交和展示额外内容。"
  [{:keys [section-key spec data patient-id on-show-summary
           report-form-instance-fn form-key extra-content]}]
  (let [[form] (Form.useForm)
        initial-values (-> (or data {})
                           (form-utils/apply-enum-defaults-to-data spec)
                           (form-utils/preprocess-date-fields spec))
        on-finish (fn [values]
                    (let [values-clj (js->clj values :keywordize-keys true)
                          transformed (form-utils/transform-date-fields-for-submission
                                       values-clj spec)]
                      (rf/dispatch [::events/update-canonical-assessment-section
                                    section-key transformed])))]
    (React/useEffect
     (fn []
       (when report-form-instance-fn
         (report-form-instance-fn section-key form))
       js/undefined)
     #js [])
    (React/useEffect
     (fn []
       (.resetFields form)
       (.setFieldsValue form (clj->js initial-values))
       js/undefined)
     #js [initial-values])
    (let [form-items (into [:<>]
                           (mapv (fn [[field-key field-schema optional? _]]
                                   (afg/render-form-item-from-spec
                                    [field-key field-schema optional? [] form]))
                                 (m/entries spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id      patient-id
        :form-instance   form
        :form-key        (or form-key (str patient-id "-" (name section-key) "-spec"))
        :initial-data    initial-values
        :on-finish-handler on-finish
        :children
        [:<>
         extra-content
         form-items
         [:> Row {:justify "end" :style {:marginTop "20px"}}
          [:> Col
           [:> Form.Item
            [:button {:type "button"
                      :on-click on-show-summary
                      :style {:padding "5px 10px"
                              :background-color "#f0f0f0"
                              :border "1px solid #ccc"
                              :border-radius "4px"
                              :cursor "pointer"}}
             "返回总结"]]]]]]))))
