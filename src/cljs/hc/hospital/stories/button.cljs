(ns hc.hospital.stories.button
  "Storybook 中演示的示例按钮组件"
  (:require [reagent.core :as r]
            ["antd" :refer [Button]]))

(defn button-view
  "简单的 Ant Design 按钮示例"
  [props]
  [:> Button props (:label props)])

(defn ^:export button
  "导出给 Storybook 使用的 React 组件"
  [props]
  (r/as-element [button-view (js->clj props :keywordize-keys true)]))
