(ns hc.hospital.components.generic-assessment-card
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hc.hospital.ui-helpers :as ui-helpers]
   [hc.hospital.pages.assessment-form-generators :as afg]
   [hc.hospital.form-utils :as form-utils]
   [hc.hospital.natural-language-generators :as nlg]
   [hc.hospital.subs :as subs]
   [hc.hospital.events :as events]
   ["antd" :as antd :refer [Form Button]])) ; Specifically import Form and Button

(defn generic-assessment-card
  [card-config]
  (let [{:keys [spec-key title icon header-bg-color malli-spec data-sub event-key
                 static-detailed-content-fn summary-view-fn
                 preprocess-data-fn postprocess-data-fn]} card-config
        view-state (r/atom :summary)
        ;; patient-id is subscribed here if needed by any passed-in functions,
        ;; though not directly used by the card's own rendering logic.
        _patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])]

    (fn [card-config] ;; Re-receive card-config for consistency, though outer one is in scope
      (let [;; Destructure again in case this function is called directly elsewhere,
            ;; though typically it's rendered by the outer fn's closure.
            {:keys [spec-key title icon header-bg-color malli-spec data-sub event-key
                    static-detailed-content-fn summary-view-fn
                    preprocess-data-fn postprocess-data-fn]} card-config
            current-data @(rf/subscribe data-sub)
            preprocess-fn (or preprocess-data-fn
                              #(-> %
                                   (form-utils/apply-enum-defaults-to-data malli-spec)
                                   (form-utils/preprocess-date-fields malli-spec)))
            postprocess-fn (or postprocess-data-fn
                               #(form-utils/transform-date-fields-for-submission malli-spec %))]

        (if (= @view-state :summary)
          ;; Summary View
          [ui-helpers/custom-styled-card
           {:title title
            :icon icon
            :header-bg-color header-bg-color
            :extra [:button {:on-click #(reset! view-state :detailed)} "Edit"]}
           (if (or (nil? current-data) (empty? current-data))
             [:div.no-data-message "No data available."] ;; Consistent empty state
             (if summary-view-fn
               (summary-view-fn current-data malli-spec)
               [nlg/generate-summary-component current-data malli-spec spec-key]))]

          ;; Detailed View
          [ui-helpers/custom-styled-card
           {:title (str "Edit " title)
            :icon icon
            :header-bg-color header-bg-color
            :extra [:button {:on-click #(reset! view-state :summary)} "Back to Summary"]}

           (when static-detailed-content-fn
             (static-detailed-content-fn))

           (let [initial-values (preprocess-fn current-data)
                 [form-instance] (antd/Form.useForm)] ; Get the form instance
             [:> Form
              {:form form-instance ; Pass the instance to the Form
               :layout "vertical"
               :initialValues (clj->js initial-values)
               :onFinish (fn [values]
                           (let [cljs-values (js->clj values :keywordize-keys true)
                                 processed-values (postprocess-fn cljs-values)]
                             (rf/dispatch [event-key spec-key processed-values])
                             (reset! view-state :summary)
                             (.resetFields form-instance)))} ; Reset fields after submission

              (afg/render-map-schema-fields malli-spec [] form-instance) ; Call render-map-schema-fields

              [:> Form.Item
               [:> Button {:type "primary" :htmlType "submit"}
                "Save"]]])])))))
