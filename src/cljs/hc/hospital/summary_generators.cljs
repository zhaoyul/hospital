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
  (let [actual-schema (when field-schema (m/deref field-schema))]
    (cond
      (nil? value) false

      ;; String handling
      (string? value)
      (if (str/blank? value)
        false
        (if (and actual-schema (= :enum (m/type actual-schema)))
          (let [kw-val (keyword value)
                enum-values (get-enum-values actual-schema)]
            (if (no-content-keywords kw-val)
              false ; Explicitly excluded
              (if (or (nil? enum-values) (enum-values kw-val))
                true ; Valid enum member (or enum-values couldn't be determined, treat as meaningful)
                (not (str/blank? value))))) ; Not an enum member, treat as free text
          true)) ; Not an enum or no schema, meaningful if not blank

      (keyword? value) (not (no-content-keywords value))

      (vector? value)
      (if (empty? value)
        false
        ;; For vectors, we generally don't have a direct sub-schema for each element in the same way maps do.
        ;; We check if *any* item in the vector is meaningful.
        ;; If a more specific check is needed based on vector's element schema, this might need adjustment
        ;; or be handled by the caller providing the element schema.
        (some #(is-value-meaningful? % nil) value))

      (map? value)
      (if (empty? value)
        false
        (if (and actual-schema (= :map (m/type actual-schema)))
          ;; Check if any value in the map is meaningful according to its own schema
          (let [entries (m/entries actual-schema)]
            (some (fn [[entry-key entry-schema-wrapper _props]]
                    (let [sub-val (get value entry-key)
                          sub-schema (m/schema entry-schema-wrapper)]
                      (is-value-meaningful? sub-val sub-schema)))
                  entries))
          true)) ; No schema for map, consider meaningful if not empty and not all values are nil/blank (covered by prior checks)

      (= true value) true
      (= false value) false
      :else true)))

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
