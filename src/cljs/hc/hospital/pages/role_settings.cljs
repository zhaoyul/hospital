(ns hc.hospital.pages.role-settings
  (:require ["antd" :refer [Button Modal Table Tree Tabs Card Typography Space]]
            ["react" :as React]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def tree-data
  [{:title "麻醉管理" :key 1 :children [{:title "查看" :key 101}]}
   {:title "问卷列表" :key 2 :children [{:title "查看" :key 102}]}
   {:title "系统管理" :key 3 :children [{:title "查看" :key 103}]}])

(defn role-modal []
  (let [visible? @(rf/subscribe [::subs/role-modal-visible?])
        role @(rf/subscribe [::subs/editing-role])
        checked (r/atom [])]
    (React/useEffect
     (fn []
       (when (:id role)
         (rf/dispatch [::events/fetch-role-permissions (:id role)]))
       nil)
     #js [role])
    (let [perm @(rf/subscribe [::subs/editing-role]) ; expect permissions in role
          _ (reset! checked (or (:permissions perm) []))]
      [:> Modal {:title (str "权限设置 - " (:name role))
                 :open visible?
                 :onOk #(rf/dispatch [::events/save-role-permissions (:id role) @checked])
                 :onCancel #(rf/dispatch [::events/close-role-modal])}
       [:> Tree {:checkable true
                  :checkedKeys @checked
                  :onCheck #(reset! checked (js->clj %2))
                  :treeData (clj->js tree-data)}]])))

(defn role-settings-tab []
  (let [roles @(rf/subscribe [::subs/roles])]
    (React/useEffect (fn [] (rf/dispatch [::events/initialize-roles])))
    [:div
     [:> Table {:dataSource roles
                :rowKey :id
                :columns [{:title "角色名" :dataIndex "name" :key "name"}
                          {:title "操作" :key "action"
                           :render (fn [_ r]
                                     (let [role (js->clj r :keywordize-keys true)]
                                       (r/as-element
                                        [:> Space {}
                                         [:> Button {:type "link"
                                                     :on-click #(rf/dispatch [::events/open-role-modal role])}
                                          "权限"]])))}]}]
     [:f> role-modal]]))
