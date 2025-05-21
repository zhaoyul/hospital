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
   ["antd" :as antd :refer [Button Spin]])) ; Added Spin for loading indicator

;; Import Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; Views

(defn app-root []
  (let [session-check-pending? @(rf/subscribe [::subs/session-check-pending?])
        is-logged-in? @(rf/subscribe [::subs/is-logged-in])
        current-path (.-pathname js/window.location)]
    (timbre/info (str "App root: Session pending? " session-check-pending? ", Logged in? " is-logged-in? ", Current path: " current-path))

    (if session-check-pending?
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100vh"}}
       [:> antd/Spin {:tip "Loading session..." :size "large"}]]
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
            ;; This case should ideally be handled by the redirect in ::session-check-failed
            ;; or ::login-failure. If reached, it implies a state inconsistency or direct navigation
            ;; to a protected route without a session.
            (timbre/warn (str "Not logged in, not on login path (" current-path "). Session check should have redirected. Forcing /login."))
            (js/window.location.assign "/login.html") ; Ensure redirect to login.html
            ;; Render login page while redirecting
            [login-page]))))))


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
  (rf/dispatch [::events/check-session]) ; Dispatch session check
  (mount-root))
