(ns hc.hospital.pages.overview
  "纵览信息模块，展示统计概览与图表。"
  (:require
   ["@ant-design/icons" :as icons]
   ["antd" :refer [Card DatePicker Row Col]]
   ["echarts" :as echarts]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hc.hospital.subs :as subs]))

(defn ^:private init-chart [id option]
  (let [dom (.getElementById js/document id)
        inst (.init echarts dom)]
    (.setOption inst (clj->js option)))
  nil)

(defn- trend-display [{:keys [trend description]}]
  (case trend
    :up [:span {:style {:color "#52c41a"}}
         (r/as-element [:> icons/ArrowUpOutlined])
         (str " " description)]
    :down [:span {:style {:color "#f5222d"}}
           (r/as-element [:> icons/ArrowDownOutlined])
           (str " " description)]
    :same [:span description]
    [:span description]))

(defn overview-content []
  (r/create-class
   {:component-did-mount
    (fn []
      (init-chart "patientSourceChart" {:title {:text "来源分布"}
                                        :tooltip {}
                                        :series [{:type "pie"
                                                  :data [{:name "自助" :value 40}
                                                         {:name "转诊" :value 60}]}]})
      (init-chart "asaChart" {:title {:text "ASA分级"}
                              :xAxis {:type "category"
                                      :data ["I" "II" "III"]}
                              :yAxis {:type "value"}
                              :series [{:type "bar"
                                        :data [20 30 10]}]})
      (init-chart "approvalRateChart" {:title {:text "通过率"}
                                       :xAxis {:type "category" :data ["今日"]}
                                       :yAxis {:type "value"}
                                       :series [{:type "bar" :data [80]}]}))
    :reagent-render
    (fn []
      (let [stats @(rf/subscribe [::subs/overview-stats])
            row1 (take 4 stats)
            row2 (drop 4 stats)]
        [:div
         [:> Card {:title "今日数据概览" :style {:marginBottom 16}}
          [:<> 
           [:> Row {:gutter 16}
            (for [{:keys [label value] :as item} row1]
              ^{:key label}
              [:> Col {:span 6}
               [:div {:style {:textAlign "center"}}
                [:div {:style {:fontSize 24 :color "#1890ff"}} value]
                [:div label]
                [:div {:style {:marginTop 4}}
                 (trend-display item)]]])]
           (when (seq row2)
             [:> Row {:gutter 16 :style {:marginTop 12}}
              (for [{:keys [label value] :as item} row2]
                ^{:key label}
                [:> Col {:span 8}
                 [:div {:style {:textAlign "center"}}
                  [:div {:style {:fontSize 24 :color "#1890ff"}} value]
                  [:div label]
                  [:div {:style {:marginTop 4}}
                   (trend-display item)]]])])]]
         [:> Card {:title "数据来源分布"}
          [:div {:id "patientSourceChart" :style {:height 300}}]
          [:div {:id "asaChart" :style {:height 300}}]
          [:div {:id "approvalRateChart" :style {:height 300}}]]]))})

