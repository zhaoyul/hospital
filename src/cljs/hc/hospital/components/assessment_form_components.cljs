(ns hc.hospital.components.assessment-form-components
  (:require
   [re-frame.core :as rf]
   [hc.hospital.patient.events :as events]
   ["antd" :refer [Form Empty Radio Card]]
   [reagent.core :as r]))

(defn patient-assessment-card-wrapper
  [{:keys [patient-id
           form-instance form-key initial-data on-finish-handler on-values-change-handler
           children]}]
  [:> Card
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
  [{:keys [form-instance label radio-name
           radio-options conditional-value
           extra-condition-values value-for-children-wrapper]}
   & children]

  (let [field-identifier (clj->js radio-name)
        watched-value (Form.useWatch field-identifier form-instance) ; Use new name for watching
        show-children? (or (= watched-value conditional-value)
                           (when extra-condition-values
                             (some #(= watched-value %) extra-condition-values)))
        wrap? (if value-for-children-wrapper
                (= watched-value value-for-children-wrapper)
                (= watched-value conditional-value))]
    (into
     [:<>
      [:> Form.Item {:label (r/as-element label) :name field-identifier}
       [:> Radio.Group {:options radio-options
                        :onChange #(let [value (-> % .-target .-value)]
                                     (rf/dispatch [::events/update-form-field radio-name value]))}]]
      (when show-children?
        (if wrap?
          [:div {:style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
           (into [:<>] children)]
          (into [:<>] children)))])))
