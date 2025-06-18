(ns hc.hospital.components.user-modal
  (:require
   ["@ant-design/icons" :as icons]
   ["antd" :refer [Button Form Input Modal Select Upload]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   ["react" :as react]
   [hc.hospital.events :as events]))

(defn user-modal
  [{:keys [visible? editing-user]}]
  (let [[form] (Form.useForm)]
    (react/useEffect
     (fn []
       (when visible?
         (if editing-user
           (.setFieldsValue form (clj->js (select-keys editing-user [:name :username :role])))
           (.resetFields form))
         nil))
     #js [visible? editing-user])
    [:> Modal {:title (if (:username editing-user) "编辑医生" "新增医生")
               :open visible?
               :okText "保存"
               :cancelText "取消"
               :onOk (fn [] (.submit form))
               :onCancel #(rf/dispatch [::events/close-user-modal])
               :destroyOnHidden true}
     [:> Form {:form form
               :layout "vertical"
               :name "doctor_form"
               :onFinish (fn [values]
                           (rf/dispatch [::events/save-user (js->clj values :keywordize-keys true)]))}
      [:> Form.Item {:name "name"
                     :label "姓名"
                     :rules #js [{:required true :message "请输入姓名!"}]}
       [:> Input {}]]
      [:> Form.Item {:name "username"
                     :label "账号"
                     :rules #js [{:required true :message "请输入账号!"}]}
       [:> Input {:disabled (boolean (:username editing-user))}]]
      [:> Form.Item {:name "role"
                     :label "角色"
                     :rules #js [{:required true :message "请选择角色!"}]}
       (let [roles @(rf/subscribe [::subs/roles])]
         [:> Select {:placeholder "选择角色"}
          (for [{:keys [id name]} roles]
            ^{:key id} [:> Select.Option {:value name} name])])]
      [:> Form.Item {:name "signature-file"
                     :label "电子签名"
                     :valuePropName "fileList"}
       [:> Upload {:name "signature"
                   :listType "picture-card"
                   :showUploadList false
                   :beforeUpload (fn [file]
                                   (let [reader (js/FileReader.)]
                                     (set! (.-onload reader)
                                           #(rf/dispatch [::events/update-editing-user-field :signature (.. % -target -result)]))
                                     (.readAsDataURL reader file))
                                   false)}
        (if (and editing-user (:signature editing-user) (not (object? (:signature editing-user))))
          [:img {:src (:signature editing-user) :alt "签名" :style {:width "100%"}}]
          [:div
           [:> icons/PlusOutlined]
           [:div {:style {:marginTop 8}} "上传"]])]]]]))
