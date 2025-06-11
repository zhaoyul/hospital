(ns hc.hospital.components.qr-scan-modal
  (:require
   ["antd" :refer [Modal Input Button]]
   [reagent.core :as r]
   [re-frame.core :as rf])) ; Added re-frame for potential future state management

;; 二维码扫描模态框组件
;; props:
;; - visible? (boolean): 控制模态框是否可见
;; - input-value (string): 输入框的当前值 (由 re-frame 控制)
;; - on-input-change (function): 输入框内容变化时的回调函数
;; - on-ok (function): 点击确定按钮时的回调函数
;; - on-cancel (function): 点击取消按钮或关闭模态框时的回调函数
(defn qr-scan-modal [{:keys [visible? input-value on-input-change on-ok on-cancel]}]
  [:> Modal {:title "扫码签到"
             :open visible?
             :onOk on-ok ; 直接使用传递过来的 on-ok 函数
             :onCancel on-cancel
             :okText "确定"
             :cancelText "取消"
             :destroyOnClose true} ; 关闭时销毁内部组件状态
   [:div {:style {:padding "20px"}} ; 增加一些内边距
    [:> Input {:placeholder "请输入患者ID"
               :value input-value ; 绑定到 props 传入的 input-value
               :on-change on-input-change}]]]) ; 调用 props 传入的 on-input-change
