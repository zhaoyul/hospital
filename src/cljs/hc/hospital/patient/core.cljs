(ns hc.hospital.patient.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [re-frame.core :as rf]
   [hc.hospital.patient.events :as events]
   [hc.hospital.patient.views :as views]
   [taoensso.timbre :as timbre]))

;; 初始化 re-frame 应用程序数据库
(defn ^:dev/after-load mount-root []
  (timbre/debug "重新挂载患者应用组件")
  (let [root-el (.getElementById js/document "patient-app")]
    (d/unmount-component-at-node root-el)
    (d/render [views/patient-form] root-el)))

(defn init! []
  (timbre/debug "初始化患者应用")
  ;; 初始化 re-frame 应用状态
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))