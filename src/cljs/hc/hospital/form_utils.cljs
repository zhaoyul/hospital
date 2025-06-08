(ns hc.hospital.form-utils
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(defn get-first-enum-value
  "Extracts the first enum value from a Malli schema.
   Handles :enum and [:maybe :enum] schemas."
  [field-schema]
  (let [actual-schema (if (= :maybe (m/type field-schema))
                        (-> field-schema m/children first) ; Get the child schema of :maybe
                        field-schema)]
    (when (and actual-schema (= :enum (m/type actual-schema)))
      (let [options (m/children actual-schema)] ; For :enum, children are the values
        (when (seq options)
          (first options))))))


(defn apply-enum-defaults-to-data
  "Recursively applies default values for nil fields that are enums,
   using the first value of the enum as the default.
   Also handles nested map schemas."
  [data schema]
  (if-not (and (map? data) schema (= :map (m/type schema)))
    ;; If data is not a map, or schema is not a map schema, return data as is.
    ;; This also handles cases where data might be nil for a non-map schema part.
    data
    (let [entries (m/entries schema {:preserve-entry-properties true})]
      (reduce
       (fn [acc-data [field-key field-schema-entry _ entry-props]]
         (let [current-val (get acc-data field-key)
               field-schema (m/schema field-schema-entry) ; Get the actual schema for the field
               schema-type (m/type field-schema)]

           (cond
             ;; If current value is nil, try to apply enum default
             (nil? current-val)
             (if-let [default-enum-val (get-first-enum-value field-schema)]
               (assoc acc-data field-key default-enum-val)
               acc-data)

             ;; If current value is a map and field schema is a map, recurse
             (and (map? current-val) (= :map schema-type))
             (assoc acc-data field-key (apply-enum-defaults-to-data current-val field-schema))

             ;; Otherwise, keep existing value
             :else
             acc-data)))
       data
       entries))))
