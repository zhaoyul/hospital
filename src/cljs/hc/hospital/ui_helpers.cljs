(ns hc.hospital.ui-helpers
  (:require ["antd" :refer [Card]]
            ["@ant-design/icons" :as icons]
            [reagent.core :as r]))

(defn custom-styled-card "创建统一样式的卡片" [icon title-text header-bg-color content]
  [:> Card {:title (r/as-element [:span icon title-text])
            :styles {:header {:background header-bg-color}
                     :body {:background "#ffffff"}} ; 确保内容区域背景为白色
            :type "inner"
            :style {:marginBottom "12px"}}
   content])
