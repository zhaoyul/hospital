(ns hc.hospital.pages.settings
  (:require
   ["@ant-design/icons" :as icons]
   ["react" :as React]
   ["antd" :refer [Button Form Input Modal Select Space Table Typography Upload Card]]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn system-settings-content []
  (let [users @(rf/subscribe [::subs/users])
        modal-open? @(rf/subscribe [::subs/user-modal-visible?])
        editing-user @(rf/subscribe [::subs/editing-user])
        [form] (Form.useForm)]

    (React/useEffect (fn [] (rf/dispatch [::events/initialize-users])))

    [:> Card
     [:> Typography.Title {:level 2
                           :style {:marginTop 0 :marginBottom "16px"
                                   :fontSize "18px" :fontWeight 500 :color "#333"}}
      "系统设置"]

     [:div {:style {:marginBottom "16px"}}
      [:> Button {:type "primary"
                  :icon (r/as-element [:> icons/PlusOutlined])
                  :on-click (fn []
                              (.resetFields form)
                              (rf/dispatch [::events/open-user-modal nil]))}
       "新增医生"]]

     [:> Table {:dataSource users
                :rowKey :id
                :style {:marginTop "16px"}
                :columns [{:title "姓名"
                           :dataIndex "name"
                           :key "name"
                           :align "center"}

                          {:title "账号"
                           :dataIndex "username"
                           :key "username"
                           :align "center"}

                          {:title "角色"
                           :dataIndex "role"
                           :key "role"
                           :align "center"}

                          {:title "电子签名"
                           :dataIndex "signature"
                           :key "signature"
                           :align "center"
                           :render (fn [signature _record]
                                     (cond
                                       (nil? signature) "无签名"
                                       (string? signature)
                                       (r/as-element [:img {:src signature
                                                            :alt "签名"
                                                            :style {:width "48px"
                                                                    :height "48px"
                                                                    :borderRadius "50%"
                                                                    :objectFit "cover"
                                                                    :border "1px solid #eee"}}])
                                       (:thumbUrl signature)
                                       (r/as-element [:img {:src (:thumbUrl signature)
                                                            :alt "签名"
                                                            :style {:width "48px"
                                                                    :height "48px"
                                                                    :borderRadius "50%"
                                                                    :objectFit "cover"
                                                                    :border "1px solid #eee"}}])
                                       :else "无签名"))}

                          {:title "操作"
                           :key "action"
                           :align "center"
                           :render (fn [_text record]
                                     (let [user (js->clj record :keywordize-keys true)]
                                       (r/as-element
                                        [:> Space {}
                                         [:> Button {:type "primary"
                                                     :ghost true
                                                     :size "small"
                                                     :icon (r/as-element [:> icons/EditOutlined])
                                                     :on-click (fn []
                                                                 (rf/dispatch [::events/open-user-modal user])
                                                                 (.setFieldsValue form (clj->js (select-keys user [:name :username :role]))))}
                                          "编辑"]
                                         [:> Button {:danger true
                                                     :ghost true
                                                     :size "small"
                                                     :icon (r/as-element [:> icons/DeleteOutlined])
                                                     :on-click #(rf/dispatch [::events/delete-user (:id user)])}
                                          "删除"]])))}]}]

     (when modal-open?
       [:> Modal {:title (if (:username editing-user) "编辑医生" "新增医生")
                  :open modal-open? ; Changed from visible to open
                  :okText "保存"
                  :cancelText "取消"
                  :onOk (fn [] (.submit form))
                  :onCancel #(rf/dispatch [::events/close-user-modal])
                  :destroyOnClose true}
        [:> Form {:form form
                  :layout "vertical"
                  :name "doctor_form"
                  :initialValues (clj->js (select-keys editing-user [:name :username :role]))
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
          [:> Select {:placeholder "选择角色"}
           [:> Select.Option {:value "麻醉医生"} "麻醉医生"]
           [:> Select.Option {:value "管理员"} "管理员"]
           [:> Select.Option {:value "主任"} "主任"]
           [:> Select.Option {:value "护士"} "护士"]
           [:> Select.Option {:value "统计"} "统计"]
           [:> Select.Option {:value "医务部统计"} "医务部统计"]
           [:> Select.Option {:value "护理管理员"} "护理管理员"]]]

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
              [:div {:style {:marginTop 8}} "上传"]])]]]])]))
