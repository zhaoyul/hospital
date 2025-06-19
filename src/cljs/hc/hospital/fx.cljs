(ns hc.hospital.fx
  "注册自定义 Re-frame 副作用，如 Ant Design 的提示信息"
  (:require [re-frame.core :as rf]
            ["antd/es/message" :default antd-message]))

(rf/reg-fx
 ::alert
 (fn [{:keys [type message duration]
       :or {type :info duration 3}}]
   (case type
     :success (.success antd-message message duration)
     :error   (.error antd-message message duration)
     :warning (.warning antd-message message duration)
     (.info antd-message message duration))))

(rf/reg-event-fx
 ::show-alert
 (fn [_ [_ params]]
   {::alert params}))
