(ns hc.hospital.pages.anesthesia-home
  (:require [hc.hospital.pages.anesthesia :refer [anesthesia-content]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [taoensso.timbre :as timbre]
            ["antd" :refer [Avatar Card Content Collapse Space Text Col CheckBox Radio Row DatePicker Tag Menu Descriptions Empty
                            Button Input InputNumber Select Form Layout Tooltip Upload Switch Typography]]
            [hc.hospital.pages.comps :refer [custom-sider-trigger]]
            ["@ant-design/icons" :as icons]))

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
                   :selectedKeys [(case active-tab "patients" "1" "assessment" "2" "history" "3" "1")]
                   :onClick #(rf/dispatch [::events/set-active-tab
                                           (condp = (.-key %)
                                             "1" "patients"
                                             "2" "assessment"
                                             "3" "history")])
                   :style {:height "100%" :borderRight 0}
                   :items menu-items}]])))

;; 新增顶部导航栏组件
(defn app-header []
  [:> Layout.Header
   {:style {:background "#fff"
            :padding "0 12px 12px"
            :display "flex"
            :alignItems "center"
            :justifyContent "space-between"
            :borderBottom "1px solid #f0f0f0"
            :height "64px"}}
   [:div {:style {:display "flex" :alignItems "center"}}
    [:> icons/ApartmentOutlined {:style {:fontSize "28px" :color "#1890ff" :marginRight "12px"}}]
    [:> Typography.Text {:style {:font-size "26px"}} "麻醉信息管理平台"]]

   ;; 右侧：用户信息
   [:div {:style {:display "flex" :alignItems "center" :cursor "pointer"}
          ;; :on-click #(rf/dispatch [::events/toggle-user-dropdown]) ; 稍后可以为用户下拉菜单添加事件
          }
    [:> Avatar {:icon (r/as-element [:> icons/UserOutlined])
                  :style {:marginRight "8px"}}]
    ;; 地点标签 - 根据图片硬编码
    [:> Tag {:color "processing" :style {:marginRight "8px"}} "聊城市人民医院"]
    [:> Typography.Text {:style {:marginRight "8px"}} "张医生"]
    [:> icons/DownOutlined {:style {:color "rgba(0, 0, 0, 0.45)"}}]
    ;; 用户下拉菜单可以根据需要在此处条件渲染
    ]])

(defn right-side [active-tab]
  [:> Layout {:style {:padding "12px 0 0"}}
   (case active-tab
     "patients" [anesthesia-content]
     ;; 可以为其他标签页添加内容
     [:div "其他激活标签页内容"])])

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])]
    [:> Layout {:style {:minHeight "100vh"}} ; 确保应用占满整个视窗高度
     [sider-bar active-tab] ; 左侧边栏
     [:> Layout {:style {:padding "0 12px 12px"}}
      [app-header]
      [:> Layout.Content {:style {:margin "0"}}
       [right-side active-tab]]]]))
