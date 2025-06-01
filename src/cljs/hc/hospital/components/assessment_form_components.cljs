(ns hc.hospital.components.assessment-form-components
  (:require
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
  [{:keys [form-instance label radio-name radio-options conditional-value children
           extra-condition-values value-for-children-wrapper]}]
  (let [watched-value (Form/useWatch radio-name form-instance)
        show-children? (or (= watched-value conditional-value)
                           (when extra-condition-values
                             (some #(= watched-value %) extra-condition-values)))
        wrap-children? (if value-for-children-wrapper
                         (= watched-value value-for-children-wrapper)
                         (= watched-value conditional-value))]
    [:<>
     [:> Form.Item {:label label :name radio-name}
      [:> Radio.Group {:options radio-options}]]
     (when show-children?
       (if wrap-children?
         [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
          children]
         children))]))
