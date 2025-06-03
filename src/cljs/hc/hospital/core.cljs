(ns hc.hospital.core
  "医院ClojureScript应用的主入口点。
  负责UI渲染、事件处理和应用初始化。"
  (:require
   ["react" :as react]
   [reagent.core :as r]
   [reagent.dom :as d]
   [re-frame.core :as rf]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [taoensso.timbre :as timbre]
   [hc.hospital.pages.anesthesia-home :refer [anesthesia-home-page]]
   ;; 已移除登录页面引用: [hc.hospital.pages.login :refer [login-page]]
   ["antd" :as antd :refer [Button Spin]])) ; 已添加 Spin 作为加载指示器

;; 导入 Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; 视图

(defn app-root
  "应用的主UI组件。根据用户登录状态和会话检查状态来渲染不同视图。"
  []
  (let [session-check-pending? @(rf/subscribe [::subs/session-check-pending?])
        is-logged-in? @(rf/subscribe [::subs/is-logged-in])
        current-path (.-pathname js/window.location)]
    (timbre/info (str "应用根组件：会话检查中？" session-check-pending? "，已登录？" is-logged-in? "，当前路径：" current-path))
    (react/useEffect
     #(rf/dispatch-sync [::events/fetch-all-assessments])
     #js [])

    (if session-check-pending?
      [:div {:style {:display "flex" :justifyContent "center" :alignItems "center" :height "100vh"}}
       [:> antd/Spin {:tip "正在加载会话..." :size "large"}
        [:div {:style {:width "100%" :height "100%"}}]]]
      (if is-logged-in?
        (do
          ;; 如果已登录，但当前路径为 /login 或 /login.html，则重定向到主应用页面。
          ;; 这是一个边缘情况，因为 /login 应该是后端渲染的页面。
          ;; 但是，如果用户手动导航或状态不一致，此逻辑会处理该情况。
          (when (or (= current-path "/login") (= current-path "/login.html"))
            (timbre/info "已登录，但位于登录页面。正在重定向到 /")
            (js/window.location.assign "/"))
          ;; 仅在登录并查看主应用时获取评估数据

          [anesthesia-home-page]) ; 渲染主应用页面
        ;; 如果未登录（且会话检查已完成）：
        ;; ::session-check-failed 事件应该已经启动了到 /login 的重定向。
        ;; 当浏览器导航时，app-root 的这部分会短暂渲染。
        ;; 此处无需渲染 [login-page]，因为 /login 路径由后端处理。
        [:div {:style {:textAlign "center" :paddingTop "50px"}}
         "正在重定向到登录页面..."]))))


;; -------------------------
;; 初始化应用

(defn ^:dev/after-load mount-root
  "重新挂载根React组件到DOM中。通常在开发环境代码热重载后调用。"
  []
  (timbre/info "重新挂载应用")
  (d/render [:f> app-root] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init!
  "应用初始化函数。设置日志，处理登录状态，并挂载根组件。"
  []
  (timbre/info "正在初始化应用...")
  (timbre/debug "调试模式已启用。")

  (if (identical? "true" (js/localStorage.getItem "justLoggedIn"))
    (do
      (timbre/info "登录后初始化：清除标记并派发 handle-just-logged-in 事件。")
      (js/localStorage.removeItem "justLoggedIn")
      (rf/dispatch [::events/handle-just-logged-in]))
    (do
      (timbre/info "正常应用初始化：派发 initialize-db 和 check-session 事件。")
      (rf/dispatch-sync [::events/initialize-db])
      (rf/dispatch [::events/check-session])))

  (mount-root))
