(ns hc.hospital.pages.overview
  "纵览信息模块，展示统计概览与图表。"
  (:require
   ["antd" :refer [Card DatePicker Row Col]]
   ["echarts" :as echarts]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn ^:private init-chart [id option]
  (let [dom (.getElementById js/document id)
        inst (.init echarts dom)]
    (.setOption inst (clj->js option)))
  nil)

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
      [:div
       [:> Card {:title "今日数据概览" :style {:marginBottom 16}}
        [:> Row {:gutter 16}
         [:> Col {:span 6}
          [:div {:style {:textAlign "center"}}
           [:div {:style {:fontSize 24 :color "#1890ff"}} "156"]
           [:div "总就诊人数"]]]
         [:> Col {:span 6}
          [:div {:style {:textAlign "center"}}
           [:div {:style {:fontSize 24 :color "#1890ff"}} "42"]
           [:div "今日患者人数"]]]
         [:> Col {:span 6}
          [:div {:style {:textAlign "center"}}
           [:div {:style {:fontSize 24 :color "#1890ff"}} "18"]
           [:div "手术患者人数"]]]
         [:> Col {:span 6}
          [:div {:style {:textAlign "center"}}
           [:div {:style {:fontSize 24 :color "#1890ff"}} "15"]
           [:div "已签字人数"]]]]]
       [:> Card {:title "数据来源分布"}
        [:div {:id "patientSourceChart" :style {:height 300}}]
        [:div {:id "asaChart" :style {:height 300}}]
        [:div {:id "approvalRateChart" :style {:height 300}}]]])}))

