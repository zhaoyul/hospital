(ns hc.hospital.pages.anesthesia-home
  "医生端首页布局，包含侧边栏及顶部导航。"
  (:require
   ["@ant-design/icons" :as icons]
   ["dayjs" :as dayjs]
   ["antd" :refer [Avatar Button DatePicker Input Layout Menu Modal Pagination Space Table Tag Typography]]
   [hc.hospital.events :as events]
   [hc.hospital.components.qr-scan-modal :refer [qr-scan-modal]]
   [hc.hospital.pages.anesthesia :refer [anesthesia-content]]
   [hc.hospital.pages.overview :refer [overview-content]]
   [hc.hospital.pages.comps :refer [custom-sider-trigger]]
   [hc.hospital.pages.settings :refer [system-settings-content]]
   [hc.hospital.pages.questionnaire :refer [questionnaire-list-content]]
   [hc.hospital.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as timbre]))


(def menu-definitions
  [{:key "1" :icon (r/as-element [:> icons/DashboardOutlined]) :label "纵览信息"  :module "纵览信息"  :tab "overview"}
   {:key "2" :icon (r/as-element [:> icons/ProfileOutlined])   :label "麻醉管理"  :module "麻醉管理"  :tab "patients"}
   {:key "3" :icon (r/as-element [:> icons/FileAddOutlined])   :label "问卷列表"  :module "问卷列表"  :tab "assessment"}
   {:key "4" :icon (r/as-element [:> icons/SettingOutlined])   :label "系统管理"  :module "系统管理"  :tab "settings"}])

(def key-by-tab (into {} (map (juxt :tab :key) menu-definitions)))
(def tab-by-key (into {} (map (juxt :key :tab) menu-definitions)))

(defn menu-items [allowed-modules]
  (let [allowed (set allowed-modules)]
    (->> menu-definitions
         (filter #(contains? allowed (:module %)))
         (map #(select-keys % [:key :icon :label]))
         vec)))

(defn sider-bar [active-tab]
  (let [sidebar-collapsed? (r/atom true)]
    (fn [active-tab]
      [:> Layout.Sider {:theme "light"
                        :collapsed @sidebar-collapsed?
                        :collapsedWidth 60
                        :width 160
                        :trigger nil
                        :collapsible true}
       [custom-sider-trigger sidebar-collapsed? #(swap! sidebar-collapsed? not)]
        (let [allowed-modules (map :module @(rf/subscribe [::subs/current-role-permissions]))]
          [:> Menu {:mode "inline"
                    :inlineCollapsed @sidebar-collapsed?
                    :selectedKeys [(get key-by-tab active-tab "1")]
                    :onClick (fn [item]
                               (when-let [tab (get tab-by-key (.-key item))]
                                 (rf/dispatch [::events/navigate-tab tab])))
                    :style {:height "100%" :borderRight 0}
                    :items (clj->js (menu-items allowed-modules))}])])))

;; 新增顶部导航栏组件
(defn app-header []
  (let [current-doctor @(rf/subscribe [::subs/current-doctor])
        is-logged-in? @(rf/subscribe [::subs/is-logged-in])]
    [:> Layout.Header
       {:style {:background "#fff"
                :padding "0 16px"
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
                    :style {:marginRight "8px" :backgroundColor "#1890ff"}}]
        ;; 地点标签 - 根据图片硬编码
        [:> Tag {:color "processing" :style {:marginRight "8px"}} "聊城市人民医院"]
        [:> Typography.Text {:style {:marginRight "8px"}} (or (:name current-doctor) "医生")]
        [:> icons/DownOutlined {:style {:color "rgba(0, 0, 0, 0.45)" :marginLeft "8px" :marginRight "16px"}}]
        [:> Button {:type "default"
                    :icon (r/as-element [:> icons/LogoutOutlined])
                    :on-click #(rf/dispatch [::events/handle-logout])}
         "退出登录"]
        ;; 用户下拉菜单可以根据需要在此处条件渲染
        ])
     ;; If not logged in, this space will be empty or you can add a Login button here later
     ]))

(defn right-side "患者麻醉管理\"patients\", 问卷列表\"assessment\", 系统设置\"settings\"" [active-tab]

  (let [allowed (set (map :module @(rf/subscribe [::subs/current-role-permissions])))]
    [:> Layout {:style {:height "calc(100vh - 64px)"
                        :overflow "auto"
                        :background "#f0f2f5"}}
     [:div {:style {:padding "24px"}}
      (case active-tab
        "overview" [overview-content]
        "patients" (if (contains? allowed "麻醉管理")
                      [anesthesia-content]
                      [:div "无权限"])
        "assessment" (if (contains? allowed "问卷列表")
                        [questionnaire-list-content]
                        [:div "无权限"])
        "settings" (if (contains? allowed "系统管理")
                      [:f> system-settings-content]
                      [:div "无权限"])
        [:div "未知标签页内容"])]]))


(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])
        qr-modal-visible? @(rf/subscribe [::subs/qr-scan-modal-visible?])]
    [:> Layout {:style {:minHeight "100vh"}}
     [sider-bar active-tab]
     [:> Layout {:style {:display "flex" :flexDirection "column"}}
      [app-header]
      [:> Layout.Content {:style {:margin "0" :overflow "initial" :display "flex" :flex "1"}}
       [right-side active-tab]]
      ;; 在页面底部渲染二维码扫描模态框
      (when qr-modal-visible?
        [:f> qr-scan-modal {:visible? qr-modal-visible?
                            :on-ok (fn [patient-id]
                                     ;; 点击确定时，派发事件以通过API查询HIS中的患者信息
                                     (rf/dispatch [::events/find-patient-by-id-in-his patient-id]))
                            :on-cancel #(rf/dispatch [::events/close-qr-scan-modal])}])]]))
