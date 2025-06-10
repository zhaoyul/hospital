(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]
            [hc.hospital.utils :as utils]
            ["dayjs" :as dayjs])) ; Ensure dayjs is required

(defn get-first-enum-value
  "获取 Malli schema 中 :enum 类型的第一个值作为默认值。
   支持处理嵌套在 :maybe 内的 :enum schema。"
  [field-schema]
  (let [dereffed-schema (m/deref field-schema) ; 首先解引用 schema
        ;; 如果是 :maybe 类型，则取其子 schema；否则使用解引用后的 schema
        actual-schema (if (= :maybe (m/type dereffed-schema))
                        (-> dereffed-schema m/children first m/deref) ; 对子 schema 也解引用
                        dereffed-schema)]
    (timbre/debug "get-first-enum-value for schema:" (m/form field-schema) "Dereffed to:" (m/form dereffed-schema) "Actual for enum check:" (m/form actual-schema))
    (if (and actual-schema (= :enum (m/type actual-schema)))
      (let [options (m/children actual-schema)]
        (when (seq options)
          (first options)))
      (do (timbre/debug "Not an enum or valid maybe-enum:" (m/form actual-schema)) nil))))

;; Forward declaration for mutual recursion
(declare apply-enum-defaults-to-data-impl)

(defn- apply-enum-defaults-to-data-impl
  "内部递归函数，用于向数据中应用枚举默认值。
   data: 当前正在处理的数据部分 (map)。
   schema: 对应于当前数据部分的 Malli schema。
   current-path: 用于日志记录的当前字段路径 (vector of keywords)。"
  [data schema current-path]
  (let [dereffed-outer-schema (m/deref schema)] ; 解引用最外层 schema
    (timbre/debug "apply-enum-defaults-to-data-impl for PATH:" current-path "DATA:" data "SCHEMA TYPE:" (m/type dereffed-outer-schema))
    ;; 基本情况：如果 schema 不是 :map 类型，则不应由此函数处理（或已到达叶子节点）
    (if-not (= :map (m/type dereffed-outer-schema))
      (do
        (timbre/warn "apply-enum-defaults-to-data-impl called with non-map schema at path" current-path "Schema type:" (m/type dereffed-outer-schema) "Schema form:" (m/form schema))
        data)
      ;; 递归情况：处理 :map 类型的 schema
      (let [entries (m/entries dereffed-outer-schema {:preserve-entry-properties true})] ; 获取 map schema 的所有条目
        (reduce
         (fn [processed-data [field-key schema-from-entry _ entry-props]] ; 解构每个条目
           (let [field-path (conj current-path field-key) ; 构建当前字段的完整路径
                 current-value (get processed-data field-key) ; 从累积的数据中获取当前字段的值
                 entry-schema-actual (m/deref (m/schema schema-from-entry))] ; 解引用条目中的 schema
             (timbre/debug "Processing field:" field-path "Value:" current-value "Raw Schema Entry form:" (m/form schema-from-entry) "Dereffed Entry Schema form:" (m/form entry-schema-actual) "Type:" (m/type entry-schema-actual))
             (cond
               ;; 情况1: 当前值为 nil
               (nil? current-value)
               (if-let [default-enum (get-first-enum-value entry-schema-actual)] ; 尝试获取枚举默认值
                 (do (timbre/info "PATH:" field-path "- Value is nil. Applying enum default:" default-enum)
                     (assoc processed-data field-key default-enum)) ; 应用默认值
                 ;; 如果不是枚举，但 schema 是 map 类型，则递归处理空 map
                 (if (= :map (m/type entry-schema-actual))
                   (do (timbre/info "PATH:" field-path "- Value is nil. Field is a map. Recursing with {} for this map.")
                       (assoc processed-data field-key (apply-enum-defaults-to-data-impl {} entry-schema-actual field-path)))
                   ;; 其他情况（非枚举、非 map），不应用默认值
                   (do (timbre/debug "PATH:" field-path "- Value is nil. Not an enum, not a map. No default applied.")
                       processed-data)))

               ;; 情况2: 当前值是一个 map，并且对应的 schema 也是 :map 类型，则递归处理
               (and (map? current-value) (= :map (m/type entry-schema-actual)))
               (do (timbre/debug "PATH:" field-path "- Value is a map. Recursing.")
                   (assoc processed-data field-key (apply-enum-defaults-to-data-impl current-value entry-schema-actual field-path)))

               ;; 情况3: 其他所有情况 (值存在且不需要特殊处理)，保持原样
               :else
               (do (timbre/debug "PATH:" field-path "- Value exists (" current-value "), not requiring special handling for defaulting or recursion. Keeping value.")
                   processed-data))))
         data ; reduce 的初始值为传入的 data
         entries))))) ; reduce 作用于 schema 的所有条目

(defn apply-enum-defaults-to-data
  "公开的包装函数，用于将 Malli schema 中定义的枚举类型字段的默认值（第一个枚举值）递归地应用到数据上。
   主要用于初始化表单数据，确保所有枚举字段在数据中存在一个初始值（如果它们尚未被设置）。
   data: 要处理的数据 (通常是一个 map，可以为 nil，此时会视为空 map)。
   schema: 描述数据结构的 Malli schema。"
  [data schema]
  (timbre/info "Public apply-enum-defaults-to-data for schema form:" (m/form schema) "(type:"(m/type schema) "dereffed type:" (m/type (m/deref schema)) ") with data:" data)
  (let [start-data (if (nil? data) {} data) ; 如果输入数据为 nil，则从空 map 开始
        dereffed-initial-schema (m/deref schema)] ; 解引用顶层 schema
    (if (nil? dereffed-initial-schema)
      (do (timbre/error "Cannot apply defaults: Schema dereferences to nil. Original schema form:" (m/form schema))
          start-data) ; 如果 schema 无效，则返回原始数据
      (apply-enum-defaults-to-data-impl start-data dereffed-initial-schema [])))) ; 调用内部实现

;; --- preprocess-date-fields function ---
(declare preprocess-date-fields*) ;; 声明内部递归辅助函数

(defn preprocess-date-fields
  "接收数据和 Malli schema，递归地将 schema 中标记为 `:is-date? true` 的字符串字段转换为 dayjs 对象。
   支持处理嵌套的 map、vector 以及 :maybe 类型的 schema。"
  [data schema]
  ;; 如果数据或 schema 为 nil，则直接返回数据，不做处理。
  (if (or (nil? data) (nil? schema))
    data
    (preprocess-date-fields* data schema)))

(defn- preprocess-date-fields*
  "内部递归函数，根据 schema 类型处理日期字段转换。"
  [data schema]
  (let [schema-type (m/type schema)
        schema-props (m/properties schema)]
    (cond
      ;; 处理 Optional Schemas (Maybe)
      ;; 如果 schema 是 :maybe 类型, 并且 data 不为 nil,
      ;; 则获取 :maybe 的子 schema (即实际的类型 schema), 并用此子 schema 递归处理 data。
      (= :maybe schema-type)
      (when-not (nil? data) ;; 只有当 data 不为 nil 时才处理 :maybe 内部
        (let [child-schema (first (m/children schema))]
          (preprocess-date-fields* data child-schema)))

      ;; 处理 Map Schemas
      ;; 如果 schema 是 :map 类型, 并且 data 是一个 map,
      ;; 则遍历 data 中的键值对。对于每个键值对,
      ;; 从 schema-entries-map 中获取对应的子 schema, 并用此子 schema 递归处理值。
      ;; 如果 schema 中不存在某个键, 则保留其原始值。
      (= :map schema-type)
      (if-not (map? data)
        data ;; 如果 schema 是 map 但数据不是 map，则直接返回数据, 可能发出警告
        (let [schema-entries-map (into {} (map (juxt :key :schema) (m/entries schema)))]
          (reduce-kv
           (fn [m k v]
             (if-let [value-schema (get schema-entries-map k)]
               (assoc m k (preprocess-date-fields* v value-schema))
               (assoc m k v))) ;; 如果键不在 schema 中定义, 保留原值 (或者说，递归调用时 value-schema 为 nil，由下一层处理)
           {} ;; 从空 map 开始构建结果
           data))) ;; 处理 data 中的所有键，如果 schema 中没有对应项，则 value-schema 会是 nil，由 preprocess-date-fields* 的 nil schema 检查处理或在 :else 分支原样返回


      ;; 处理 Vector Schemas
      ;; 如果 schema 是 :vector 类型, 并且 data 是一个 vector,
      ;; 则获取 vector 的元素 schema, 并用此元素 schema 递归处理 vector 中的每个元素。
      (= :vector schema-type)
      (if-not (vector? data)
        data ;; 如果 schema 是 vector 但数据不是 vector，则直接返回数据
        (let [element-schema (first (m/children schema))] ;; 获取 vector 元素的 schema
          (if element-schema
            (mapv #(preprocess-date-fields* % element-schema) data)
            data))) ;; 如果 vector schema 没有子元素定义 (不常见), 返回原数据

      ;; 核心转换逻辑：检查字段是否标记为日期并进行转换
      ;; 如果 schema 的属性中标记了 :is-date? true,
      ;; 并且 data 是一个非空字符串, 则尝试将其解析为 dayjs 对象。
      ;; `utils/parse-date` 应返回一个 dayjs 对象。
      ;; 如果解析失败或结果无效, 则保留原值并记录警告/错误。
      (and (:is-date? schema-props) (string? data) (not (str/blank? data)))
      (try
        (let [parsed-date (utils/parse-date data)] ;; utils/parse-date 应该返回 dayjs 对象
          (if (.isValid parsed-date)
            parsed-date
            (do (timbre/warn (str "值 \"" data "\" 无法解析为有效日期 (isValid failed)，保留原值。")) data)))
        (catch js/Error e
          (timbre/error (str "解析日期字符串 \"" data "\" 时发生 JS 错误: " (.-message e) "，保留原值。"))
          data))

      ;; 默认情况：如果以上条件都不满足，则不进行转换，直接返回原始数据。
      :else data)))

;; --- transform-date-fields-for-submission ---
(declare transform-date-fields-for-submission*) ;; 声明内部递归辅助函数

(defn transform-date-fields-for-submission
  "接收数据和 Malli schema，递归地将 schema 中标记为 `:is-date? true` 的 dayjs 对象字段转换回 ISO 字符串。
   用于在提交表单数据前进行转换。
   支持处理嵌套的 map、vector 以及 :maybe 类型的 schema。"
  [data schema]
  ;; 如果数据或 schema 为 nil，则直接返回数据，不做处理。
  (if (or (nil? data) (nil? schema))
    data
    (transform-date-fields-for-submission* data schema)))

(defn- transform-date-fields-for-submission*
  "内部递归函数，根据 schema 类型处理日期字段从 dayjs 对象到字符串的转换。"
  [data schema]
  (let [schema-type (m/type schema)
        schema-props (m/properties schema)]
    (cond
      ;; 处理 Optional Schemas (Maybe)
      (= :maybe schema-type)
      (when-not (nil? data)
        (let [child-schema (first (m/children schema))]
          (transform-date-fields-for-submission* data child-schema)))

      ;; 处理 Map Schemas
      (= :map schema-type)
      (if-not (map? data)
        data
        (let [schema-entries-map (into {} (map (juxt :key :schema) (m/entries schema)))]
          (reduce-kv
           (fn [m k v]
             (if-let [value-schema (get schema-entries-map k)]
               (assoc m k (transform-date-fields-for-submission* v value-schema))
               (assoc m k v)))
           {}
           data)))

      ;; 处理 Vector Schemas
      (= :vector schema-type)
      (if-not (vector? data)
        data
        (let [element-schema (first (m/children schema))]
          (if element-schema
            (mapv #(transform-date-fields-for-submission* % element-schema) data)
            data)))

      ;; 核心转换逻辑：检查字段是否为 dayjs 对象并标记为日期
      ;; TODO: 需要根据 spec 进一步区分日期和日期时间，目前统一使用 date->iso-string
      (and (:is-date? schema-props) (instance? dayjs data)) ; Check if it's a dayjs object
      (utils/date->iso-string data) ; Convert dayjs object to ISO string

      ;; 默认情况：不进行转换
      :else data)))
