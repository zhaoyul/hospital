(ns hc.hospital.components.assessment-card
  "用于评估界面中切换概要/详细视图的通用卡片组件。"
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.ui-helpers :as ui-helpers]
            [hc.hospital.subs :as subs]))

(defn assessment-section-card
  "通用评估卡片组件。接受图标、标题、背景色、订阅及视图组件等参数，
  自动处理视图切换和数据传递。"
  [{:keys [icon title bg-color data-sub data-key summary-view detailed-view]
    :as props}]
  (let [view-state (r/atom :summary)
        show-summary #(reset! view-state :summary)
        toggle-view #(reset! view-state (if (= @view-state :summary)
                                          :detailed
                                          :summary))
        patient-id @(rf/subscribe [::subs/canonical-patient-outpatient-number])
        section-data @(rf/subscribe data-sub)]
    (fn []
      [ui-helpers/custom-styled-card
       icon
       title
       bg-color
       (if (= @view-state :summary)
         [summary-view {data-key section-data}]
         [:f> detailed-view (merge props
                                   {:patient-id    patient-id
                                    data-key       section-data
                                    :on-show-summary show-summary})])
       :on-click toggle-view
       :view-state @view-state
       :card-style {:cursor "pointer"}
       :card-body-style {:padding "0px"}])))
