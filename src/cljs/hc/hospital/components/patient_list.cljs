(ns hc.hospital.components.patient-list
  (:require ["@ant-design/icons" :as icons :refer [SyncOutlined QrcodeOutlined UserOutlined]]
            ["antd" :refer [Button DatePicker Input Select Space Empty Tag]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [hc.hospital.subs :as subs]
            [hc.hospital.events :as events]))

(defn patient-list-filters []
  (let [date-range @(rf/subscribe [::subs/date-range])
        status @(rf/subscribe [::subs/assessment-status-filter])
        assessment-status-options [{:value "all" :label "全部状态"}
                                   {:value "待评估" :label "待评估"}
                                   {:value "评估中" :label "评估中"}
                                   {:value "已批准" :label "已批准"}
                                   {:value "已驳回" :label "已驳回"}
                                   {:value "已暂缓" :label "已暂缓"}]
        label-width "60px"]
    [:div {:style {:padding "16px" :borderBottom "1px solid #f0f0f0"}}
     [:> Space {:style {:marginBottom "16px"
                        :width "100%"
                        :display "flex"
                        :justifyContent "space-between"
                        :alignItems "center"}}
      [:> Button {:type "primary"
                  :icon (r/as-element [:> SyncOutlined])
                  :onClick #(rf/dispatch [::events/sync-applications])
                  :style {:display "flex" :alignItems "center"}}
       "同步申请"]
      [:> Button {:type "default"
                  :icon (r/as-element [:> QrcodeOutlined])
                  :on-click #(rf/dispatch [::events/open-qr-scan-modal])}
       "扫码签到"]]
     [:div {:style {:display "flex" :alignItems "center" :marginBottom "12px"}}
      [:span {:style {:width label-width
                      :textAlign "left"
                      :color "#666"
                      :marginRight "8px"}}
       "申请日期:"]
      [:> DatePicker.RangePicker
       {:style {:flex "1"}
        :value date-range
        :onChange #(rf/dispatch [::events/set-date-range %])}]]
     [:div {:style {:display "flex" :alignItems "center" :marginBottom "12px"}}
      [:span {:style {:width label-width
                      :textAlign "left"
                      :color "#666"
                      :marginRight "8px"}}
       "状态:"]
      [:> Select {:style {:flex "1"}
                  :placeholder "评估状态: 请选择"
                  :options assessment-status-options
                  :value status
                  :onChange #(rf/dispatch [::events/set-assessment-status-filter %])}]]
     [:div {:style {:display "flex" :alignItems "center"}}
      [:span {:style {:width label-width
                      :textAlign "left"
                      :color "#666"
                      :marginRight "8px"}}
       "患者:"]
      [:> Input.Search {:style {:flex "1"}
                        :placeholder "请输入患者姓名/门诊号"
                        :allowClear true
                        :onSearch #(rf/dispatch [::events/search-patients %])}]]]))

(defn patient-list [patients-sub]
  (let [patients @(rf/subscribe patients-sub)
        current-patient-id @(rf/subscribe [::subs/current-patient-id])]
    [:div {:style {:height "100%" :overflowY "auto"}}
     (if (seq patients)
       (for [item patients]
         ^{:key (:key item)}
         [:div {:style {:padding "10px 12px"
                        :borderBottom "1px solid #f0f0f0"
                        :display "flex"
                        :justifyContent "space-between"
                        :alignItems "center"
                        :background (when (= (:key item) current-patient-id) "#e6f7ff")
                        :cursor "pointer"}
                :onClick #(rf/dispatch [::events/select-patient (:key item)])}
          [:div {:style {:display "flex" :alignItems "center"}}
           [:> UserOutlined {:style {:marginRight "8px" :fontSize "16px"}}]
           [:div
            [:div {:style {:fontWeight "500"}} (:name item)]
            [:div {:style {:fontSize "12px" :color "gray"}}
             (str (:gender item) " " (:age item) " " (:anesthesia-type item))]
            (when (seq (:tags item))
              [:div {:style {:marginTop "4px"}}
               (for [{:keys [label color]} (:tags item)]
                 ^{:key label} [:> Tag {:color color :style {:marginRight "2px"}} label])])]]
          [:div {:style {:textAlign "right"}}
           [:div {:style {:fontSize "12px" :color "gray" :marginBottom "4px"}} (:date item)]
           [:> Tag {:color (case (:status item)
                             "待评估" "orange"
                             "已批准" "green"
                             "已暂缓" "blue"
                             "已驳回" "red"
                             "default")} (:status item)]]])
       [:> Empty {:description "暂无患者数据" :style {:marginTop "40px"}}])]))

(defn patient-list-panel [{:keys [patients-sub]}]
  [:<>
   [patient-list-filters]
   [patient-list patients-sub]])
