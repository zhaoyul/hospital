(ns hc.hospital.pages.comps
  (:require
   ["@ant-design/icons" :refer [LeftOutlined RightOutlined]]
   ["antd" :refer [Button]]
   [reagent.core :as r]))

(defn custom-sider-trigger [*collapsed? on-toggle]
  [:> Button
   {:type "primary"
    :shape "circle"
    :icon (if @*collapsed?
            (r/as-element [:> RightOutlined])
            (r/as-element [:> LeftOutlined]))
    :on-click on-toggle
    :style {:position "absolute"
            :top "72px"      ;; 根据你的设计调整垂直位置
            :right "-12px"   ;; 负值使其部分悬浮在Sider外部
            :z-index 1       ;; 确保在Sider之上
            :display "flex"
            :align-items "center"
            :justify-content "center"
            :font-size "12px" ; 调整图标大小
            :box-shadow "0 2px 8px rgba(0, 0, 0, 0.15)"
            :background-color "#fff" ; 白色背景
            :color "#1890ff"         ; Ant Design 主蓝色图标
            :border "none"}}])
