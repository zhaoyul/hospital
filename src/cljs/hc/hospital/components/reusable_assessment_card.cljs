(ns hc.hospital.components.reusable-assessment-card
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            ["antd" :refer [Button]] ; Changed from antd.core
            [hc.hospital.pages.assessment-form-generators :as afg]
            [hc.hospital.natural-language-generators :as nlg]
            [hc.hospital.form-utils :as form-utils]
            [hc.hospital.ui-helpers :as ui-helpers]
            [hc.hospital.components.assessment-form-components :as afc]
            [hc.hospital.events :as events] ; Ensure this is used or remove
            [hc.hospital.subs :as subs]))

(defn reusable-assessment-card
  "A reusable card component for displaying and editing assessment sections."
  [{:keys [assessment-key title icon color spec data-sub update-event
           report-form-instance-fn static-content summary-view-fn]}]
  (let [view-state (r/atom :summary)
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number]) ; Used by data-sub indirectly
        assessment-data @(rf/subscribe data-sub)
        toggle-view-fn #(swap! view-state (fn [current-state]
                                            (if (= current-state :summary) :detailed :summary)))]
    (r/as-element
     [ui-helpers/custom-styled-card
      {:icon icon
       :title title
       :color color
       :on-click toggle-view-fn}
      (condp = @view-state
        :summary
        (if summary-view-fn
          (summary-view-fn assessment-data)
          (if (or (nil? assessment-data) (empty? assessment-data)) ; Added nil check
            [:p "No data to summarize"]
            [nlg/generate-summary-component {:spec spec
                                             :data assessment-data
                                             :assessment-key assessment-key}]))

        :detailed
        [:div
         (when static-content
           static-content)
         [afc/patient-assessment-card-wrapper
          {:form-spec spec
           :form-props {:initial-values (-> assessment-data
                                            (form-utils/apply-enum-defaults-to-data spec)
                                            (form-utils/preprocess-date-fields spec))
                        :on-finish (fn [values]
                                     (let [transformed-values (form-utils/transform-date-fields-for-submission spec values)]
                                       ;; Assuming update-event is a vector like [:event-id]
                                       ;; and the event expects [assessment-key data] as additional args.
                                       (rf/dispatch (vec (concat update-event [assessment-key transformed-values])))))
                        :report-form-instance-fn report-form-instance-fn}
           :render-form-item-fn afg/render-form-item-from-spec}]
         [:> Button {:on-click toggle-view-fn ; Changed from antd/button
                       :type "primary"
                       :style {:margin-top "10px"}}
          "Return to Summary"] ; Button closes
         ] ; Detailed div closes
        ) ; condp closes
       ] ; custom-styled-card vector closes
      ) ; r/as-element closes
     ) ; let closes
    ) ; defn closes
