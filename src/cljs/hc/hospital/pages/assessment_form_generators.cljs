(ns hc.hospital.pages.assessment-form-generators
  (:require
   [malli.core :as m]
   ;; Removed [malli.util :as mu]
   [reagent.core :as r]
   ["antd" :refer [Form Input InputNumber DatePicker Radio Select Checkbox]]
   ["react" :as React]
   [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
   [clojure.string :as str]
   [taoensso.timbre :as timbre]))

;; Declare mutually recursive functions
(declare render-form-item-from-spec)
(declare render-map-schema-fields)
(declare render-conditional-map-section)

;; --- Custom Utility Functions ---
(defn keyword->label [k]
  (when k
    (-> (name k)
        (str/replace #"-" " ")
        (str/capitalize))))

;; --- Malli Introspection Helpers (Inferred & Custom) ---
(defn is-conditional-map-schema?
  "Checks if a schema is a map with a conditional structure.
   Heuristic: a map where the first child's schema is an :enum,
   and subsequent keys in the map correspond to the enum values,
   representing the conditional branches.
   Example: [:map [:condition-key [:enum :a :b]] [:a map-a-schema] [:b map-b-schema]]"
  [schema]
  (if (and schema (= :map (m/type schema)))
    (let [children (m/children schema)]
      (if (seq children)
        (let [first-child-props (second (first children))]
          (= :enum (m/type first-child-props)))
        false))
    false))

(defn get-map-schema-conditional-key
  "Gets the keyword of the conditional key from a conditional map schema.
   Assumes the first key in the map definition is the conditional one."
  [schema]
  (when (is-conditional-map-schema? schema)
    (-> (m/children schema) first first)))

(defn get-map-schema-conditional-options-map
  "Extracts the map of conditional options {enum-value schema} from a conditional map schema.
   Assumes the schema is structured like [:map [:cond-key [:enum :val1 ..]] [:val1 schema1] ...]"
  [schema]
  (when (is-conditional-map-schema? schema)
    (let [children (m/children schema)
          conditional-key-entry (first children)
          conditional-key-name (first conditional-key-entry)
          option-entries (filter #(not= (first %) conditional-key-name) children)]
      (into {} (map (fn [[k v]] [k v]) option-entries)))))


(defn is-map-schema-with-conditional-key?
  "Checks for the common pattern like [:map [:有无 ...] [:详情 ...]]
   where :有无 is an enum and :详情 is a map schema."
  [schema]
  (if (and schema (= :map (m/type schema)))
    (let [props (into {} (m/entries schema))]
      (and (contains? props :有无)
           (contains? props :详情)
           (= :enum (m/type (get props :有无)))
           (= :map (m/type (get props :详情)))))
    false))

(defn is-date-string-schema?
  "Checks if a schema is likely a date string schema.
   Heuristic: schema name is '日期字符串Spec' or 'Optional日期字符串',
   or it's a regex schema matching YYYY-MM-DD."
  [schema]
  (if schema
    (or
     (if (= :re (m/type schema))
       (let [children (m/children schema)]
         (and (first children) (instance? js/RegExp (first children))
              (or (= (str (first children)) "/\\d{4}-\\d{2}-\\d{2}/")
                  (= (str (first children)) "/\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?Z?/"))))
       false)
     (:type/date? (m/properties schema)))
    false))

(defn get-key-optional-status
  "Finds if a key within a map schema is optional using m/entries."
  [map-schema key-to-find]
  (let [entries (m/entries map-schema)]
    (if-let [entry (some (fn [[k _ optional? _]] (when (= k key-to-find) optional?)) entries)]
      entry
      false)))

;; --- Malli Helper Functions (Original from subtask) ---
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

;; Initial Rendering Functions (specific input types)
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

;; --- Core Data-Driven Rendering Functions ---
(defn render-map-schema-fields [map-schema parent-form-path form-instance]
  (let [entries (m/entries map-schema)]
    (mapv (fn [[field-key field-schema optional? entry-props]]
            (let [current-path (conj parent-form-path field-key)]
              [render-form-item-from-spec [field-key field-schema optional? parent-form-path form-instance entry-props]]))
          entries)))

(defn render-conditional-map-section [field-key field-schema parent-form-path form-instance entry-props]
  (let [conditional-key (get-map-schema-conditional-key field-schema)
        options-map (get-map-schema-conditional-options-map field-schema)
        conditional-form-path (conj parent-form-path field-key conditional-key)
        conditional-value-watch (Form.useWatch (clj->js conditional-form-path) (:form-instance form-instance))
        label-text (or (:label entry-props) (keyword->label field-key))]
    (when (nil? conditional-key)
      (throw (js/Error. (str "render-conditional-map-section: conditional-key is nil for " field-key))))
    (when (nil? options-map)
      (throw (js/Error. (str "render-conditional-map-section: options-map is nil for " field-key))))
    (when (nil? conditional-value-watch)
      (timbre/info "Conditional value for " conditional-form-path " is nil, section may not render if not intended.")
      )
    [:<> {:key (str (name field-key) "-conditional-section")}
     [render-form-item-from-spec [conditional-key (get options-map conditional-key) false parent-form-path form-instance {:label label-text}]]
     (when-let [detail-schema (get options-map conditional-value-watch)]
       (let [detail-path (conj parent-form-path field-key :详情)]
         [:div {:key (str (name field-key) "-details-" conditional-value-watch)
                :style {:marginLeft "20px" :borderLeft "2px solid #eee" :paddingLeft "15px"}}
          [render-map-schema-fields detail-schema detail-path form-instance]]))]))

(defn render-form-item-from-spec [[field-key field-schema optional? parent-form-path form-instance entry-props]]
  (let [form-path (conj parent-form-path field-key)
        label-text (or (:label entry-props) (keyword->label field-key))
        malli-type (timbre/spy :info (get-malli-type field-schema))
        malli-props (get-malli-properties field-schema)
        is-cond-map (is-conditional-map-schema? field-schema)
        is-map-with-cond-key (is-map-schema-with-conditional-key? field-schema)]

    (cond
      is-cond-map
      [render-conditional-map-section field-key field-schema parent-form-path form-instance entry-props]

      is-map-with-cond-key
      (let [child-map-schema field-schema
            has-key (get-map-schema-conditional-key child-map-schema)
            detail-key :详情
            has-form-path (conj form-path has-key)
            has-value-watch (Form.useWatch (clj->js has-form-path) (:form-instance form-instance))
            options-map (get-map-schema-conditional-options-map child-map-schema)
            detail-map-schema (get options-map detail-key)
            is-has-optional (get-key-optional-status child-map-schema has-key) ; Using workaround
            has-field-schema (get options-map has-key)]
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


      :else
      (do (timbre/warn (str "No renderer for malli type: " malli-type " of field " field-key))
          [:p (str "Unrecognized type: " malli-type " for " label-text)]))))
