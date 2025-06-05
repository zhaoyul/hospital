(ns hc.hospital.pages.assessment-form-generators
  (:require
   [malli.core :as m]
   ;; Removed [malli.util :as mu] ;; 已移除 [malli.util :as mu]
   [reagent.core :as r]
   ["antd" :refer [Form Input InputNumber DatePicker Radio Select Checkbox]]
   ["react" :as React]
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

;; Declare mutually recursive functions ;; 声明相互递归函数
(declare render-form-item-from-spec)
(declare render-map-schema-fields)
(declare render-conditional-map-section)

;; --- Custom Utility Functions --- ;; --- 自定义工具函数 ---
(defn keyword->label [k]
  (when k
    (-> (name k)
        (str/replace #"-" " ")
        (str/capitalize))))

(defn- get-entry-details [map-schema key-to-find]
  (when (and map-schema key-to-find (m/schema? map-schema) (m/type map-schema)) ;; Check if it's a schema first
    (if (= :map (m/type map-schema)) ;; Only proceed if it's a map schema
      (when-let [entry-pair (clojure.core/find (fn [[k _]] (= k key-to-find)) (m/entries map-schema))]
        (let [val-schema-wrapper (second entry-pair)]
          {:schema (if (m/schema? val-schema-wrapper) (m/schema val-schema-wrapper) nil) ;; ensure val-schema-wrapper is a schema
           :optional? (if (m/schema? val-schema-wrapper) (:optional (m/properties val-schema-wrapper) false) false)}))
      (timbre/warn "get-entry-details called with non-map schema:" (m/type map-schema) "for key:" key-to-find))))

;; --- Malli Introspection Helpers (Inferred & Custom) --- ;; --- Malli 内省辅助函数 (推断和自定义) ---
(defn is-conditional-map-schema?
  "检查一个 schema 是否是具有条件结构的 map。
   启发式规则：一个 map，其中第一个子项的 schema 是一个 :enum，
   并且 map 中的后续键对应于枚举值，表示条件分支。
   例如：[:map [:condition-key [:enum :a :b]] [:a map-a-schema] [:b map-b-schema]]"
  [schema]
  (if (and schema (m/schema? schema) (= :map (m/type schema)))
    (let [children (m/children schema)]
      (if (seq children)
        (let [first-child-props (second (first children))]
          ;; Ensure first-child-props is a schema before calling m/type
          ;; 在调用 m/type 之前确保 first-child-props 是一个 schema
          (if (m/schema? first-child-props)
            (= :enum (m/type first-child-props))
            false))
        false))
    false))

(defn get-map-schema-conditional-key
  "从条件 map schema 中获取条件键的关键字。
   假设 map 定义中的第一个键是条件键。"
  [schema]
  (when (is-conditional-map-schema? schema)
    (-> (m/children schema) first first)))

(defn get-map-schema-conditional-options-map
  "从条件 map schema 中提取条件选项的 map {枚举值 schema}。
   假设 schema 的结构类似于 [:map [:cond-key [:enum :val1 ..]] [:val1 schema1] ...]"
  [schema]
  (when (is-conditional-map-schema? schema)
    (let [children (m/children schema)
          conditional-key-entry (first children)
          conditional-key-name (first conditional-key-entry)
          option-entries (filter #(not= (first %) conditional-key-name) children)]
      (into {} (map (fn [[k v]] [k v]) option-entries)))))


(defn is-map-schema-with-conditional-key?
  "检查常见的模式，例如 [:map [:有无 ...] [:详情 ...]]，
   其中 :有无 是一个枚举，:详情 是一个 map schema。"
  [schema]
  (if (and schema (m/schema? schema) (= :map (m/type schema)))
    (let [props (into {} (m/entries schema))
          prop-ynu (get props :有无)
          prop-detail (get props :详情)]
      (and (contains? props :有无)
           (contains? props :详情)
           (if (m/schema? prop-ynu) (= :enum (m/type prop-ynu)) false)
           (if (m/schema? prop-detail) (= :map (m/type prop-detail)) false)))
    false))

(defn is-date-string-schema?
  "检查一个 schema 是否可能是日期字符串 schema。
   启发式规则：schema 名称为 '日期字符串Spec' 或 'Optional日期字符串'，
   或者它是一个匹配 YYYY-MM-DD 的正则表达式 schema。"
  [schema]
  (if (and schema (m/schema? schema)) ;; Added m/schema? check ;; 添加了 m/schema? 检查
    (or
     (if (= :re (m/type schema)) ;; m/type is safe now ;; m/type 现在是安全的
       (let [children (m/children schema)] ;; m/children is safe if m/type was :re ;; 如果 m/type 是 :re，则 m/children 是安全的
         (and (first children) (instance? js/RegExp (first children))
              (or (= (str (first children)) "/\\d{4}-\\d{2}-\\d{2}/")
                  (= (str (first children)) "/\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?Z?/"))))
       false)
     (:type/date? (m/properties schema))) ;; m/properties is safe ;; m/properties 是安全的
    false))

(defn get-key-optional-status
  "使用 m/entries 查找 map schema 中的某个键是否是可选的。"
  [map-schema key-to-find]
  (if (and map-schema (m/schema? map-schema) key-to-find)
    (let [entries (m/entries map-schema)] ;; m/entries is safe now ;; m/entries 现在是安全的
      (if-let [entry (some (fn [[k _ optional? _]] (when (= k key-to-find) optional?)) entries)]
        entry
        false))
    false)) ;; Return false if map-schema is nil, not a schema, or key-to-find is nil ;; 如果 map-schema 为 nil、不是 schema 或 key-to-find 为 nil，则返回 false

;; --- Malli Helper Functions (Original from subtask) --- ;; --- Malli 辅助函数 (来自子任务的原始版本) ---
(defn get-malli-type [schema]
  (if (m/schema? schema)
    (m/type schema)
    nil))

(defn get-malli-properties [schema]
  (if (m/schema? schema)
    (m/properties schema)
    {}))

(defn get-malli-children [schema]
  (if (m/schema? schema)
    (m/children schema)
    []))

(defn is-optional-schema? [schema]
  (= :maybe (get-malli-type schema)))

(defn get-enum-options [schema]
  (when (= :enum (get-malli-type schema))
    (let [children (get-malli-children schema)]
      (mapv (fn [opt] {:value (if (keyword? opt) (name opt) opt)
                      :label (if (keyword? opt) (name opt) (str opt))})
            children))))

;; Initial Rendering Functions (specific input types) ;; 初始渲染函数 (特定输入类型)
(defn render-text-input [field-schema form-path label-text]
  [:> Form.Item {:name (clj->js form-path) :label label-text}
   [:> Input {:placeholder (str "请输入" label-text)}]])

(defn render-number-input [field-schema form-path label-text]
  (let [props (get-malli-properties field-schema)
        min-val (:min props)
        max-val (:max props)
        input-props (cond-> {:placeholder (str "请输入" label-text) :style {:width "100%"}}
                      min-val (assoc :min min-val)
                      max-val (assoc :max max-val))]
    [:> Form.Item {:name (clj->js form-path) :label label-text}
     [:> InputNumber (clj->js input-props)]]))

(defn render-datepicker [field-schema form-path label-text]
  [:> Form.Item {:name (clj->js form-path) :label label-text}
   [:> DatePicker {:style {:width "100%"} :placeholder (str "请选择" label-text)}]])

(defn render-radio-group [field-schema form-path label-text]
  (let [options (get-enum-options field-schema)]
    [:> Form.Item {:name (clj->js form-path) :label label-text}
     [:> Radio.Group {:options (clj->js options)}]]))

(defn render-select [field-schema form-path label-text]
  (let [options (get-enum-options field-schema)]
    [:> Form.Item {:name (clj->js form-path) :label label-text}
     [:> Select {:placeholder (str "请选择" label-text)
                 :options (clj->js options)
                 :allowClear true
                 :style {:width "100%"}}]]))

(defn render-checkbox-group [field-schema form-path label-text]
  (let [vector-child-schema (first (get-malli-children field-schema))
        options (if vector-child-schema (get-enum-options vector-child-schema) [])]
    [:> Form.Item {:name (clj->js form-path) :label label-text}
     [:> Checkbox.Group {:options (clj->js options)}]]))

;; --- Core Data-Driven Rendering Functions --- ;; --- 核心数据驱动渲染函数 ---
(defn render-map-schema-fields [map-schema parent-form-path form-instance]
  (let [entries (m/entries map-schema)]
    (into [:<>]
          (mapv (fn [[field-key field-schema optional? entry-props]]
                  (let [current-path (conj parent-form-path field-key)]
                    [render-form-item-from-spec [field-key field-schema optional? parent-form-path form-instance entry-props]]))
                entries))))

(defn render-conditional-map-section [field-key field-schema parent-form-path form-instance entry-props]
  (let [conditional-key (get-map-schema-conditional-key field-schema)
        options-map (get-map-schema-conditional-options-map field-schema)
        conditional-form-path (conj parent-form-path field-key conditional-key)
        conditional-value-watch (Form.useWatch (clj->js conditional-form-path) (:form-instance form-instance))
        label-text (or (:label entry-props) (keyword->label field-key))
        actual-conditional-key-schema (:schema (get-entry-details field-schema conditional-key))] ;; Get the actual schema for the conditional key ;; 获取条件键的实际 schema
    (when (nil? conditional-key)
      (throw (js/Error. (str "render-conditional-map-section: conditional-key is nil for " field-key))))
    (when (nil? options-map)
      (throw (js/Error. (str "render-conditional-map-section: options-map is nil for " field-key))))
    (when (nil? conditional-value-watch)
      (timbre/info "Conditional value for " conditional-form-path " is nil, section may not render if not intended.")
      )
    [:<> {:key (str (name field-key) "-conditional-section")}
     [render-form-item-from-spec [conditional-key actual-conditional-key-schema false parent-form-path form-instance {:label label-text}]]
     (when-let [detail-schema (get options-map conditional-value-watch)]
       (let [detail-path (conj parent-form-path field-key :详情)]
         [:div {:key (str (name field-key) "-details-" conditional-value-watch)
                :style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
          [render-map-schema-fields detail-schema detail-path form-instance]]))]))

(defn render-form-item-from-spec [[field-key field-schema optional? parent-form-path form-instance entry-props]]
  (let [form-path (conj parent-form-path field-key)
        label-text (timbre/spy :info (or (:label entry-props) (keyword->label field-key)))
        malli-type (timbre/spy :info (get-malli-type field-schema))
        malli-props (get-malli-properties field-schema)
        is-cond-map (is-conditional-map-schema? field-schema)
        is-map-with-cond-key (is-map-schema-with-conditional-key? field-schema)]

    (cond
      is-cond-map
      [render-conditional-map-section field-key field-schema parent-form-path form-instance entry-props]

      is-map-with-cond-key
      (let [child-map-schema field-schema
            has-key :有无 ;; Assuming this is the primary key for this pattern ;; 假设这是此模式的主键
            detail-key :详情 ;; Assuming this is the details key ;; 假设这是详情键
            has-form-path (conj form-path has-key)
            has-value-watch (Form.useWatch (clj->js has-form-path) (:form-instance form-instance))

            ;; Directly get schemas using m/entry ;; 使用 m/entry 直接获取 schema
            has-field-data (get-entry-details child-map-schema has-key)
            detail-field-data (get-entry-details child-map-schema detail-key)

            has-field-schema (:schema has-field-data)
            is-has-optional (:optional? has-field-data false) ;; m/entry provides optional status directly ;; m/entry 直接提供可选状态

            detail-map-schema (:schema detail-field-data)
            ;; is-detail-optional could also be retrieved if needed: (:optional? detail-field-data false) ;; 如果需要，也可以检索 is-detail-optional：(:optional? detail-field-data false)
            ]
        [:<> {:key (str (name field-key))}
         [render-form-item-from-spec [has-key has-field-schema is-has-optional form-path form-instance {:label label-text}]]
         (when (and (= has-value-watch :有) detail-map-schema)
           [:div {:key (str (name field-key) "-details") :style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
            [render-map-schema-fields detail-map-schema (conj form-path detail-key) form-instance]])])

      (= malli-type :map)
      [:div {:key (str (name field-key) "-map-section") :style {:marginBottom "10px"}}
       [:h4 {:style {:fontSize "15px" :marginBottom "8px" :borderBottom "1px solid #f0f0f0" :paddingBottom "4px"}} label-text]
       [render-map-schema-fields field-schema form-path form-instance]]

      (= malli-type :string)
      [render-text-input field-schema form-path label-text]

      (or (= malli-type :int) (= malli-type :double) (= malli-type :float))
      [render-number-input field-schema form-path label-text]

      (= malli-type :enum)
      (let [enum-count (count (get-malli-children field-schema))]
        (if (> enum-count 3)
          [render-select field-schema form-path label-text]
          [render-radio-group field-schema form-path label-text]))

      (= malli-type :vector)
      (if (= :enum (get-malli-type (first (get-malli-children field-schema))))
        [render-checkbox-group field-schema form-path label-text]
        (do (timbre/warn "Unsupported vector child type for field " field-key) nil))

      (is-date-string-schema? field-schema)
      [render-datepicker field-schema form-path label-text]

      (= malli-type :boolean)
      [render-radio-group assessment-specs/是否Enum form-path label-text]

      (= malli-type :malli.core/val)
      (let [unwrapped-schema (first (get-malli-children field-schema))]
        (if unwrapped-schema
          [render-form-item-from-spec [field-key unwrapped-schema optional? parent-form-path form-instance entry-props]]
          (do (timbre/warn (str "Malli type :malli.core/val for field " field-key " has no child schema."))
              [:p (str "No child schema for :malli.core/val type for " label-text)])))

      (= malli-type :maybe)
      (let [unwrapped-schema (first (get-malli-children field-schema))]
        (if unwrapped-schema
          [render-form-item-from-spec [field-key unwrapped-schema optional? parent-form-path form-instance entry-props]]
          (do (timbre/warn (str "Malli type :maybe for field " field-key " has no child schema."))
              [:p (str "No child schema for :maybe type for " label-text)])))

      :else
      (do (timbre/warn (str "No renderer for malli type: " malli-type " of field " field-key))
          [:p (str "Unrecognized type: " malli-type " for " label-text)]))))
