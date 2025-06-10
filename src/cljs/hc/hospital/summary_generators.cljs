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

(declare is-value-meaningful?)

(defn is-value-meaningful?
  "判断一个值，根据其类型和内容，是否算作有意义的、应在总结中显示。"
  [value field-schema]
  (let [actual-schema (when field-schema (m/deref field-schema))]
    (cond
      (nil? value) false

      (string? value)
      (if (str/blank? value)
        false
        (if (and actual-schema (= :enum (m/type actual-schema)))
          (let [kw-val (keyword value)
                enum-values (get-enum-values actual-schema)]
            (if (no-content-keywords kw-val)
              false
              (if (or (nil? enum-values) (enum-values kw-val))
                true
                (not (str/blank? value)))))
          true))

      (keyword? value) (not (no-content-keywords value))

      (vector? value) (some #(is-value-meaningful? % nil) value)

      (map? value)
      (cond
        (empty? value) false
        ;; Rule for :内容 being nil makes the map non-meaningful IFF other keys are also not meaningful
        (and (contains? value :内容) (let [content-val (:内容 value)] (or (nil? content-val) (= :nil content-val))))
        (let [other-keys (dissoc value :内容)]
          (if (empty? other-keys) false ;; Only :内容 nil was present
              (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) other-keys)))

        ;; Rule for :有无 being "无" or :无 makes the map non-meaningful IFF other keys are also not meaningful
        (contains? value :有无)
        (let [yuwu-val (:有无 value)]
          (if (or (= "无" yuwu-val) (= :无 yuwu-val))
            (let [other-keys (dissoc value :有无)]
              (if (empty? other-keys) false ;; Only :有无 "无" was present
                  (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) other-keys)))
            ;; :有无 is present and not "无", evaluate normally based on all keys
            (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) value)))

        ;; Default map handling: meaningful if any of its values are meaningful according to its own schema or direct check
        :else
        (if (and actual-schema (= :map (m/type actual-schema)))
          (let [entries (m/entries actual-schema)]
            (some (fn [[entry-key entry-schema-wrapper _props]]
                    (let [sub-val (get value entry-key)
                          sub-schema (m/schema entry-schema-wrapper)]
                      (is-value-meaningful? sub-val sub-schema)))
                  entries))
          ;; Fallback: actual-schema is not a map or is nil.
          (some (fn [[_map-key map-val]] (is-value-meaningful? map-val nil)) value))))

      (= true value) true
      (= false value) false
      :else true)))

(declare generate-summary-hiccup*)

(defn- generate-summary-hiccup*
  [data schema level]
  (let [schema-type (m/type schema)
        actual-schema (m/deref schema)
        base-font-size 14
        level-font-reduction 1
        current-label-font-size (max 12 (- (+ base-font-size 1) (* level level-font-reduction)))
        current-value-font-size (max 12 (- base-font-size (* level level-font-reduction)))]
    (cond
      (= :maybe schema-type)
      (when-not (nil? data)
        (generate-summary-hiccup* data (first (m/children actual-schema)) level))

      (not (or (= :map schema-type) (= :vector schema-type))) ;; 原子值处理
      (if (is-value-meaningful? data actual-schema)
        (let [formatted-value (cond
                                (vector? data)
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
                                            processed-val (if (and (= :心电图 key-in-data)
                                                                     (map? val-at-key)
                                                                     (contains? val-at-key :描述))
                                                            (let [description-val (:描述 val-at-key)
                                                                  description-schema (when entry-schema
                                                                                       (when-let [desc-entry (->> (m/entries entry-schema)
                                                                                                                  (filter #(= :描述 (first %)))
                                                                                                                  first)]
                                                                                         (m/schema (second desc-entry))))]
                                                              (generate-summary-hiccup* description-val description-schema (inc level)))
                                                            (generate-summary-hiccup* val-at-key entry-schema (inc level)))]
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
        (if (seq child-hiccups)
          (into [:<>] child-hiccups)
          nil))

      (= :vector schema-type)
      (if (is-value-meaningful? data actual-schema)
        (let [element-schema (first (m/children actual-schema))]
          (if (and element-schema (not= :map (m/type element-schema)))
            (let [items (->> data
                             (map #(generate-summary-hiccup* % element-schema level))
                             (filterv identity))]
              (when (seq items)
                [:ul {:style {:list-style-type "'• '" :padding-left "20px" :margin-top "2px" :margin-bottom "2px"}}
                 (for [item items] [:li {:key (str item) :style {:font-size (str current-value-font-size "px")}} item])]))
            (let [items (->> data
                             (map-indexed (fn [idx item-data]
                                            (let [item-hiccup (generate-summary-hiccup* item-data element-schema (inc level))]
                                              (when item-hiccup
                                                [:div {:key idx
                                                       :style {:border "1px solid #f0f0f0"
                                                               :padding "5px"
                                                               :margin-bottom "5px"
                                                               :border-radius "4px"}}
                                                 item-hiccup]))))
                             (filterv identity)
                             vec)]
              (when (seq items) (into [:<>] items)))))
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
