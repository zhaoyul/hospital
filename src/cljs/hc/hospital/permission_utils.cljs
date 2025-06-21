(ns hc.hospital.permission-utils
  "权限工具函数与组件，用于统一控制界面元素的展示与可用性。"
  (:require [re-frame.core :as rf]
            [hc.hospital.subs :as subs]))

(defn allowed?
  "判断当前角色是否拥有指定模块及操作的权限。"
  [module action]
  @(rf/subscribe [::subs/has-permission? module action]))

(defn with-permission
  "根据权限渲染组件。\n  module - 权限模块名\n  action - 操作名\n  content - 有权限时渲染的 hiccup 或函数\n  fallback - (可选) 无权限时渲染内容，默认为 nil"
  [module action content & [fallback]]
  (let [allow? @(rf/subscribe [::subs/has-permission? module action])]
    (if allow?
      (if (fn? content) (content) content)
      (when fallback
        (if (fn? fallback) (fallback) fallback))))
)
