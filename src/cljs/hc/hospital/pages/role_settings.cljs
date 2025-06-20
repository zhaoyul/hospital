(ns hc.hospital.pages.role-settings
  (:require ["antd" :refer [Button Modal Table Tree Tabs Card Typography Space]]
            ["react" :as React]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def action-labels
  {"系统管理" {"view-users" "查看用户"
             "create-user" "新增用户"
             "edit-user" "编辑用户"
             "delete-user" "删除用户"
             "view-roles" "查看角色"
             "edit-role" "角色编辑"}})

(defn action->label [module action]
  (get-in action-labels [module action] "查看"))

(defn build-tree-data [permissions]
  (->> permissions
       (group-by :module)
       (map (fn [[module perms]]
              {:title module
               :key module
               :children (mapv (fn [{:keys [id action]}]
                                  {:title (action->label module action)
                                   :key id})
                                perms)}))
       vec))

(defn role-modal []
  (let [visible? @(rf/subscribe [::subs/role-modal-visible?])
        role @(rf/subscribe [::subs/editing-role])
        permissions @(rf/subscribe [::subs/permissions])
        tree-data (build-tree-data permissions)
        ;; 本地勾选状态使用 React/useState 保存
        [checked set-checked] (React/useState [])]
    ;; 当打开弹窗或角色变更时获取权限
    (React/useEffect
     (fn []
       (when (:id role)
         (rf/dispatch [::events/fetch-role-permissions (:id role)]))
       js/undefined)
     #js [(:id role)])
    ;; 根据订阅到的权限更新本地状态
    (React/useEffect
     (fn []
       (set-checked (or (:permissions role) []))
       js/undefined)
     #js [(:permissions role)])
    [:> Modal {:title (str "权限设置 - " (:name role))
               :open visible?
               :onOk #(rf/dispatch [::events/save-role-permissions (:id role) checked])
               :onCancel #(rf/dispatch [::events/close-role-modal])}
     [:> Tree {:checkable true
               :checkedKeys (clj->js checked)
               :onCheck (fn [keys# _#]
                          (set-checked (js->clj keys#)))
               :treeData (clj->js tree-data)}]]))

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
