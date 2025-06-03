(ns hc.hospital.ui-helpers
  (:require ["antd" :refer [Card]]
            ["@ant-design/icons" :as icons]
            [reagent.core :as r]))

(defn custom-styled-card "创建统一样式的卡片" [icon title-text header-bg-color content card-extra-props]
  (let [base-style {:marginBottom "12px"}
        ;; props from call site take precedence for style
        merged-style (merge base-style (:style card-extra-props))
        base-body-style {:background "#ffffff"}
        ;; props from call site take precedence for bodyStyle
        merged-body-style (merge base-body-style (:bodyStyle card-extra-props))]
    [:> Card (merge
              {:title (r/as-element [:span icon title-text])
               :styles {:header {:background header-bg-color}
                        :body merged-body-style} ; Use merged body style
               :type "inner"
               :style merged-style}
              ;; Pass other props like onDoubleClick, ensuring :style and :bodyStyle are not duplicated
              (dissoc card-extra-props :style :bodyStyle))
     content]))

(defn page-loading "页面加载中" []
  [:div.page-loading-container
   [:> Spin :size "large"]])

(defn section-title [title]
  [:div.section-title title])

(defn patient-info-bar [patient-info]
  (when patient-info
    [:div.patient-info-bar
     [:span (str "姓名：" (:name patient-info))]
     [:span (str "年龄：" (:age patient-info))]
     [:span (str "性别：" (if (= (:sex patient-info) 1) "男" "女"))]
     [:span (str "住院号：" (:hospital-number patient-info))]
     [:span (str "入院日期：" (:admission-date patient-info))]
     [:span (str "手术日期：" (:operation-date patient-info))]]))

(defn icon-text [{:keys [icon text class on-click]}]
  [:span {:class (str "icon-text " class)
          :on-click on-click}
   icon
   text])

(defn operations-dropdown [operations]
  [:> Dropdown {:overlay (r/as-element
                          [:> Menu (for [[idx {:keys [key title icon on-click]}] (map-indexed vector operations)]
                                     ^{:key idx}
                                     [:> Menu.Item {:key key :on-click on-click} icon title])])}
   [:a {:on-click (fn [e] (.e/preventDefault))}
    "操作" [:> icons/DownOutlined]]])
