(ns hc.hospital.summary-generators
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.util :as mu]
            [clojure.set :as set]
            [hc.hospital.specs.assessment-complete-cn-spec :as cn-specs]
            [taoensso.timbre :as timbre]))

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
  (timbre/debug "is-value-meaningful? called with value:" (pr-str value) "schema type:" (when field-schema (m/type field-schema)))
  (let [actual-schema (when field-schema (m/deref field-schema))
        result (cond
                 (nil? value)
                 (do (timbre/debug "is-value-meaningful? nil value -> false") false)

                 (string? value)
                 (let [res (if (str/blank? value)
                             false
                             (if (and actual-schema (= :enum (m/type actual-schema)))
                               (let [kw-val (keyword value)
                                     enum-values (get-enum-values actual-schema)]
                                 (if (no-content-keywords kw-val)
                                   false
                                   (if (or (nil? enum-values) (enum-values kw-val))
                                     true
                                     (not (str/blank? value)))))
                               true))]
                   (timbre/debug "is-value-meaningful? string" (pr-str value) "blank?" (str/blank? value) "schema-is-enum?" (and actual-schema (= :enum (m/type actual-schema))) "-> result:" res)
                   res)

                 (keyword? value)
                 (let [res (not (no-content-keywords value))]
                   (timbre/debug "is-value-meaningful? keyword" (pr-str value) "no-content?" (no-content-keywords value) "-> result:" res)
                   res)

                 (vector? value)
                 (let [res (some #(is-value-meaningful? % nil) value)]
                   (timbre/debug "is-value-meaningful? vector, item meaningful?" res "-> result:" res)
                   res)

                 (map? value)
                 (cond
                   (empty? value)
                   (do (timbre/debug "is-value-meaningful? map: empty? true -> false") false)

                   (and (contains? value :内容) (let [content-val (:内容 value)] (or (nil? content-val) (= :nil content-val))))
                   (let [other-keys (dissoc value :内容)]
                     (timbre/debug "is-value-meaningful? map: :内容 is nil/empty, other keys count:" (count other-keys) "other keys:" (pr-str other-keys))
                     (if (empty? other-keys)
                       (do (timbre/debug "is-value-meaningful? map: :内容 nil, no other keys -> false") false)
                       (let [res (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) other-keys)]
                         (timbre/debug "is-value-meaningful? map: :内容 nil, other keys meaningful?" res "-> result:" res)
                         res)))

                   (contains? value :有无)
                   (let [yuwu-val (:有无 value)]
                     (timbre/debug "is-value-meaningful? map: contains :有无, value:" (pr-str yuwu-val))
                     (if (or (= "无" yuwu-val) (= :无 yuwu-val))
                       (let [other-keys (dissoc value :有无)]
                         (timbre/debug "is-value-meaningful? map: :有无 is '无', other keys count:" (count other-keys) "other keys:" (pr-str other-keys))
                         (if (empty? other-keys)
                           (do (timbre/debug "is-value-meaningful? map: :有无 '无', no other keys -> false") false)
                           (let [res (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) other-keys)]
                             (timbre/debug "is-value-meaningful? map: :有无 '无', other keys meaningful?" res "-> result:" res)
                             res)))
                       (do
                         (timbre/debug "is-value-meaningful? map: :有无 is not '无', evaluating all keys for meaningfulness.")
                         (let [res (some (fn [[k v]] (is-value-meaningful? v (when actual-schema (get (m/entries actual-schema) k)))) value)]
                            (timbre/debug "is-value-meaningful? map: :有无 not '无', all keys meaningful?" res "-> result:" res)
                            res))))

                   :else
                   (let [default-map-meaningful (if (and actual-schema (= :map (m/type actual-schema)))
                                                  (let [entries (m/entries actual-schema)]
                                                    (some (fn [[entry-key entry-schema-wrapper _props]]
                                                            (let [sub-val (get value entry-key)
                                                                  sub-schema (m/schema entry-schema-wrapper)
                                                                  meaningful (is-value-meaningful? sub-val sub-schema)]
                                                              (timbre/debug "is-value-meaningful? map (:else with schema) sub-key:" entry-key "val:" (pr-str sub-val) "meaningful?" meaningful)
                                                              meaningful))
                                                          entries))
                                                  (some (fn [[mk mv]]
                                                          (let [meaningful (is-value-meaningful? mv nil)]
                                                            (timbre/debug "is-value-meaningful? map (:else no schema) sub-key:" mk "val:" (pr-str mv) "meaningful?" meaningful)
                                                            meaningful))
                                                        value))]
                     (timbre/debug "is-value-meaningful? map (:else branch) default result:" default-map-meaningful)
                     default-map-meaningful))

                 (= true value) (do (timbre/debug "is-value-meaningful? boolean true -> true") true)
                 (= false value) (do (timbre/debug "is-value-meaningful? boolean false -> false") false)
                 :else (do (timbre/debug "is-value-meaningful? default to true for value:" (pr-str value)) true))
        ]
    (timbre/info "is-value-meaningful? for value:" (pr-str value) " FINAL RESULT:" result)
    result))

(declare generate-summary-hiccup*)

(defn- generate-summary-hiccup*
  [data schema level]
  (let [indent-str (apply str (repeat level "  "))]
    (timbre/debug indent-str "[generate-summary-hiccup* level " level "] data:" (pr-str data) "schema type:" (m/type schema) "schema:" (pr-str schema))
    (let [schema-type (m/type schema)
          actual-schema (m/deref schema)
          base-font-size 14
          level-font-reduction 1
          current-label-font-size (max 12 (- (+ base-font-size 1) (* level level-font-reduction)))
          current-value-font-size (max 12 (- base-font-size (* level level-font-reduction)))
          result (cond
                   (= :maybe schema-type)
                   (when-not (nil? data)
                     (timbre/debug indent-str "  :maybe type, data present, processing child:" (first (m/children actual-schema)))
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
                       (timbre/debug indent-str "  atomic value (meaningful):" (pr-str formatted-value))
                       formatted-value)
                     (do (timbre/debug indent-str "  atomic value (not meaningful):" (pr-str data)) nil))

                   (= :map schema-type)
                   (let [entries (if actual-schema (m/entries actual-schema) [])
                         child-hiccups (->> entries
                                            (mapv (fn [[key-in-data entry-schema-wrapper _props]]
                                                   (let [val-at-key (get data key-in-data)]
                                                     (timbre/debug indent-str "  map key:" key-in-data "val:" (pr-str val-at-key))
                                                     (let [entry-schema (m/schema entry-schema-wrapper)
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
                                                                             (timbre/debug indent-str "    special handling for :心电图, description val:" (pr-str description-val))
                                                                             (generate-summary-hiccup* description-val description-schema (inc level)))
                                                                           (generate-summary-hiccup* val-at-key entry-schema (inc level)))]
                                                       (timbre/debug indent-str "    processed-val for " key-in-data ":" (pr-str processed-val))
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
                                                             processed-val])])))))
                                            (filterv identity))]
                     (if (seq child-hiccups)
                       (do (timbre/debug indent-str "  map result (hiccup seq):" (count child-hiccups) " items")
                           (into [:<>] child-hiccups))
                       (do (timbre/debug indent-str "  map result: nil (no meaningful children)")
                           nil)))

                   (= :vector schema-type)
                   (if (is-value-meaningful? data actual-schema) ; Vector itself is meaningful (e.g. not empty)
                     (let [element-schema (first (m/children actual-schema))]
                       (timbre/debug indent-str "  vector is meaningful. Element schema:" (pr-str element-schema))
                       (if (and element-schema (not= :map (m/type element-schema))) ; Vector of non-maps
                         (let [items (->> data
                                          (mapv (fn [item-data]
                                                 (timbre/debug indent-str "    vector item (non-map):" (pr-str item-data))
                                                 (generate-summary-hiccup* item-data element-schema level))) ; level does not increase for simple list items
                                          (filterv identity))]
                           (if (seq items)
                             (do (timbre/debug indent-str "    vector (non-map) result (items):" (count items))
                                 [:ul {:style {:list-style-type "'• '" :padding-left "20px" :margin-top "2px" :margin-bottom "2px"}}
                                  (for [item items] [:li {:key (str item) :style {:font-size (str current-value-font-size "px")}} item])])
                             (do (timbre/debug indent-str "    vector (non-map) result: nil (no meaningful items)") nil)))
                         ;; Vector of maps (or complex items treated as maps)
                         (let [items (->> data
                                          (map-indexed (fn [idx item-data]
                                                         (timbre/debug indent-str "    vector item (map/complex) index:" idx "data:" (pr-str item-data))
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
                           (if (seq items)
                             (do (timbre/debug indent-str "    vector (map/complex) result (items):" (count items))
                                 (into [:<>] items))
                             (do (timbre/debug indent-str "    vector (map/complex) result: nil (no meaningful items)") nil)))))
                     (do (timbre/debug indent-str "  vector itself is not meaningful or empty.") nil))
                   :else (do (timbre/debug indent-str "generate-summary-hiccup* fell into :else case, returning nil for data:" (pr-str data)) nil))
          ]
      (timbre/debug indent-str "[generate-summary-hiccup* END level " level "] returning:" (if (and (vector? result) (pos? (count result))) "hiccup" (pr-str result)))
      result)))

(defn generate-summary-hiccup
  "公共入口函数，生成总结的Hiccup结构。"
  [data schema system-name]
  (timbre/info "generate-summary-hiccup called for system:" system-name "data:" (pr-str data) "schema:" (pr-str schema))
  (when-let [content (generate-summary-hiccup* data schema 0)] ;; 初始 level 设为 0
    (when (or (not (vector? content)) (seq content))
      (let [summary-hiccup [:div {:class (str "summary-section summary-" (name system-name))
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
                             content]]]
        (timbre/info "generate-summary-hiccup for system:" system-name " -> SUCCESSFULLY generated hiccup.")
        summary-hiccup))
    (do (timbre/warn "generate-summary-hiccup for system:" system-name " -> resulted in EMPTY content, not rendering section.")
        nil)))
