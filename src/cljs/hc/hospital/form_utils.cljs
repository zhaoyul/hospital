(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]
            [hc.hospital.utils :as utils])) ; Added hc.hospital.utils

(defn get-first-enum-value [field-schema]
  (let [dereffed-schema (m/deref field-schema) ; Dereference the schema first
        actual-schema (if (= :maybe (m/type dereffed-schema))
                        (-> dereffed-schema m/children first m/deref) ; Deref the child too
                        dereffed-schema)]
    (timbre/info "get-first-enum-value for schema:" (m/form field-schema) "Dereffed to:" (m/form dereffed-schema) "Actual for enum check:" (m/form actual-schema))
    (if (and actual-schema (= :enum (m/type actual-schema)))
      (let [options (m/children actual-schema)]
        (when (seq options)
          (first options)))
      (do (timbre/info "Not an enum or valid maybe-enum:" (m/form actual-schema)) nil))))

;; Forward declaration for mutual recursion
(declare apply-enum-defaults-to-data-impl)

(defn- apply-enum-defaults-to-data-impl [data schema current-path]
  (let [dereffed-outer-schema (m/deref schema)]
    (timbre/info "apply-enum-defaults-to-data-impl for PATH:" current-path "DATA:" data "SCHEMA TYPE:" (m/type dereffed-outer-schema))
    (if-not (= :map (m/type dereffed-outer-schema))
      (do
        (timbre/warn "apply-enum-defaults-to-data-impl called with non-map schema at path" current-path "Schema type:" (m/type dereffed-outer-schema) "Schema form:" (m/form schema))
        data)
      (let [entries (m/entries dereffed-outer-schema {:preserve-entry-properties true})]
        (reduce
         (fn [processed-data [field-key schema-from-entry _ entry-props]] ; Deconstruct the entry
           (let [field-path (conj current-path field-key)
                 current-value (get processed-data field-key) ; Get value from accumulator
                 entry-schema-actual (m/deref (m/schema schema-from-entry))] ; Deref the schema from entry
             (timbre/info "Processing field:" field-path "Value:" current-value "Raw Schema Entry form:" (m/form schema-from-entry) "Dereffed Entry Schema form:" (m/form entry-schema-actual) "Type:" (m/type entry-schema-actual))
             (cond
               (nil? current-value)
               (if-let [default-enum (get-first-enum-value entry-schema-actual)]
                 (do (timbre/info "PATH:" field-path "- Value is nil. Applying enum default:" default-enum)
                     (assoc processed-data field-key default-enum))
                 (if (= :map (m/type entry-schema-actual))
                   (do (timbre/info "PATH:" field-path "- Value is nil. Field is a map. Recursing with {} for this map.")
                       (assoc processed-data field-key (apply-enum-defaults-to-data-impl {} entry-schema-actual field-path)))
                   (do (timbre/info "PATH:" field-path "- Value is nil. Not an enum, not a map. No default applied.")
                       processed-data)))

               (and (map? current-value) (= :map (m/type entry-schema-actual)))
               (do (timbre/info "PATH:" field-path "- Value is a map. Recursing.")
                   (assoc processed-data field-key (apply-enum-defaults-to-data-impl current-value entry-schema-actual field-path)))

               :else
               (do (timbre/info "PATH:" field-path "- Value exists (" current-value "), not requiring special handling for defaulting or recursion. Keeping value.")
                   processed-data))))
         data ; Initial value for reduce
         entries))))) ; Collection for reduce

(defn apply-enum-defaults-to-data
  "Public wrapper function for applying enum defaults."
  [data schema]
  (timbre/info "Public apply-enum-defaults-to-data for schema form:" (m/form schema) "(type:"(m/type schema) "dereffed type:" (m/type (m/deref schema)) ") with data:" data)
  (let [start-data (if (nil? data) {} data)
        dereffed-initial-schema (m/deref schema)]
    (if (nil? dereffed-initial-schema)
      (do (timbre/error "Cannot apply defaults: Schema dereferences to nil. Original schema form:" (m/form schema))
          start-data)
      (apply-enum-defaults-to-data-impl start-data dereffed-initial-schema []))))

;; --- New preprocess-date-fields function ---
(declare preprocess-date-fields*) ;; 声明内部递归辅助函数

(defn preprocess-date-fields
  "接收数据和 Malli schema，递归地将 schema 中标记为日期的字符串字段转换为 dayjs 对象。"
  [data schema]
  (if (or (nil? data) (nil? schema))
    data
    (preprocess-date-fields* data schema)))

(defn- preprocess-date-fields*
  "内部递归函数，处理日期字段转换。"
  [data schema]
  (let [schema-type (m/type schema)]
    (cond
      ;; 处理 Optional Schemas (Maybe)
      (= :maybe schema-type)
      (when-not (nil? data) ;; 只有当 data 不为 nil 时才处理 :maybe 内部
        (let [child-schema (first (m/children schema))]
          (preprocess-date-fields* data child-schema)))

      ;; 处理 Map Schemas
      (= :map schema-type)
      (if-not (map? data)
        data ;; 如果 schema 是 map 但数据不是 map，则直接返回数据
        (let [schema-entries-map (into {} (map (juxt :key :schema) (m/entries schema)))]
          (reduce-kv
           (fn [m k v]
             (if-let [value-schema (get schema-entries-map k)]
               (assoc m k (preprocess-date-fields* v value-schema))
               (assoc m k v))) ;; If key not in schema, keep original value
           {} ;; 从空 map 开始构建
           (select-keys data (keys schema-entries-map))))) ;; 只处理 schema 中存在的键值对
           ;; Removed extra parenthesis here


      ;; 处理 Vector Schemas (假设 vector 包含的是相同类型的 schema)
      (= :vector schema-type)
      (if-not (vector? data)
        data ;; 如果 schema 是 vector 但数据不是 vector，则直接返回数据
        (let [element-schema (first (m/children schema))] ;; 获取 vector 元素的 schema
          (if element-schema
            (mapv #(preprocess-date-fields* % element-schema) data)
            data))) ;; 如果 vector schema 没有子元素定义，返回原数据

      ;; 检查是否有日期字段元数据
      (:is-date? (m/properties schema))
      (if (and (string? data) (not (clojure.string/blank? data)))
        (try
          (let [parsed-date (utils/parse-date data)]
             (if (.isValid parsed-date)
               parsed-date
               (do (timbre/warn (str "值 " data " 无法解析为有效日期，保留原值。")) data))) ; Corrected timbre/warn string concatenation
          (catch js/Error e
            (timbre/error (str "解析日期字符串 " data " 时发生错误: " (.-message e) "，保留原值。")) ; Corrected timbre/error string concatenation
            data))
        data) ;; 如果不是字符串或者为空，则不处理

      ;; 默认情况：不进行转换
      :else data)))
