(ns hc.hospital.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [re-frame.core :as rf]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [taoensso.timbre :as timbre]
   [hc.hospital.pages.anesthesia-home :refer [anesthesia-home-page]]
   ["antd" :as antd :refer [Button]])) ; Import Button component from antd

;; Import Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; Views

(defn home-page []
  [anesthesia-home-page])

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (timbre/info "重新挂载应用")
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  ;; 初始化日志系统
  (timbre/info "初始化应用")
  (timbre/debug "DEBUG模式已启用")

  ;; 初始化 re-frame 应用状态
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
