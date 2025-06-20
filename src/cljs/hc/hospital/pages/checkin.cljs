(ns hc.hospital.pages.checkin
  (:require ["antd" :refer [Layout Card Button Empty]]
            ["@ant-design/icons" :refer [SaveOutlined]]
            ["react" :as React]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [hc.hospital.subs :as subs]
            [hc.hospital.events :as events]
            [hc.hospital.pages.anesthesia :as anesthesia]
            [hc.hospital.components.patient-list :refer [patient-list-panel]]))


(defn save-button []
  [:> Layout.Footer {:style {:padding "10px 0"
                             :background "white"
                             :borderTop "1px solid #f0f0f0"
                             :textAlign "center"}}
  [:> Button {:type "primary"
               :icon (r/as-element [:> SaveOutlined])
               :onClick (fn []
                          (rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :签到时间] (.toISOString (js/Date.))])
                          (rf/dispatch [::events/update-canonical-assessment-field [:基本信息 :评估状态] "评估中"]) ; 护士签到后状态变更
                          (rf/dispatch [::events/save-final-assessment-later]))}
    "确认签到"]])

(defn- checkin-header [patient-name patient-status]
  [:> Card {:style {:marginBottom "12px"}}
   [:div {:style {:display "flex" :justifyContent "space-between" :alignItems "center"}}
    [:h3 {:style {:fontSize "16px" :fontWeight "500"}} patient-name]
    (when (= patient-status "已批准")
      [:> Button {:type "primary"
                  :danger true
                  :on-click #(rf/dispatch [::events/reject-patient])}
       "退回"])
    (when (not= patient-status "已批准")
      [:> Button {:type "primary"
                  :on-click #(rf/dispatch [::events/approve-patient])}
       "批准"])]])

(defn assessment []
  (let [current-id @(rf/subscribe [::subs/current-patient-id])
        basic-info @(rf/subscribe [::subs/canonical-basic-info])
        patient-name (get basic-info :姓名 "未知患者")
        patient-status (get basic-info :评估状态 "待评估")
        editable? (not= patient-status "已批准")] 
    (if current-id
      [:> Layout {:style {:display "flex" :flexDirection "column" :height "calc(100vh - 64px)"}}
       [:> Layout.Content {:style {:padding "5px 12px" :overflowY "auto" :flexGrow 1 :background "#f0f2f5"}}
        [checkin-header patient-name patient-status]
        [:f> anesthesia/patient-info-card {:include-surgery-fields? false
                                           :editable? editable?}]
        [anesthesia/general-condition-card {:editable? editable?}]]
       [save-button]]
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100%"}}
       [:> Empty {:description "请选择患者"}]])))

(defn checkin-content []
  (React/useEffect
   (fn []
     (rf/dispatch [::events/set-assessment-status-filter "待评估"])
     js/undefined)
   #js [])
  [:> Layout.Content {:style {:margin 0 :minHeight 280 :overflow "hidden" :display "flex"}}
   [:> Card {:style {:width "400px" :minWidth "350px" :height "calc(100vh - 64px)" :borderRight "1px solid #f0f0f0" :padding "0"}}
    [patient-list-panel {:patients-sub [::subs/unchecked-patients]}]]
   [:div {:style {:flexGrow 1 :background "#f0f2f5" :overflow "hidden" :display "flex" :flexDirection "column"}}
    [assessment]]])
