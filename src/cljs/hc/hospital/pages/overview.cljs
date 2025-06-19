(ns hc.hospital.pages.overview
  "纵览信息模块，展示统计概览与图表。"
  (:require
   ["@ant-design/icons" :as icons]
   ["antd" :refer [Card DatePicker]]
   [hc.hospital.utils :as utils]
   ["echarts" :as echarts]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [hc.hospital.subs :as subs]
   [hc.hospital.events :as events]))

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

(defn- stat-card [{:keys [label value] :as item}]
  [:> Card {:style {:flex "1 0 200px" :marginBottom 16}}
   [:div {:style {:textAlign "center"}}
    [:div {:style {:fontSize 24 :color "#1890ff"}} value]
    [:div label]
    [:div {:style {:marginTop 4}}
     (trend-display item)]]])

(defn- chart-card [id title]
  [:> Card {:title title
            :style {:width 300 :marginBottom 16}}
   [:div {:id id :style {:height 300}}]])

(defn overview-content []
  (r/create-class
   {:component-did-mount
   (fn []
     ;; 数据来源分布 - 每日门诊与住院患者
     (init-chart "patientSourceChart"
                  {
                   :tooltip {:trigger "axis"}
                   :legend {:data ["门诊" "住院"]}
                   :xAxis {:type "category"
                           :data ["周一" "周二" "周三" "周四" "周五" "周六" "周日"]}
                   :yAxis {:type "value"}
                   :series [{:name "门诊" :type "bar" :data [20 30 15 25 40 50 45]}
                            {:name "住院" :type "bar" :data [10 20 25 30 35 40 30]}]})
      ;; ASA 评级分布
      (init-chart "asaChart"
                  {
                   :tooltip {:trigger "item"}
                   :series [{:type "pie"
                             :radius "50%"
                             :data [{:name "I级" :value 30}
                                    {:name "II级" :value 50}
                                    {:name "III级" :value 20}]
                             :emphasis {:itemStyle {:shadowBlur 10
                                                    :shadowOffsetX 0
                                                    :shadowColor "rgba(0,0,0,0.5)"}}}]})
      ;; 通过率
      (init-chart "approvalRateChart"
                  {
                   :tooltip {:trigger "item"}
                   :series [{:type "pie"
                             :radius ["40%" "70%"]
                             :label {:show false}
                             :emphasis {:label {:show true :fontSize 20 :fontWeight "bold"}}
                             :labelLine {:show false}
                             :data [{:name "通过" :value 80}
                                    {:name "未通过" :value 20}]}]})
      ;; 最近10天手术人数
      (init-chart "surgeryCountChart"
                  {
                   :tooltip {:trigger "axis"}
                   :xAxis {:type "category"
                           :data ["1日" "2日" "3日" "4日" "5日" "6日" "7日" "8日" "9日" "10日"]}
                   :yAxis {:type "value"}
                   :series [{:type "line"
                             :data [15 18 20 22 19 16 24 28 30 27]}]})
      ;; 性别分布
      (init-chart "genderChart"
                  {
                   :tooltip {:trigger "item"}
                   :series [{:type "pie"
                             :radius ["40%" "70%"]
                             :data [{:name "男" :value 60}
                                    {:name "女" :value 40}]}]})
      ;; 年龄分布
      (init-chart "ageChart"
                  {
                   :tooltip {:trigger "axis"}
                   :xAxis {:type "category" :data ["0-20" "21-40" "41-60" "61+"]}
                   :yAxis {:type "value"}
                   :series [{:type "bar" :data [5 20 30 15]}]})
      (rf/dispatch [::events/fetch-overview-stats]))
    :reagent-render
    (fn []
      (let [stats @(rf/subscribe [::subs/overview-stats])
            date @(rf/subscribe [::subs/overview-date])]
        [:div
         [:> Card {:title "数据概览"
                   :extra (r/as-element
                           [:> DatePicker {:value (utils/to-dayjs date)
                                           :onChange (fn [_ ds]
                                                       (rf/dispatch [::events/set-overview-date ds])
                                                       (rf/dispatch [::events/fetch-overview-stats ds]))}])
                   :style {:marginBottom 16}}
          [:div {:style {:display "flex" :flexWrap "wrap" :gap 16}}
           (for [item stats]
             ^{:key (:label item)}
             [stat-card item])]]
         [:> Card {:title "数据来源分布" :style {:marginBottom 16}}
          [:div {:style {:display "flex" :flexWrap "wrap" :gap 16}}
           [chart-card "patientSourceChart" "数据来源分布"]
           [chart-card "asaChart" "ASA评级分布"]
           [chart-card "approvalRateChart" "通过率"]]]
         [:> Card {:title "手术数据分析"}
          [:div {:style {:display "flex" :flexWrap "wrap" :gap 16}}
           [chart-card "surgeryCountChart" "最近10天手术人数"]
           [chart-card "genderChart" "性别分布"]
           [chart-card "ageChart" "年龄分布"]]]]))}))

