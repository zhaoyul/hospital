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
   ;; Removed login page require: [hc.hospital.pages.login :refer [login-page]]
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
    (react/useEffect
     #(rf/dispatch-sync [::events/fetch-all-assessments])
     #js [])

    (if session-check-pending?
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100vh"}}
       [:div ; <--- This is the new wrapping div
        [:> antd/Spin {:tip "Loading session..." :size "large"}]]]
      (if is-logged-in?
        (do
          ;; If logged in, but somehow on /login or /login.html, redirect to main app page.
          ;; This is an edge case, as /login should be a backend-rendered page.
          ;; However, if the user manually navigates or state gets inconsistent, this handles it.
          (when (or (= current-path "/login") (= current-path "/login.html"))
            (timbre/info "Logged in, but on login page. Redirecting to /")
            (js/window.location.assign "/"))
          ;; Fetch assessments only when logged in and viewing main app

          [anesthesia-home-page]) ; Render main application page
        ;; If not logged in (and session check is complete):
        ;; The ::session-check-failed event should have already initiated a redirect to /login.
        ;; This part of app-root will briefly render while the browser navigates.
        ;; No need to render [login-page] here as the /login path is handled by the backend.
        [:div {:style {:textAlign "center" :paddingTop "50px"}}
         "Redirecting to login..."]))))


;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (timbre/info "重新挂载应用")
  (d/render [:f> app-root] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  ;; 初始化日志系统
  (timbre/info "Initializing application...")
  (timbre/debug "DEBUG mode enabled.")

  (if (identical? "true" (js/localStorage.getItem "justLoggedIn"))
    (do
      (timbre/info "Initializing after login: clearing flag and dispatching handle-just-logged-in.")
      (js/localStorage.removeItem "justLoggedIn")
      (rf/dispatch [::events/handle-just-logged-in]))
    (do
      (timbre/info "Normal application initialization: dispatching initialize-db and check-session.")
      (rf/dispatch-sync [::events/initialize-db])
      (rf/dispatch [::events/check-session])))

  (mount-root))
