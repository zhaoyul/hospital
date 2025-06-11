(ns hc.hospital.natural-language-generators
  (:require [clojure.string :as str]
            [malli.core :as m]
            [taoensso.timbre :as timbre :refer [log warn error info spy]]
            [hc.hospital.specs.assessment-complete-cn-spec :as cn-specs]))

(defonce control-word-config
  {:有无 {:positive :有,
          :negative :无,
          :label-suffix "史",
          :positive-prefix "",
          :negative-prefix "无",
          :omit-if-negative true} ; Default to omit negative "有无" unless for specific conditions
   :是否 {:positive :是,
          :negative :否,
          :label-suffix "",
          :positive-prefix "",
          :negative-prefix "非",
          :omit-if-negative true}
   :状态 {:positive :异常,
          :negative :正常,
          :label-suffix "状态",
          :positive-prefix "",
          :negative-prefix "",
          :omit-if-negative false} ; Usually we want to state "XX状态：正常"
   ;; Specific override for certain fields where "无XX史" is desired
   :心脏起搏器植入史 {:positive :有, :negative :无, :label-suffix "史", :negative-prefix "无", :omit-if-negative true}
   ;; Add other fields that need explicit negation if their :有无 is :无
   })

(defn is-val-present?
  [value]
  (cond
    (nil? value) false
    (string? value) (not (str/blank? value))
    (coll? value) (seq value)
    :else true))

(defn schema-key->display-label
  [field-key]
  (when field-key
    (-> (name field-key)
        (str/replace #"-" " ")
        (str/replace #"Spec$" "")
        (str/capitalize))))

(defn format-value
  "Formats a single atomic value into a string for display."
  [value _field-schema]
  (cond
    (nil? value) ""
    (keyword? value) (name value)
    (boolean? value) (if value "是" "否")
    :else (str value)))

(declare generate-description-parts parts->hiccup)

(defn generate-description-parts
  "Recursively generates a vector of 'parts' from data and schema.
   Part types: :statement, :key-value, :list-items, :nested-group, :atomic-value"
  [data schema]
  (let [actual-schema (m/deref schema)
        schema-type (m/type actual-schema)]
    (cond
      (= :maybe (m/type schema))
      (when (is-val-present? data)
        (generate-description-parts data (first (m/children schema))))

      (= :map schema-type)
      (when (is-val-present? data)
        (let [entries (m/entries actual-schema)
              control-key-entry (first (filter #(contains? control-word-config (first %)) entries))
              is-controlled-negative? (if control-key-entry
                                        (let [[ctrl-key _] control-key-entry ; ctrl-schema-wrapper not needed here
                                              ctrl-data (get data ctrl-key)
                                              ctrl-def (get control-word-config ctrl-key)]
                                          (and (= ctrl-data (:negative ctrl-def))
                                               (:omit-if-negative ctrl-def true)))
                                        false)]
          (if is-controlled-negative?
            nil
            (let [parts (reduce
                         (fn [acc [field-key entry-schema-wrapper entry-optional? entry-properties]] ; Destructure entry correctly
                           (let [entry-schema (m/schema entry-schema-wrapper)
                                 entry-data (get data field-key)
                                 label (schema-key->display-label field-key)]
                             (if-not (is-val-present? entry-data)
                               acc
                               (cond
                                 ;; NEW Condition: Check if entry-data is a map with only a :描述 key
                                 (and (map? entry-data)
                                      (= 1 (count (keys entry-data)))
                                      (contains? entry-data :描述)
                                      (not (or (= field-key :详情) (= field-key :描述))))
                                 (let [desc-val (get entry-data :描述)
                                       ;; Attempt to get schema for the :描述 value itself
                                       desc-val-schema (when entry-schema
                                                         (when-let [map-entries (m/entries entry-schema)]
                                                           (when-let [desc-entry-tuple (first (filter #(= :描述 (first %)) map-entries))]
                                                             (m/schema (second desc-entry-tuple)))))
                                       formatted-desc-val (format-value desc-val desc-val-schema)]
                                   (if (is-val-present? formatted-desc-val)
                                     (conj acc {:type :key-value :label label :value formatted-desc-val})
                                     acc))

                                 ;; Existing condition for when field-key IS :详情 or :描述
                                 (or (= field-key :详情) (= field-key :描述))
                                 (if (map? entry-data)
                                   (into acc (generate-description-parts entry-data entry-schema))
                                   (let [val-str (format-value entry-data entry-schema)]
                                     (if (is-val-present? val-str)
                                       (conj acc {:type :key-value :label label :value val-str})
                                       acc)))

                                 ;; Existing control word logic
                                 (some? (get control-word-config field-key)) ; Check if control-def exists
                                 (let [control-def (get control-word-config field-key)
                                       positive-val (:positive control-def)
                                       negative-val (:negative control-def)
                                       label-suffix (:label-suffix control-def "")
                                       full-label (str label label-suffix)
                                       part-base {:original-key field-key :label label :prefix (:positive-prefix control-def "")}]
                                   (cond
                                     (= entry-data positive-val)
                                     (conj acc (assoc part-base :type :statement :positive true))
                                     (= entry-data negative-val)
                                     (if (get control-def :omit-if-negative true)
                                       acc
                                       (conj acc (assoc part-base :type :statement :positive false :prefix (:negative-prefix control-def ""))))
                                     :else ; Data doesn't match positive or negative, treat as key-value
                                     (conj acc {:type :key-value :label full-label :value (format-value entry-data entry-schema)})))

                                 ;; Default handling for nested groups / key-values (when no other condition met)
                                 :else
                                 (let [child-parts (generate-description-parts entry-data entry-schema)]
                                   (if (seq child-parts)
                                     (conj acc {:type :nested-group :label label :content child-parts})
                                     (let [formatted-atomic-val (format-value entry-data entry-schema)]
                                       (if (is-val-present? formatted-atomic-val)
                                         (conj acc {:type :key-value :label label :value formatted-atomic-val})
                                         acc))))))))
                         [] ; initial value for reduce
                         entries)] ; collection for reduce
              (when (seq parts) parts)))))

      (= :vector schema-type)
      (when (is-val-present? data)
        (let [element-schema (first (m/children actual-schema))
              processed-item-part-groups (map #(generate-description-parts % element-schema) data)]
          (let [valid-item-part-groups (filterv seq processed-item-part-groups)]
            (when (seq valid-item-part-groups)
              (if (every? (fn [pg] (and (= 1 (count pg)) (= :atomic-value (:type (first pg)))))
                          valid-item-part-groups)
                [{:type :list-items :items (mapv #(:value (first %)) valid-item-part-groups)}]
                (reduce into [] valid-item-part-groups))))))

      :else
      (when (is-val-present? data)
        (let [val-str (format-value data actual-schema)]
          (when (is-val-present? val-str)
            [{:type :atomic-value :value val-str}]))))))

;; --- Hiccup Rendering ---
(declare render-hypertension-details) ; Specific formatter

(defn- render-part [part]
  "Renders a single part into its Hiccup representation."
  (when part
    (let [{:keys [type label value items positive content prefix original-key]} part]
      (condp = type
        :statement
        (let [control-def (get control-word-config original-key)
              label-suffix (or (:label-suffix control-def) "")
              base-label (or label (schema-key->display-label original-key)) ; Ensure label is present
              display-label (str base-label label-suffix)]
          (if positive
             [:div.statement.positive [:strong display-label "："]] ;; Positive statement always ends with a colon
            (when-not (get control-def :omit-if-negative true)
              (let [neg-prefix (or prefix (:negative-prefix control-def "无"))]
                [:div.statement.negative [:strong neg-prefix display-label "。"]]))))

        :key-value
        (when-not (str/blank? (str value))
          [:div.key-value [:strong label "："] [:span value]])

        :list-items
        (when (seq items)
          [:ul.list-items {:style {:list-style-type "disc" :margin-left "20px" :padding-left "0px"}}
           (for [item items] [:li {:key (str item)} item])])

        :nested-group
        (when (seq content)
          (cond
            ;; Specific renderers for complex groups
            (= label "高血压") (render-hypertension-details content)
            ;; Add other custom group renderers here if needed
            ;; (= label "心律失常") (render-arrhythmia-details content)
            :else ;; Default rendering for nested groups
            [:div.nested-group {:style {:margin-left "15px"}}
             (when label [:div [:strong label "："]])
             (parts->hiccup content)]))

        :atomic-value
        [:span value]

        (let [fallback-content (or value items content)]
          (when fallback-content
            [:span (str fallback-content)]))
        ))))

(defn parts->hiccup
  "Receives a vector of 'text parts' and assembles them into a Hiccup structure."
  [parts]
  (when (seq parts)
    (let [hiccup-elements (->> parts
                               (mapv render-part)
                               (filterv identity))]
      (when (seq hiccup-elements)
        (if (= 1 (count hiccup-elements))
          (first hiccup-elements)
          (into [:<>] hiccup-elements)
          )))))

;; --- Custom Formatters for Specific Complex Fields ---
(defn render-hypertension-details [hypertension-parts]
  ;; hypertension-parts is a vector of parts for 分级, 病史时长, 治疗 etc.
  (let [parts-map (into {} (map (fn [p] [(:label p) p]) hypertension-parts))
        grading (get-in parts-map ["分级" :value])
        history-duration (get-in parts-map ["病史时长" :value])
        treatment-group-content (get-in parts-map ["治疗" :content])

        desc-parts (cond-> []
                     (is-val-present? grading) (conj (str "分级为" grading))
                     (is-val-present? history-duration) (conj (str "病史时长" history-duration)))
        desc (if (seq desc-parts)
               (str/join "，" desc-parts)
               "")]

    [:div.hypertension-details
     (when (is-val-present? desc)
       [:div desc "。"])
     (when (seq treatment-group-content)
       [:div {:style {:margin-left "15px"}}
        [:strong (schema-key->display-label :治疗) "："]
        (parts->hiccup treatment-group-content)])]))


(defn generate-summary-component
  "Generates a Hiccup component for the summary of the given data and schema."
  [data schema system-name-key]
  (if-not (and data schema system-name-key)
    (do (warn "generate-summary-component called with nil data, schema, or system-name-key") nil)
    (let [system-label (schema-key->display-label system-name-key)
          parts (generate-description-parts data schema)]
      (if (seq parts) ; Only proceed if there are meaningful parts
        (let [content-hiccup (parts->hiccup parts)]
          (when content-hiccup ; Further ensure hiccup generation was successful
            [:div.summary-section {:key (name system-name-key)
                                   :style {:padding "10px" :border "1px solid #ddd" :margin-bottom "10px" :border-radius "4px"}}
             [:h3 {:style {:font-size "16px" :font-weight "bold" :margin-top "0" :margin-bottom "8px"}}
              system-label "："]
             content-hiccup]))
        ;; If (seq parts) is nil, the if form returns nil, hiding the section.
        nil))))
