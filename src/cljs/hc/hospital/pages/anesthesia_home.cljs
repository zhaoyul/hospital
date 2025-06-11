(ns hc.hospital.pages.anesthesia-home
  (:require
   ["@ant-design/icons" :as icons]
   ["dayjs" :as dayjs]
   ["antd" :refer [Avatar Button DatePicker Input Layout Menu Modal Pagination Space Table Tag Typography]] ; Added Modal, Table, Space, Button, DatePicker, Input, Pagination
   [hc.hospital.events :as events]
   [hc.hospital.components.qr-scan-modal :refer [qr-scan-modal]] ; 引入二维码扫描模态框组件
   [hc.hospital.pages.anesthesia :refer [anesthesia-content]]
   [hc.hospital.pages.comps :refer [custom-sider-trigger]]
   [hc.hospital.pages.settings :refer [system-settings-content]]
   [hc.hospital.pages.questionnaire :refer [questionnaire-list-content]]
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
    (fn [active-tab]
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
  (let [current-doctor @(rf/subscribe [::subs/current-doctor])
        is-logged-in? @(rf/subscribe [::subs/is-logged-in])]
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
     (when is-logged-in?
       [:div {:style {:display "flex" :alignItems "center" :cursor "pointer"}
              ;; :on-click #(rf/dispatch [::events/toggle-user-dropdown]) ; 稍后可以为用户下拉菜单添加事件
              }
        [:> Avatar {:icon (r/as-element [:> icons/UserOutlined])
                    :style {:marginRight "8px" :backgroundColor "#1890ff"}}] ; Added background color
        ;; 地点标签 - 根据图片硬编码
        [:> Tag {:color "processing" :style {:marginRight "8px"}} "聊城市人民医院"]
        [:> Typography.Text {:style {:marginRight "8px"}} (or (:name current-doctor) "医生")]
        [:> icons/DownOutlined {:style {:color "rgba(0, 0, 0, 0.45)" :marginLeft "8px" :marginRight "16px"}}] ; Added margin-left and right for spacing
        ;; 新增扫码签到按钮
        [:> Button {:type "primary" ; 使用 primary 类型使其更突出
                    :icon (r/as-element [:> icons/QrcodeOutlined]) ; 二维码图标
                    :style {:marginRight "16px"} ; 与退出登录按钮保持间距
                    :on-click #(rf/dispatch [::events/open-qr-scan-modal])}
         "扫码签到"]
        [:> Button {:type "default"
                    :icon (r/as-element [:> icons/LogoutOutlined])
                    :on-click #(rf/dispatch [::events/handle-logout])}
         "退出登录"]
        ;; 用户下拉菜单可以根据需要在此处条件渲染
        ])
     ;; If not logged in, this space will be empty or you can add a Login button here later
     ]))

(defn right-side "患者麻醉管理\"patients\", 问卷列表\"assessment\", 系统设置\"settings\"" [active-tab]
  [:> Layout {:style {:height "calc(100vh - 64px)"
                      :overflow "auto" ;; Changed from hidden to auto
                      :background "#f0f2f5"}} ;; Added background color to match prototype
   [:div {:style {:padding "24px"}} ;; Added common styling wrapper and height adjustment
    (case active-tab
      "patients" [anesthesia-content]
      "assessment" [questionnaire-list-content] ;; Use the new component
      "settings" [:f> system-settings-content]
      [:div "未知标签页内容"])]]) ; Removed inline style from default case as it's handled by the wrapper

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])
        qr-modal-visible? @(rf/subscribe [::subs/qr-scan-modal-visible?])
        qr-input-value @(rf/subscribe [::subs/qr-scan-input-value])]
    [:> Layout {:style {:minHeight "100vh"}}
     [sider-bar active-tab]
     [:> Layout {:style {:display "flex" :flexDirection "column"}}
      [app-header]
      [:> Layout.Content {:style {:margin "0" :overflow "initial" :display "flex" :flex "1"}}
       [right-side active-tab]]
      ;; 在页面底部渲染二维码扫描模态框
      (when qr-modal-visible?
        [qr-scan-modal {:visible? qr-modal-visible?
                        :input-value qr-input-value
                        :on-input-change (fn [e] (rf/dispatch [::events/set-qr-scan-input (-> e .-target .-value)]))
                        :on-ok (fn []
                                 ;; 点击确定时，派发事件以通过API查询HIS中的患者信息
                                 (rf/dispatch [::events/find-patient-by-id-in-his qr-input-value]))
                        :on-cancel #(rf/dispatch [::events/close-qr-scan-modal])}])]]))
