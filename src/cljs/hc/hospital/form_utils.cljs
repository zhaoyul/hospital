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
        (timbre/info "Enum options:" options)
        (when (seq options)
          (first options)))
      (do
        (timbre/info "Not an enum or valid maybe-enum:" (m/form actual-schema))
        nil))))

(declare apply-enum-defaults-to-data*) ;; Forward declare for mutual recursion if needed, though current structure is linear

(defn- apply-enum-defaults-to-data-impl [data schema current-path-vec]
  (if-not (and (map? data) schema (= :map (m/type schema)))
    data
    (let [entries (m/entries schema {:preserve-entry-properties true})]
      (reduce
        (fn [acc-data [field-key field-schema-entry _ _entry-props]]
          (let [current-val (get acc-data field-key)
                field-schema (m/schema field-schema-entry) ; Get the actual schema for the field
                new-path (conj current-path-vec field-key)]
            (cond
              (nil? current-val)
              (if-let [default-enum-val (get-first-enum-value field-schema)]
                (do
                  (timbre/info "Defaulting for path:" new-path "to value:" default-enum-val)
                  (assoc acc-data field-key default-enum-val))
                (do
                  (timbre/info "No default enum for path:" new-path "Schema type:" (m/type field-schema))
                  acc-data))

              (and (map? current-val) (= :map (m/type field-schema)))
              (assoc acc-data field-key (apply-enum-defaults-to-data-impl current-val field-schema new-path))

              :else
              acc-data)))
        data
        entries))))

(defn apply-enum-defaults-to-data
  "Public wrapper function for applying enum defaults."
  [data schema]
  (timbre/info "Starting apply-enum-defaults-to-data for schema:" (m/form schema) "with data:" data)
  (apply-enum-defaults-to-data-impl data schema []))
