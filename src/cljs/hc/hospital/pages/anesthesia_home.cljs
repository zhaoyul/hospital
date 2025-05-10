(ns hc.hospital.pages.anesthesia-home
  (:require [hc.hospital.pages.anesthesia :refer [anesthesia-content]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.events :as events]
            [hc.hospital.subs :as subs]
            [hc.hospital.components.antd :as antd]
            [taoensso.timbre :as timbre]
            [hc.hospital.components.form-components :as form-comp]
            [hc.hospital.pages.comps :refer [custom-sider-trigger]]))

(def menu-items
  [{:key "1" :icon (r/as-element [antd/laptop-outlined]) :label "麻醉管理"}
   {:key "2" :icon (r/as-element [antd/user-outlined]) :label "患者文书"}
   {:key "3" :icon (r/as-element [antd/notification-outlined]) :label "患者签到"}])


(defn sider-bar [active-tab]
  (let [sidebar-collapsed? (r/atom true)]
    (fn []
      [antd/sider {:sidebar-collapsed? true
                   :theme "light"
                   :collapsed @sidebar-collapsed?
                   :trigger nil
                   :collapsible true}
       [custom-sider-trigger sidebar-collapsed? #(swap! sidebar-collapsed? not)]
       [antd/menu {:mode "inline"
                   :inlineCollapsed @sidebar-collapsed?
                   :selectedKeys [(case active-tab "patients" "1" "assessment" "2" "history" "3" "1")]
                   :onClick #(rf/dispatch [::events/set-active-tab
                                           (timbre/spy :info
                                                       (condp = (.-key %)
                                                         "1" "patients"
                                                         "2" "assessment"
                                                         "3" "history"))])
                   :style {:height "100%" :borderRight 0}
                   :items menu-items}]])))

(defn right-side [active-tab]
  (case active-tab
    "patients" [anesthesia-content]))

(defn anesthesia-home-page []
  (let [active-tab @(rf/subscribe [::subs/active-tab])
        _selected-forms-data @(rf/subscribe [::subs/selected-patient-assessment-forms-data])]
    [antd/layout
     [sider-bar active-tab]
     [antd/layout {:style {:padding "0 12px 12px"}}
      [right-side active-tab]]]))
