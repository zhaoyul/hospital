(ns hc.hospital.ui-helpers
  (:require ["antd" :refer [Card]]
            ["@ant-design/icons" :as icons]
            [reagent.core :as r]))

(defn custom-styled-card 
  "创建统一样式的卡片，支持双击事件和自定义样式"
  [icon title-text header-bg-color content & {:keys [on-double-click card-style card-body-style]}]
  [:> Card {:title (r/as-element [:span icon title-text])
            :styles {:header {:background header-bg-color}
                     :body (merge {:background "#ffffff"} card-body-style)} ; 合并自定义body样式
            :type "inner"
            :style (merge {:marginBottom "12px"} card-style) ; 合并自定义卡片样式
            :onDoubleClick on-double-click} ; 添加双击事件支持
   content])
