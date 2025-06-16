(ns hc.hospital.components.qr-scan-modal
  (:require
   ["antd" :refer [Modal Input Form]]))

;; 二维码扫描模态框组件
;; props:
;; - visible? (boolean): 控制模态框是否可见
;; - on-ok (function): 点击确定按钮时的回调函数，接收输入的患者ID
;; - on-cancel (function): 点击取消按钮或关闭模态框时的回调函数
(defn qr-scan-modal [{:keys [visible? on-ok on-cancel]}]
  (let [[form] (Form.useForm)]
    [:> Modal {:title "扫码签到"
               :open visible?
               :onOk (fn [] (.submit form))
               :onCancel on-cancel
               :okText "确定"
               :cancelText "取消"
               :destroyOnHidden true}
     [:> Form {:form form
               :layout "vertical"
               :onFinish (fn [values]
                           (on-ok (aget values "patient-id")))}
      [:> Form.Item {:name "patient-id"}
       [:> Input {:placeholder "请输入患者ID"
                  :autoFocus true
                  :onPressEnter (fn [_] (.submit form))}]]]]))
