(ns hc.hospital.pages.assessment-card-common
  (:require ["antd" :refer [Form Row Col]]
            ["react" :as React]
            [malli.core :as m]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [hc.hospital.components.assessment-form-components :as afc]
            [hc.hospital.pages.assessment-form-generators :as afg]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.natural-language-generators :as nlg]
            [hc.hospital.form-utils :as form-utils]
            [hc.hospital.ui-helpers :as ui-helpers]))

(defn assessment-card
  "根据传入配置渲染评估卡片。"
  [{:keys [section-key title icon color spec data-sub report-form-instance-fn]}]
  (let [view-state (r/atom :summary)
        show-summary #(reset! view-state :summary)
        toggle-view #(reset! view-state (if (= @view-state :summary) :detailed :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        section-data @(rf/subscribe data-sub)]
    (fn []
      [ui-helpers/custom-styled-card
       icon title color
       (if (= @view-state :summary)
         (if (seq section-data)
           (let [summary (nlg/generate-summary-component section-data spec section-key)]
             (if (and (vector? summary) (empty? summary))
               [:div {:style {:padding "10px"}} (str "暂无" title "评估数据可供总结 (内容为空)。")]
               summary))
           [:div {:style {:padding "10px"}} (str "暂无" title "评估数据可供总结。")])
         (let [[form] (Form.useForm)
               initial (-> section-data
                            (or {})
                            (form-utils/apply-enum-defaults-to-data spec)
                            (form-utils/preprocess-date-fields spec))
               on-finish (fn [values]
                           (rf/dispatch [::events/update-canonical-assessment-section
                                         section-key
                                         (form-utils/transform-date-fields-for-submission
                                          (js->clj values :keywordize-keys true)
                                          spec)]))]
           (React/useEffect
            (fn []
              (when report-form-instance-fn
                (report-form-instance-fn section-key form))
              js/undefined)
            #js [])
           [afc/patient-assessment-card-wrapper
            {:patient-id patient-id
             :form-instance form
             :form-key (str patient-id "-" (name section-key) "-spec")
             :initial-data initial
             :on-finish-handler on-finish
             :children
             [:<>
              (into [:<>]
                    (mapv (fn [[k s opt? _]]
                            (afg/render-form-item-from-spec [k s opt? [] form]))
                          (m/entries spec)))
              [:> Row {:justify "end" :style {:marginTop "20px"}}
               [:> Col
                [:> Form.Item
                 [:button {:type "button"
                           :on-click show-summary
                           :style {:padding "5px 10px"
                                   :background-color "#f0f0f0"
                                   :border "1px solid #ccc"
                                   :border-radius "4px"
                                   :cursor "pointer"}}
                  "返回总结"]]]]]}]))
       :on-click toggle-view
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}]))
