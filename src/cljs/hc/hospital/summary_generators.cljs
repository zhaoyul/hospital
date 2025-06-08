(ns hc.hospital.summary-generators
  (:require [malli.core :as m]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

;; Forward declaration for mutual recursion
(declare generate-summary-for-entry)

;; Corrected keyword->label to handle string input directly for label.
(defn- keyword->label [k-or-label-str]
  (if (keyword? k-or-label-str)
    (-> (name k-or-label-str) (str/replace #"-" " ") (str/capitalize))
    (str k-or-label-str))) ; If it's already a string (from :label prop), use it directly.


(defn generate-summary-from-spec
  ([system-data system-spec]
   (generate-summary-from-spec system-data system-spec []))
  ([system-data system-spec parent-path]
   (when-not (map? system-data) ; Ensures data is a map for get operations
     (when (seq parent-path) ; Only warn if it's not the top-level data being nil/wrong type
       (timbre/warn "System data is not a map for spec at path:" parent-path "Data:" system-data))
     ;; For top-level call, if system-data is not a map, it's problematic.
     ;; For nested calls, if a sub-field expected to be a map isn't, it's also an issue.
     (if (empty? parent-path) (if (nil? system-data) "无数据" "数据格式错误") ""))

   (if (nil? system-data) ; If, after the above check, system-data is nil (e.g. top-level was nil)
     (if (empty? parent-path) "无数据" "") ; Return "无数据" for top-level, "" for nested
     (if-not (= :map (m/type system-spec))
       (do (timbre/warn "Spec is not a map type for data at path:" parent-path "Data:" system-data)
           (if (empty? parent-path) "Spec定义错误" "")) ; Return "Spec定义错误" for top-level, "" for nested
       (let [entries (m/entries system-spec {:preserve-entry-properties true})
             summary-parts (transient [])]
         (doseq [[field-key field-schema-entry _ entry-props] entries]
           (let [current-path (conj parent-path field-key)
                 field-schema (m/schema field-schema-entry) ; Get the actual schema
                 field-value (get system-data field-key)]
             (generate-summary-for-entry summary-parts field-key field-schema entry-props field-value current-path system-data)))
         (let [final-parts (persistent! summary-parts)]
           (if (empty? final-parts)
             (if (empty? parent-path) "未见明显异常" "") ; Return "" for sub-summaries if they are empty
             (str/join "; " final-parts))))))))

(defn- generate-summary-for-entry [summary-parts field-key field-schema entry-props field-value current-path system-data]
  (let [label-source (or (:label entry-props) field-key)
        field-label (keyword->label label-source)
        schema-type (m/type field-schema)]
    (cond
      ;; Skip if value is nil, or an empty collection, or a blank string.
      (or (nil? field-value)
          (and (coll? field-value) (empty? field-value))
          (and (string? field-value) (str/blank? field-value)))
      nil

      (= :map schema-type)
      (let [nested-summary (generate-summary-from-spec field-value field-schema current-path)]
        (when-not (str/blank? nested-summary) ; Only add if nested summary is not blank
          (conj! summary-parts (str field-label " (" nested-summary ")"))))

      (= :vector schema-type)
      (let [;; Example: try to get element type, though Malli doesn't make this super easy for :vector
            ;; This is a simplification; robustly getting element type might need m/children or similar
            ;; For now, we'll rely on the content of field-value.
            display-value (cond
                            (every? #(or (string? %) (keyword? %) (number? %)) field-value)
                            (str/join ", " (map str field-value)) ; Use str to handle keywords/numbers

                            (every? map? field-value) ; Vector of maps
                            (str "[共 " (count field-value) " 项详细内容]") ; Placeholder

                            :else (str field-value))]
        (when-not (str/blank? display-value)
          (conj! summary-parts (str field-label ": " display-value))))

      (= :enum schema-type)
      (conj! summary-parts (str field-label ": " (name field-value)))

      (or (= :boolean schema-type) (= :malli.core/boolean schema-type)) ; Check for boolean type
      (conj! summary-parts (str field-label ": " (if field-value "是" "否")))

      :else ; Default for string, number, etc.
      (let [display-value (str field-value)]
         (when-not (str/blank? display-value) ; Ensure non-blank before adding
            (conj! summary-parts (str field-label ": " display-value)))))))
