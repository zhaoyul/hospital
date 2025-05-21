(ns hc.hospital.core
  (:require
   ["react" :as react]
   [reagent.core :as r]
   [reagent.dom :as d]
   [re-frame.core :as rf]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [taoensso.timbre :as timbre]
   [hc.hospital.pages.anesthesia-home :refer [anesthesia-home-page]]
   [hc.hospital.pages.login :refer [login-page]] ; Added login page
   ["antd" :as antd :refer [Button]])) ; Import Button component from antd

;; Import Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; Views

(defn app-root []
  (let [is-logged-in? @(rf/subscribe [::subs/is-logged-in])
        current-path (.-pathname js/window.location)]
    (timbre/info (str "App root: Logged in? " is-logged-in? ", Current path: " current-path))

    (if is-logged-in?
      (do
        (when (or (= current-path "/login") (= current-path "/login.html"))
          (timbre/info "Logged in, but on login page. Redirecting to /")
          (js/window.location.assign "/"))
        ;; Fetch assessments only when logged in and viewing main app
        (react/useEffect
         #(rf/dispatch-sync [::events/fetch-all-assessments])
         #js [])
        [anesthesia-home-page]) ; Render main application page
      (if (or (= current-path "/login") (= current-path "/login.html"))
        [login-page] ; Render login page if already on login path
        (do
          (timbre/info (str "Not logged in and not on login path (" current-path "). Redirecting to /login"))
          (js/window.location.assign "/login") ; Redirect to login page
          ;; Render login page while redirecting or if redirect fails to prevent blank screen
          [login-page])))))


;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (timbre/info "重新挂载应用")
  (d/render [:f> app-root] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  ;; 初始化日志系统
  (timbre/info "初始化应用")
  (timbre/debug "DEBUG模式已启用")

  ;; 初始化 re-frame 应用状态
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
