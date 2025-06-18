(ns hc.hospital.pages.settings
  (:require
   ["@ant-design/icons" :as icons]
   ["react" :as React]
   ["antd" :refer [Button Space Table Typography Card Tabs]]
   [hc.hospital.components.user-modal :refer [user-modal]]
   [hc.hospital.pages.role-settings :refer [role-settings-tab]]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]))


(defn user-settings-tab []
  (let [users @(rf/subscribe [::subs/users])
        modal-open? @(rf/subscribe [::subs/user-modal-visible?])
        editing-user @(rf/subscribe [::subs/editing-user])]

    (React/useEffect (fn [] (rf/dispatch [::events/initialize-users])))

    [:div
     [:div {:style {:marginBottom "16px"}}
      [:> Button {:type "primary"
                  :icon (r/as-element [:> icons/PlusOutlined])
                  :on-click #(rf/dispatch [::events/open-user-modal nil])}
       "新增医生"]]

     [:> Table {:dataSource users
                :rowKey :id
                :style {:marginTop "16px"}
                :columns [{:title "姓名"
                           :dataIndex "name"
                           :key "name"
                           :align "center"}

                          {:title "账号"
                           :dataIndex "username"
                           :key "username"
                           :align "center"}

                          {:title "角色"
                           :dataIndex "role"
                           :key "role"
                           :align "center"}

                          {:title "电子签名"
                           :dataIndex "signature"
                           :key "signature"
                           :align "center"
                           :render (fn [signature _record]
                                     (cond
                                       (nil? signature) "无签名"
                                       (string? signature)
                                       (r/as-element [:img {:src signature
                                                            :alt "签名"
                                                            :style {:width "48px"
                                                                    :height "48px"
                                                                    :borderRadius "50%"
                                                                    :objectFit "cover"
                                                                    :border "1px solid #eee"}}])
                                       (:thumbUrl signature)
                                       (r/as-element [:img {:src (:thumbUrl signature)
                                                            :alt "签名"
                                                            :style {:width "48px"
                                                                    :height "48px"
                                                                    :borderRadius "50%"
                                                                    :objectFit "cover"
                                                                    :border "1px solid #eee"}}])
                                       :else "无签名"))}

                          {:title "操作"
                           :key "action"
                           :align "center"
                           :render (fn [_text record]
                                     (let [user (js->clj record :keywordize-keys true)]
                                       (r/as-element
                                        [:> Space {}
                                         [:> Button {:type "primary"
                                                     :ghost true
                                                     :size "small"
                                                     :icon (r/as-element [:> icons/EditOutlined])
                                                     :on-click #(rf/dispatch [::events/open-user-modal user])}
                                          "编辑"]
                                         [:> Button {:danger true
                                                     :ghost true
                                                     :size "small"
                                                     :icon (r/as-element [:> icons/DeleteOutlined])
                                                     :on-click #(rf/dispatch [::events/delete-user (:id user)])}
                                          "删除"]])))}]}]

     (when modal-open?
       [:f> user-modal {:visible? modal-open?
                        :editing-user editing-user}])]))

(defn system-settings-content []
  [:> Card
   [:> Typography.Title {:level 2
                         :style {:marginTop 0 :marginBottom "16px"
                                 :fontSize "18px" :fontWeight 500 :color "#333"}}
    "系统设置"]
   [:> Tabs {:defaultActiveKey "users"}
    [:> Tabs.TabPane {:tab "用户设置" :key "users"}
     [user-settings-tab]]
    [:> Tabs.TabPane {:tab "角色设置" :key "roles"}
     [role-settings-tab]]]])
