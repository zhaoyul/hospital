(ns hc.hospital.components.form-components
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            ["antd" :refer [Card Content Collapse Space Text Col CheckBox Radio Row DatePicker Tag  Descriptions Empty Button Input InputNumber Select Form Layout Tooltip Upload Switch]]
            [taoensso.timbre :as timbre]))

;; 创建一个"有无"选择组件，包含"无"和"有"的单选按钮，以及一个可选的描述输入框
(defn yes-no-with-description
  [{:keys [label field-name-prefix label-width]}]
  (let [switch-value* (r/atom false)]
    (fn []
      [:> Row {:gutter 8 :wrap false :align "middle"}
       [:> Col {:style {:whiteSpace "nowrap"
                        :width (or label-width "100px")
                        :textAlign "left"
                        :paddingRight "8px"}}
        [:span (str label ":")]]
       [:> Col
        [:> Form.Item {:name (keyword (str field-name-prefix "-switch"))
                       :valuePropName "checked"
                       :noStyle true
                       :style {:marginBottom 0}}
         [:> Switch {:onChange #(reset! switch-value* %)
                     :checkedChildren "有"
                     :unCheckedChildren "无"}]]]
       [:> Col {:flex "auto" :style {:marginLeft "8px"}}
        [:> Form.Item {:name (keyword (str field-name-prefix "-desc"))
                       :noStyle true}
         [:> Input {:placeholder "请输入"
                    :disabled (not @switch-value*)
                    :style {:width "100%"}}]]]])))

;; 创建一个复选框组件，带有可选的输入框
(defn checkbox-with-conditional-input
  [{:keys [label checkbox-label field-name input-width]}]
  [:> Form.Item {:label (str label ":")
                 :style {:marginBottom "0px"}}
   [:> Row {:gutter 8 :wrap false}
    [:> Col
     [:> Form.Item {:name (keyword (str field-name "-checkbox")) :valuePropName "checked" :noStyle true}
      [:> CheckBox checkbox-label]]]
    [:> Col {:flex "auto"}
     [:> Form.Item {:name field-name
                    :noStyle true
                    :dependencies [(keyword (str field-name "-checkbox"))]}
      ;; fixme
      #_(fn [form-instance]
          (let [checked? (.getFieldValue form-instance (keyword (str field-name "-checkbox")))]
            [:> Input {:placeholder "请输入"
                       :disabled (not checked?)
                       :style {:width (or input-width "100%")}}]))]]]])

;; 创建一个单选按钮组组件
(defn radio-button-group
  [{:keys [label name options button-style option-type]}]
  [:> Form.Item {:name name :label (str label ":")}
   [:> Radio.Group {:buttonStyle (or button-style "solid") :optionType (or option-type "button")}
    (for [{:keys [value label]} options]
      ^{:key value}
      [:> Radio {:value value} label])]])

;; 创建一个带单位的数字输入组件
(defn number-input-with-unit
  [{:keys [label name step unit]}]
  [:> Form.Item {:name name :label label}
   [:> Input {:type "number"
              :step (or step "1")
              :addonAfter unit
              :style {:width "200px"}}]])

;; 创建一个带标题的部分组件
(defn section-title [{:keys [title margin-top]}]
  [:div.section-title {:style {:fontWeight "bold"
                               :fontSize "16px"
                               :marginBottom "16px"
                               :marginTop (or margin-top "24px")}}
   title])

;; 创建一个双列输入行，常用于并排的表单项
(defn two-column-row [{:keys [left-item right-item]}]
  [:> Row {:gutter 16}
   [:> Col {:span 12} left-item]
   [:> Col {:span 12} right-item]])

;; 创建一个标准表单组件，处理表单组件的通用逻辑
(defn standard-form [{:keys [form-data subscription-key update-event form-content]}]
  (let [data @(rf/subscribe [subscription-key])
        [form] ((.-useForm (.-Form js/antd)))]
    [:> Form {:form form
                :layout "vertical"
                :initialValues data
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [update-event all-values]))
                :style {:paddingBottom "24px"}}
     form-content]))

;; 创建一个限制长度的文本输入组件
(defn limited-text-input
  [{:keys [label name max-length placeholder style addonAfter]}]
  [:> Form.Item {:name name :label (when label (str label ":"))}
   [:> Input {:placeholder (or placeholder "请输入")
              :maxLength (or max-length 100)
              :addonAfter addonAfter
              :style (merge {:width "100%"} style)}]])
