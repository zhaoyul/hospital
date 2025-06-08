(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]
            [hc.hospital.pages.assessment-form-generators :as afg] ;; 确保已添加
            [hc.hospital.utils :as utils])) ;; 确保已添加

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

;; --- 新添加的函数 ---

(declare process-initial-values-by-schema-impl) ;; 声明内部递归函数

(defn process-initial-values-by-schema
  "根据 Malli schema 递归处理表单的初始数据。
   主要用于将符合特定条件的日期字符串转换为 dayjs 对象。
   - data: 表单的初始数据 (map)。
   - schema: 对应数据的 Malli schema。"
  [data schema]
  (timbre/debug "process-initial-values-by-schema: Initial data:" data "Schema form:" (m/form schema))
  (if (or (nil? data) (nil? schema))
    data ;; 如果数据或 schema 为空，则直接返回原始数据
    (process-initial-values-by-schema-impl data (m/deref schema) [])))

(defn- process-initial-values-by-schema-impl
  "process-initial-values-by-schema 的内部递归实现。
   - current-data: 当前层级的数据。
   - current-schema: 当前层级的 schema (已解引用)。
   - path: 当前数据在整体结构中的路径 (用于调试)。"
  [current-data current-schema path]
  (timbre/trace "process-impl: Path:" path "Data:" current-data "Schema type:" (m/type current-schema) "Schema form:" (m/form current-schema))
  (cond
    ;; 基本情况：如果数据为 nil，直接返回
    (nil? current-data)
    current-data

    ;; Schema 是 :map 类型
    (= :map (m/type current-schema))
    (if (map? current-data)
      (reduce-kv
       (fn [acc k v]
         (let [entry-schema-wrapper (->> (m/entries current-schema {:preserve-entry-properties true})
                                         (some (fn [[entry-key schema-from-entry _ _]]
                                                 (when (= entry-key k) schema-from-entry))))
               entry-schema (when entry-schema-wrapper (m/schema entry-schema-wrapper))]
           (if entry-schema
             (assoc acc k (process-initial-values-by-schema-impl v (m/deref entry-schema) (conj path k)))
             (do
               (timbre/warn "No schema found for key:" k "at path:" path "Skipping processing for this key.")
               (assoc acc k v))))) ; 如果找不到对应的 schema entry，保留原值
       {} ;; 从空 map 开始构建，确保只包含 schema 中定义的键或已处理的键
       current-data) ; current-data 已经确定是 map
      (do
        (timbre/warn "Data is not a map for a :map schema at path:" path "Data:" current-data)
        current-data)) ; 如果 schema 是 map 但数据不是 map，则返回原始数据

    ;; Schema 是 :maybe 类型 (可选)
    (= :maybe (m/type current-schema))
    (let [child-schema (-> current-schema m/children first m/deref)]
      (process-initial-values-by-schema-impl current-data child-schema path)) ; 路径不变，因为 :maybe 不增加路径层级

    ;; Schema 是其他类型（可能是叶子节点或需要特殊处理的类型）
    :else
    (if (afg/is-date-string-schema? current-schema)
      (if (string? current-data)
        (if-not (str/blank? current-data)
          (let [parsed-date (utils/parse-date current-data)]
            ;; 假设 utils/parse-date 返回 dayjs 对象, 检查其是否有效
            (if (.isValid parsed-date)
              (do
                (timbre/debug "Converted date at path:" path "Original:" current-data "Parsed:" parsed-date)
                parsed-date)
              (do
                (timbre/warn "Invalid date string at path:" path "Value:" current-data)
                current-data))) ; 无效日期字符串，返回原值
          current-data) ; 空字符串，返回原值
        current-data) ; 不是字符串，返回原值（例如已经是 dayjs 对象或 nil）
      current-data))) ; 非日期类型，返回原值
