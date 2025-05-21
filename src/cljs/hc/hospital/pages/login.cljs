(ns hc.hospital.pages.login
  (:require
   ["antd" :refer [Form Input Button Alert Typography Card]]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]))

(defn login-page []
  (let [username (r/atom "")
        password (r/atom "")
        login-error @(rf/subscribe [::subs/login-error])
        is-logged-in? @(rf/subscribe [::subs/is-logged-in])]

    (when is-logged-in?
      ;; If already logged in (e.g. state persisted or re-hydrated), redirect to home
      ;; This is a secondary check; primary routing is in core.cljs
      (js/window.location.assign "/"))

    [:> Card {:title (r/as-element [:> Typography.Title {:level 3 :style {:textAlign "center"}} "用户登录"])
              :style {:width 400 :margin "100px auto" :padding "20px"}
              :bordered true}
     [:> Form {:layout "vertical"
               :onFinish #(rf/dispatch [::events/handle-login {:username @username :password @password}])}
      [:> Form.Item {:label "用户名"
                     :name "username"
                     :rules [{:required true :message "请输入用户名!"}]}
       [:> Input {:placeholder "请输入用户名"
                  :value @username
                  :onChange #(reset! username (-> % .-target .-value))}]]
      [:> Form.Item {:label "密码"
                     :name "password"
                     :rules [{:required true :message "请输入密码!"}]}
       [:> Input.Password {:placeholder "请输入密码"
                           :value @password
                           :onChange #(reset! password (-> % .-target .-value))}]]
      (when login-error
        [:> Form.Item
         [:> Alert {:message login-error :type "error" :showIcon true}]])
      [:> Form.Item
       [:> Button {:type "primary" :htmlType "submit" :style {:width "100%"}}
        "登录"]]]]))
