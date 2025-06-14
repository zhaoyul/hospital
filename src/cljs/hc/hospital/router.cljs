(ns hc.hospital.router
  "基于 Reitit 的前端路由管理。"
  (:require [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [re-frame.core :as rf-core]
            [taoensso.timbre :as timbre]))

(def routes
  "路由表，定义页面标签与路径的映射。"
  [["/" {:name :patients}]
   ["/assessment" {:name :assessment}]
   ["/settings" {:name :settings}]])

(def router
  (rf/router routes))

(defn- on-navigate
  [match]
  (when match
    (let [tab (name (get-in match [:data :name]))]
      (timbre/debug "路由切换:" tab)
      (rf-core/dispatch [:hc.hospital.events/set-active-tab tab]))))

(defn init-router!
  "初始化路由并开始监听浏览器历史变化。"
  []
  (rfe/start! router on-navigate {:use-fragment false}))

(def tab->route
  {"patients" :patients
   "assessment" :assessment
   "settings" :settings})

(defn navigate!
  "根据标签名导航到相应路径。"
  [tab]
  (if-let [route (get tab->route tab)]
    (rfe/push-state route)
    (timbre/warn "未知标签" tab)))
