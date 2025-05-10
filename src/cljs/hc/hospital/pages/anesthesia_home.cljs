(ns hc.hospital.pages.anesthesia-home
  (:require
   ["@ant-design/icons" :as icons]
   ["antd" :refer [Avatar Layout Menu Space Space Tag Typography]] ; Added Modal, Table, Space
   [hc.hospital.events :as events]
   [hc.hospital.pages.anesthesia :refer [anesthesia-content]]
   [hc.hospital.pages.comps :refer [custom-sider-trigger]]
   [hc.hospital.pages.settings :refer [system-settings-content]]
   [hc.hospital.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as timbre]))

(def menu-items
  [{:key "1" :icon (r/as-element [:> icons/ProfileOutlined]) :label "麻醉管理"}
   {:key "2" :icon (r/as-element [:> icons/FileAddOutlined]) :label "问卷列表"}
   {:key "3" :icon (r/as-element [:> icons/SettingOutlined]) :label "系统管理"}])

(defn sider-bar [active-tab]
  (let [sidebar-collapsed? (r/atom true)]
    (fn []
      [:> Layout.Sider {:theme "light"
                        :collapsed @sidebar-collapsed?
                        :trigger nil
                        :collapsible true}
       [custom-sider-trigger sidebar-collapsed? #(swap! sidebar-collapsed? not)]
       [:> Menu {:mode "inline"
                 :inlineCollapsed @sidebar-collapsed?
                 :selectedKeys [(case active-tab
                                  "patients" "1"
                                  "assessment" "2"
                                  "settings" "3" ; Map "settings" tab to key "3"
                                  "1")] ; Default to "1" if no match
                 :onClick (fn [item]
                            (rf/dispatch (timbre/spy :info [::events/set-active-tab
                                                            (condp = (.-key item)
                                                              "1" "patients"
                                                              "2" "assessment"
                                                              "3" "settings")]))) ; Key "3" maps to "settings"
                 :style {:height "100%" :borderRight 0}
                 :items menu-items}]])))

;; 新增顶部导航栏组件
(defn app-header []
  [:> Layout.Header
   {:style {:background "#fff"
            :padding "0 24px" ; Adjusted padding
            :display "flex"
            :alignItems "center"
            :justifyContent "space-between"
            :borderBottom "1px solid #f0f0f0"
            :height "64px"}}
   [:div {:style {:display "flex" :alignItems "center"}}
    ;; Using ApartmentOutlined as a placeholder for fas fa-hospital-user
    [:> icons/ApartmentOutlined {:style {:fontSize "24px" :color "#1890ff" :marginRight "10px"}}]
    [:> Typography.Title {:level 4 :style {:margin 0 :fontSize "18px"}} "麻醉信息管理平台"]]

   ;; 右侧：用户信息
   [:div {:style {:display "flex" :alignItems "center" :cursor "pointer"}
          ;; :on-click #(rf/dispatch [::events/toggle-user-dropdown]) ; 稍后可以为用户下拉菜单添加事件
          }
    [:> Avatar {:icon (r/as-element [:> icons/UserOutlined])
                :style {:marginRight "8px" :backgroundColor "#1890ff"}}] ; Added background color
    ;; 地点标签 - 根据图片硬编码
    [:> Tag {:color "processing" :style {:marginRight "8px"}} "聊城市人民医院"]
    [:> Typography.Text {:style {:marginRight "8px"}} "张医生"]
    [:> icons/DownOutlined {:style {:color "rgba(0, 0, 0, 0.45)" :marginLeft "8px"}}] ; Added margin-left
    ;; 用户下拉菜单可以根据需要在此处条件渲染
    ]])

(defn right-side "患者麻醉管理\"patients\", 问卷列表\"assessment\", 系统设置\"settings\"" [active-tab]
  [:> Layout {:style {:height "calc(100vh - 64px)"
                      :overflow "hidden"}}
   (case (timbre/spy :info active-tab)
     "patients" [anesthesia-content]
     "assessment" [:div {:style {:padding "24px" :background "#fff" :margin "16px" :borderRadius "4px"}} "问卷列表内容"]
     "settings" [:f> system-settings-content]
     [:div {:style {:padding "24px" :background "#fff" :margin "16px" :borderRadius "4px"}} "未知标签页内容"])])

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])]
    [:> Layout {:style {:minHeight "100vh"}}
     [sider-bar active-tab]
     [:> Layout {:style {:display "flex" :flexDirection "column"}}
      [app-header]
      [:> Layout.Content {:style {:margin "0" :overflow "initial" :display "flex" :flex "1"}}
       [right-side active-tab]]]]))
