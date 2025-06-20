(ns hc.hospital.components.layout-components
  (:require ["antd" :refer [Card Space]]))

(defn filter-panel
  "统一的筛选区域容器，使用 Antd Card 与 Space 布局。"
  [& children]
  [:> Card {:bodyStyle {:padding "16px"}
            :style {:marginBottom "16px"}}
   (into [:> Space {:direction "vertical" :size "middle" :style {:width "100%"}}]
         children)])
