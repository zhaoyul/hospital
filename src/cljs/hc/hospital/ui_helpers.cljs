(ns hc.hospital.ui-helpers
  (:require ["antd" :refer [Card]]
            [reagent.core :as r]))

(defn custom-styled-card
  "创建统一样式的卡片，支持双击事件和自定义样式"
  [icon title-text header-bg-color content & {:keys [on-click card-style card-body-style view-state]}]
  [:> Card {:title (r/as-element [:div {:on-click on-click} [:span icon title-text]])
            :styles {:header {:background header-bg-color}
                     :body (merge {:background "#ffffff"} card-body-style)}
            :type "inner"
            :style (merge {:marginBottom "12px"} card-style)}
   content])
