(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(defn get-first-enum-value
  "Extracts the first enum value from a Malli schema.
   Handles :enum and [:maybe [:enum ...]] schemas."
  [field-schema]
  (timbre/info "get-first-enum-value for schema:" (m/form field-schema))
  (let [schema-type (m/type field-schema)
        actual-schema (if (= :maybe schema-type)
                        (let [children (m/children field-schema)]
                          (when (seq children) (first children)))
                        field-schema)]
    (if (and actual-schema (= :enum (m/type actual-schema)))
      (let [options (m/children actual-schema)]
        (timbre/info "Enum options:" options "for schema" (m/form actual-schema))
        (when (seq options)
          (first options)))
      (do
        (timbre/info "Not an enum or valid maybe-enum:" (m/form actual-schema))
        nil))))

;; Forward declare for mutual recursion
(declare apply-enum-defaults-to-data-impl)

(defn- apply-enum-defaults-to-data-impl [data schema current-path]
  (if-not (= :map (m/type schema))
    (do
      (timbre/warn "apply-enum-defaults-to-data-impl called with non-map schema at path" current-path "Schema type:" (m/type schema))
      data)
    (reduce-kv
     (fn [processed-data field-key field-schema-entry-wrapper] ;; field-schema-entry-wrapper is typically [:schema {:optional true} actual-schema-form] or just actual-schema-form
       (let [field-path (conj current-path field-key)
             current-value (get data field-key) ; Get value from original data for this level (or data being built up)
             ;; Resolve the actual schema definition from the entry
             ;; m/entries gives [key schema optional? props]. The 'schema' here is the schema definition.
             field-schema (m/schema (if (vector? field-schema-entry-wrapper) ; Handle potential :schema wrapper
                                       (if (= :schema (first field-schema-entry-wrapper))
                                         (nth field-schema-entry-wrapper 2)
                                         field-schema-entry-wrapper)
                                       field-schema-entry-wrapper))]

         (cond
           ;; Case 1: Value is nil, try to apply enum default or recurse for map
           (nil? current-value)
           (if-let [default-enum (get-first-enum-value field-schema)]
             (do
               (timbre/info "PATH:" field-path "- Value is nil. Applying enum default:" default-enum)
               (assoc processed-data field-key default-enum))
             (if (= :map (m/type field-schema))
               (do
                 (timbre/info "PATH:" field-path "- Value is nil. Field is a map. Recursing with {} for this map.")
                 ;; Ensure the key exists with an empty map before recursing if it was entirely nil,
                 ;; then recurse on that empty map.
                 (assoc processed-data field-key (apply-enum-defaults-to-data-impl {} field-schema field-path)))
               (do
                 (timbre/info "PATH:" field-path "- Value is nil. Not an enum, not a map. No default applied. Schema type:" (m/type field-schema))
                 processed-data))) ; No change if not enum and not map

           ;; Case 2: Value exists and is a map, and schema is a map, recurse
           (and (map? current-value) (= :map (m/type field-schema)))
           (do
             (timbre/info "PATH:" field-path "- Value is a map. Recursing.")
             (assoc processed-data field-key (apply-enum-defaults-to-data-impl current-value field-schema field-path)))

           ;; Case 3: Value exists and is not a map (or schema is not a map), keep existing value
           :else
           (do
             (timbre/info "PATH:" field-path "- Value exists ("(type current-value)"), not a map requiring recursion or already defaulted. Keeping value:" current-value)
             ;; Ensure to assoc the current value to processed-data if it wasn't nil,
             ;; otherwise reduce-kv might drop fields that don't match above conditions.
             ;; However, starting reduce-kv with `data` handles this.
             processed-data))))
     data ; Start with the original data map for this level
     (->> schema (m/entries {:preserve-entry-properties true}) (into {}))))) ; Iterate over actual schema entries

(defn apply-enum-defaults-to-data
  "Public wrapper function for applying enum defaults."
  [data schema]
  (timbre/info "Starting apply-enum-defaults-to-data for schema type:" (m/type schema) "with data:" data)
  (if (nil? data)
    (apply-enum-defaults-to-data-impl {} schema [])
    (apply-enum-defaults-to-data-impl data schema [])))
