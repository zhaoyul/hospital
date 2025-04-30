(ns hc.hospital.components.form-components
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [hc.hospital.utils :refer [event-value]]
            [taoensso.timbre :as timbre]
            [hc.hospital.components.antd :as antd]))

;; 创建一个"有无"选择组件，包含"无"和"有"的单选按钮，以及一个可选的描述输入框
(defn yes-no-with-description
  [{:keys [label field-name-prefix label-width]}]
  (let [switch-value* (r/atom false)]
    (fn []
      [antd/row {:gutter 8 :wrap false :align "middle"}
       [antd/col {:style {:whiteSpace "nowrap"
                          :width (or label-width "100px")
                          :textAlign "left"
                          :paddingRight "8px"}}
        [:span (str label ":")]]
       [antd/col
        [antd/form-item {:name (keyword (str field-name-prefix "-switch"))
                         :valuePropName "checked"
                         :noStyle true
                         :style {:marginBottom 0}}
         [antd/switch {:onChange #(reset! switch-value* %)
                       :checkedChildren "有"
                       :unCheckedChildren "无"}]]]
       [antd/col {:flex "auto" :style {:marginLeft "8px"}}
        [antd/form-item {:name (keyword (str field-name-prefix "-desc"))
                         :noStyle true}
         [antd/input {:placeholder "请输入"
                      :disabled (not @switch-value*)
                      :style {:width "100%"}}]]]])))

;; 创建一个复选框组件，带有可选的输入框
(defn checkbox-with-conditional-input
  [{:keys [label checkbox-label field-name input-width]}]
  [antd/form-item {:label (str label ":")
                   :style {:marginBottom "0px"}}
   [antd/row {:gutter 8 :wrap false}
    [antd/col
     [antd/form-item {:name (keyword (str field-name "-checkbox")) :valuePropName "checked" :noStyle true}
      [antd/checkbox checkbox-label]]]
    [antd/col {:flex "auto"}
     [antd/form-item {:name field-name
                      :noStyle true
                      :dependencies [(keyword (str field-name "-checkbox"))]}
      (fn [form-instance]
        (let [checked? (.getFieldValue form-instance (keyword (str field-name "-checkbox")))]
          [antd/input {:placeholder "请输入"
                       :disabled (not checked?)
                       :style {:width (or input-width "100%")}}]))]]]])

;; 创建一个单选按钮组组件
(defn radio-button-group
  [{:keys [label name options button-style option-type]}]
  [antd/form-item {:name name :label (str label ":")}
   [antd/radio-group {:buttonStyle (or button-style "solid") :optionType (or option-type "button")}
    (for [{:keys [value label]} options]
      ^{:key value}
      [antd/radio {:value value} label])]])

;; 创建一个带单位的数字输入组件
(defn number-input-with-unit
  [{:keys [label name step unit]}]
  [antd/form-item {:name name :label label}
   [antd/input {:type "number"
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
  [antd/row {:gutter 16}
   [antd/col {:span 12} left-item]
   [antd/col {:span 12} right-item]])

;; 创建一个标准表单组件，处理表单组件的通用逻辑
(defn standard-form [{:keys [form-data subscription-key update-event form-content]}]
  (let [data @(rf/subscribe [subscription-key])
        [form] ((.-useForm (.-Form js/antd)))]
    [antd/form {:form form
                :layout "vertical"
                :initialValues data
                :onValuesChange (fn [_changed-values all-values]
                                  (rf/dispatch [update-event all-values]))
                :style {:paddingBottom "24px"}}
     form-content]))

;; 创建一个限制长度的文本输入组件
(defn limited-text-input
  [{:keys [label name max-length placeholder style addonAfter]}]
  [antd/form-item {:name name :label (when label (str label ":"))}
   [antd/input {:placeholder (or placeholder "请输入")
                :maxLength (or max-length 100)
                :addonAfter addonAfter
                :style (merge {:width "100%"} style)}]])
