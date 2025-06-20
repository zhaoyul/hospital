(ns hc.hospital.pages.checkin
  (:require ["antd" :refer [Layout Card Button Empty]]
            ["@ant-design/icons" :refer [SaveOutlined]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [hc.hospital.subs :as subs]
            [hc.hospital.events :as events]
            [hc.hospital.pages.anesthesia :as anesthesia]))

(defn patient-list []
  (let [patients @(rf/subscribe [::subs/unchecked-patients])
        current-id @(rf/subscribe [::subs/current-patient-id])]
    [:div {:style {:height "100%" :overflowY "auto"}}
     (if (seq patients)
       (for [p patients]
         ^{:key (:key p)}
         [:div {:style {:padding "10px 12px"
                        :borderBottom "1px solid #f0f0f0"
                        :background (when (= (:key p) current-id) "#e6f7ff")
                        :cursor "pointer"}
                :onClick #(rf/dispatch [::events/select-patient (:key p)])}
          (:name p)])
       [:> Empty {:description "暂无待签到患者"}])]))

(defn patient-list-panel []
  [:div {:style {:padding "16px"}}
   [patient-list]])

(defn save-button []
  [:> Layout.Footer {:style {:padding "10px 0"
                             :background "white"
                             :borderTop "1px solid #f0f0f0"
                             :textAlign "center"}}
   [:> Button {:type "primary"
               :icon (r/as-element [:> SaveOutlined])
               :onClick (fn []
                          (rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :签到时间] (.toISOString (js/Date.))])
                          (rf/dispatch [::events/save-final-assessment-later]))}
    "确认签到"]])

(defn assessment []
  (let [current-id @(rf/subscribe [::subs/current-patient-id])]
    (if current-id
      [:> Layout {:style {:display "flex" :flexDirection "column" :height "calc(100vh - 64px)"}}
       [:> Layout.Content {:style {:padding "5px 12px" :overflowY "auto" :flexGrow 1 :background "#f0f2f5"}}
        [:f> anesthesia/patient-info-card]
        [anesthesia/general-condition-card]]
       [save-button]]
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100%"}}
       [:> Empty {:description "请选择患者"}]])))

(defn checkin-content []
  [:> Layout.Content {:style {:margin 0 :minHeight 280 :overflow "hidden" :display "flex"}}
   [:> Card {:style {:width "400px" :minWidth "350px" :height "calc(100vh - 64px)" :borderRight "1px solid #f0f0f0" :padding "0"}}
    [patient-list-panel]]
   [:div {:style {:flexGrow 1 :background "#f0f2f5" :overflow "hidden" :display "flex" :flexDirection "column"}}
    [assessment]]])
