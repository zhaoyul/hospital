(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

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
