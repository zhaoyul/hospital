(ns hc.hospital.summary-generators
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.util :as mu]
            [clojure.set :as set]
            [hc.hospital.specs.assessment-complete-cn-spec :as cn-specs]))

;; 辅助函数，用于从 spec 中获取 Enum 定义的关键字集合
;; 注意：这假设 Enum schema 的子项直接是关键字值
(defn- get-enum-values [enum-schema]
  (when (and enum-schema (= :enum (m/type enum-schema)))
    (set (m/children enum-schema))))

(def ^:private no-content-keywords #{:无 :否 :不详})

(defn schema-key->label [k]
  (when k
    (-> (name k)
        (str/replace #"-" " ")
        (str/capitalize))))

(declare generate-summary-hiccup*)

(defn is-value-meaningful?
  "判断一个值，根据其类型和内容，是否算作有意义的、应在总结中显示。"
  [value field-schema]
  (cond
    (nil? value) false
    (and (string? value) (str/blank? value)) false
    (keyword? value) (not (no-content-keywords value))
    (vector? value) (if (empty? value)
                      false
                      (some #(is-value-meaningful? % nil) value))
    (map? value) (if (empty? value)
                   false
                   true)
    (= true value) true
    (= false value) false
    :else true))

(defn- generate-summary-hiccup*
  [data schema level]
  (let [schema-type (m/type schema)
        actual-schema (m/deref schema) ; Dereference for :maybe, :enum etc.
        base-font-size 14 ; 基准字体大小 for content
        level-font-reduction 1 ; 每层级减小的字号
        current-label-font-size (max 12 (- (+ base-font-size 1) (* level level-font-reduction))) ; 标签稍大，最小12px
        current-value-font-size (max 12 (- base-font-size (* level level-font-reduction)))      ; 内容字体，最小12px
        ]
    (cond
      (= :maybe schema-type)
      (when-not (nil? data)
        (generate-summary-hiccup* data (first (m/children actual-schema)) level))

      (not (or (= :map schema-type) (= :vector schema-type))) ;; 原子值处理
      (if (is-value-meaningful? data actual-schema) ; Use actual-schema for enum checks
        (let [formatted-value (cond
                                (vector? data) ;; Should ideally be handled by :vector case, but as fallback for simple lists
                                (let [meaningful-items (filterv #(is-value-meaningful? % nil) data)]
                                  (when (seq meaningful-items)
                                    (str/join ", " (map #(if (keyword? %) (name %) (str %)) meaningful-items))))
                                (keyword? data) (name data)
                                (boolean? data) (if data "是" "否")
                                :else (str data))]
          formatted-value)
        nil)

      (= :map schema-type)
      (let [entries (m/entries actual-schema)
            child-hiccups (->> entries
                               (mapv (fn [[key-in-data entry-schema-wrapper _props]]
                                      (let [entry-schema (m/schema entry-schema-wrapper)
                                            val-at-key (get data key-in-data)
                                            label (schema-key->label key-in-data)
                                            processed-val (generate-summary-hiccup* val-at-key entry-schema (inc level))]
                                        (when processed-val
                                          [:div {:key (str (name key-in-data) "-" level) :style {:margin-bottom "4px"}}
                                           [:span {:style {:font-weight "bold"
                                                          :font-size (str current-label-font-size "px")
                                                          :margin-right "5px"}}
                                            label ":"]
                                           (if (and (vector? processed-val) (some #(or (vector? %) (map? %)) processed-val) (not (every? string? processed-val)))
                                             [:div {:style {:margin-left "20px"
                                                            :padding-left "10px"
                                                            :border-left "1px solid #eee"}}
                                              processed-val]
                                             [:span {:style {:font-size (str current-value-font-size "px")}}
                                              processed-val])]))))
                               (filterv identity))]
        (when (seq child-hiccups)
          (into [:<>] child-hiccups))) ;; <--- 修改点

      (= :vector schema-type)
      (if (is-value-meaningful? data actual-schema)
        (let [element-schema (first (m/children actual-schema))]
          (if (and element-schema (not= :map (m/type element-schema)))
            (let [items (->> data
                             (map #(generate-summary-hiccup* % element-schema level)) ; level does not increase for simple list items
                             (filterv identity))]
              (when (seq items)
                [:ul {:style {:list-style-type "'• '" :padding-left "20px" :margin-top "2px" :margin-bottom "2px"}}
                 (for [item items] [:li {:key (str item) :style {:font-size (str current-value-font-size "px")}} item])]))
            ;; Vector of maps
            (let [items (->> data
                             (map-indexed (fn [idx item-data]
                                            (let [item-hiccup (generate-summary-hiccup* item-data element-schema (inc level))] ; level increases for map items
                                              (when item-hiccup
                                                [:div {:key idx
                                                       :style {:border "1px solid #f0f0f0"
                                                               :padding "5px"
                                                               :margin-bottom "5px"
                                                               :border-radius "4px"}}
                                                 item-hiccup]))))
                             (filterv identity)
                             vec)]
              (when (seq items) (into [:<>] items))) ;; <--- 修改点
            ))
        nil)
      :else nil)))

(defn generate-summary-hiccup
  "公共入口函数，生成总结的Hiccup结构。"
  [data schema system-name]
  (when-let [content (generate-summary-hiccup* data schema 0)] ;; 初始 level 设为 0
    (when (or (not (vector? content)) (seq content))
      [:div {:class (str "summary-section summary-" (name system-name))
             :style {:padding "10px" :border "1px solid #ddd" :margin-bottom "10px" :border-radius "4px"}}
       [:h3 {:style {:font-size "18px"
                     :font-weight "bold"
                     :color "#333"
                     :margin-top "0"
                     :margin-bottom "10px"
                     :padding-bottom "5px"
                     :border-bottom "1px solid #eee"}}
        (schema-key->label system-name)]
       [:div {:style {:margin-left "5px"}}
        content]])))
