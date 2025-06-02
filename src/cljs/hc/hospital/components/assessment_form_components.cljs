(ns hc.hospital.components.assessment-form-components
  (:require
   [taoensso.timbre :as timbre :refer [spy]]
   ["antd" :refer [Form Empty Radio]]
   [hc.hospital.ui-helpers :refer [custom-styled-card]]))

(defn patient-assessment-card-wrapper
  [{:keys [icon title header-color patient-id
           form-instance form-key initial-data on-finish-handler on-values-change-handler
           children]}]
  [custom-styled-card
   icon ; Assuming icon is already a hiccup vector like [:> HeartOutlined ...]
   title
   header-color
   (if patient-id
     [:> Form (cond-> {:form form-instance
                       :key form-key
                       :layout "vertical"
                       :initialValues (when initial-data (clj->js initial-data))
                       :onFinish on-finish-handler}
                on-values-change-handler (assoc :onValuesChange on-values-change-handler))
      children]
     [:> Empty {:description "请先选择患者"}])])

(defn form-item-radio-conditional
  [{:keys [form-instance label radio-name ; This 'radio-name' is the original prop key (e.g., a keyword)
           radio-options conditional-value children
           extra-condition-values value-for-children-wrapper]}]

  (let [field-identifier (clj->js (spy :info radio-name)) ; Renamed 'radio-name' from let to 'field-identifier'
        watched-value (Form.useWatch field-identifier form-instance) ; Use new name for watching
        show-children? (or (= (spy :info watched-value) (spy :info conditional-value))
                           (when (spy :info extra-condition-values)
                             (some #(= watched-value %) extra-condition-values)))
        wrap-children? (if value-for-children-wrapper
                         (= watched-value value-for-children-wrapper)
                         (= watched-value conditional-value))]
    [:<>
     [:> Form.Item {:label label :name field-identifier} ; Use new name for Form.Item
      [:> Radio.Group {:options radio-options
                       :onChange #(let [value (-> % .-target .-value)]
                                    ; 'radio-name' here now unambiguously refers to the destructured prop from {:keys ...}
                                    (.setFieldsValue form-instance (js-obj (name radio-name) value)))}]]
     (when show-children?
       (if wrap-children?
         [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
          children]
         children))]))
