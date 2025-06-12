(ns hc.hospital.components.common-assessment-card
  (:require
   ["@ant-design/icons" :refer [LoadingOutlined]]
   ["antd" :refer [Form Row Col]]
   ["react" :as React]
   [hc.hospital.components.assessment-form-components :as afc]
   [hc.hospital.pages.assessment-form-generators :as afg]
   [hc.hospital.form-utils :as form-utils]
   [hc.hospital.ui-helpers :as ui-helpers]
   [hc.hospital.natural-language-generators :as nlg]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [malli.core :as m]))

(defn- summary-view [props]
  (let [{:keys [data spec spec-key title]} props]
    (if (seq data)
      (nlg/generate-summary-component data spec spec-key)
      [:div.summary-section {:key (str (name spec-key) "-empty")
                             :style {:padding "10px" :border "1px solid #ddd" :margin-bottom "10px" :border-radius "4px"}}
       [:h3 {:style {:font-size "16px" :font-weight "bold" :margin-top "0" :margin-bottom "8px"}}
        (nlg/schema-key->display-label spec-key) "："]
       [:p {:style {:margin "0"}} "暂无数据可供总结。"]])))

(defn- detailed-view [props]
  (let [{:keys [report-form-instance-fn patient-id data spec spec-key on-show-summary on-finish-event additional-detailed-content]} props
        [form] (Form.useForm)
        initial-form-values (let [data-from-db (or data {})
                                  data-with-enum-defaults (form-utils/apply-enum-defaults-to-data
                                                            data-from-db
                                                            spec)
                                  data-with-parsed-dates (form-utils/preprocess-date-fields
                                                           data-with-enum-defaults
                                                           spec)]
                              data-with-parsed-dates)
        on-finish-fn (fn [values]
                       (let [values-clj (js->clj values :keywordize-keys true)
                             transformed-values (form-utils/transform-date-fields-for-submission
                                                  values-clj
                                                  spec)]
                         (rf/dispatch [on-finish-event spec-key transformed-values])))]
    (React/useEffect (fn []
                       (when report-form-instance-fn
                         (report-form-instance-fn spec-key form))
                       js/undefined)
                     #js [])
    (React/useEffect
     (fn []
       (.resetFields form)
       (.setFieldsValue form (clj->js initial-form-values))
       js/undefined)
     #js [initial-form-values])

    (let [form-items (into [:<>]
                           (mapv (fn [[field-key field-schema optional? _]]
                                   (afg/render-form-item-from-spec [field-key field-schema optional? [] form]))
                                 (m/entries spec)))]
      [afc/patient-assessment-card-wrapper
       {:patient-id patient-id
        :form-instance form
        :form-key (str patient-id "-" (name spec-key) "-spec")
        :initial-data initial-form-values
        :on-finish-handler on-finish-fn
        :children
        [:<>
         (when additional-detailed-content additional-detailed-content)
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
             "返回总结"]]]]]}])))

(defn common-assessment-card [props]
  (let [{:keys [title icon color spec spec-key data-sub patient-id-sub update-event report-form-instance-fn card-props additional-detailed-content]} props
        view-state (r/atom :summary)
        show-summary-fn #(reset! view-state :summary)
        toggle-view-fn #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [patient-id-sub])
        data @(rf/subscribe [data-sub])]
    (fn []
      (if (and patient-id data)
        [ui-helpers/custom-styled-card
         (when icon
           [icon {:style {:marginRight "8px"}}])
         title
         color
         (if (= @view-state :summary)
           [summary-view {:data data :spec spec :spec-key spec-key :title title}]
           [:f> detailed-view (merge card-props {:patient-id patient-id
                                                 :data data
                                                 :spec spec
                                                 :spec-key spec-key
                                                 :on-show-summary show-summary-fn
                                                 :on-finish-event update-event
                                                 :report-form-instance-fn report-form-instance-fn
                                                 :additional-detailed-content additional-detailed-content})])
         :on-click toggle-view-fn
         :view-state @view-state
         :card-style {:cursor "pointer"}
         :card-body-style {:padding "0px"}]
        [:> LoadingOutlined {:style {:font-size "24px" :padding "20px"}}]))))
