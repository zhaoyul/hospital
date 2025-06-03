(ns hc.hospital.ui-helpers
  (:require ["antd" :refer [Card]]
            ["@ant-design/icons" :as icons]
            [reagent.core :as r]))

(defn custom-styled-card "创建统一样式的卡片"
  [{:keys [icon title-text header-bg-color content on-double-click card-style card-body-style]}]
  [:> Card {:title (r/as-element [:span icon title-text])
            :styles {:header {:background header-bg-color}
                     :body (merge {:background "#ffffff"} card-body-style)}
            :type "inner"
            :style (merge {:marginBottom "12px"} card-style)
            :onDoubleClick on-double-click} ; Changed from :on-double-click to :onDoubleClick for Ant Design
   content])
