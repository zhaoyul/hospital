(ns hc.hospital.natural-language-generators
  "根据 Malli Schema 生成自然语言描述的工具。"
  (:require [clojure.string :as str]
            [malli.core :as m]
            [hc.hospital.specs.assessment-complete-cn-spec :as cn-specs]))

(defonce control-word-config
  {:有无 {:positive :有,
          :negative :无,
          :label-suffix "", ; 由 "史" 修改为此
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
   :心脏起搏器植入史 {:positive :有, :negative :无, :label-suffix "", :negative-prefix "无", :omit-if-negative true} ; label-suffix 修改为 ""
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

(defn- normalize-entry-data
  "一个辅助函数，用于预处理特定的映射结构数据条目。
  它主要处理包含 :内容 或 :结果 键的映射：
  - 如果数据是类似 {:内容 val} 或 {:结果 val} 的映射，且 val 被视为空值（例如 nil、空字符串），
    则此函数返回 nil，从而有效地将该条目标记为待省略。
  - 如果数据是类似 {:内容 val} 或 {:结果 val} 的映射，且 val 存在，则提取并返回 val。
  其他数据类型或不包含这些特定键的映射结构将原样返回。"
  [data]
  (if (map? data)
    (cond
      ;; Check for {:内容 val} where val is not present
      (and (contains? data :内容) (not (is-val-present? (:内容 data)))) nil
      ;; Check for {:结果 val} where val is not present
      (and (contains? data :结果) (not (is-val-present? (:结果 data)))) nil

      ;; If {:内容 val} and val is present, return val
      (contains? data :内容) (:内容 data)
      ;; If {:结果 val} and val is present, return val
      (contains? data :结果) (:结果 data)

      :else data)
    data))

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
                          (fn [acc [field-key entry-schema-wrapper entry-optional? entry-properties]]
                            (let [entry-schema (m/schema entry-schema-wrapper)
                                  original-entry-data (get data field-key)
                                  label (schema-key->display-label field-key)]
                              (if (or (= original-entry-data {:有无 "无"})
                                      (= original-entry-data {:有无 :无}))
                                acc
                                (let [entry-data (normalize-entry-data original-entry-data)]
                                  (if-not (is-val-present? entry-data)
                                    acc
                                    (cond
                                      ;; Condition for map with only a :描述 key (from original logic)
                                      (and (map? entry-data) ; Check normalized entry-data
                                           (= 1 (count (keys entry-data)))
                                           (contains? entry-data :描述)
                                           (not (or (= field-key :详情) (= field-key :描述))))
                                      (let [desc-val (:描述 entry-data) ; Use normalized entry-data
                                            desc-val-schema (when entry-schema
                                                              (when-let [map-entries (m/entries entry-schema)]
                                                                (when-let [desc-entry-tuple (first (filter #(= :描述 (first %)) map-entries))]
                                                                  (m/schema (second desc-entry-tuple)))))
                                            formatted-desc-val (format-value desc-val desc-val-schema)]
                                        (if (is-val-present? formatted-desc-val)
                                          (conj acc {:type :key-value :label label :value formatted-desc-val})
                                          acc))

                                      ;; Condition for when field-key IS :详情 or :描述
                                      (or (= field-key :详情) (= field-key :描述))
                                      (if (map? entry-data) ; Use normalized entry-data
                                        (into acc (generate-description-parts entry-data entry-schema)) ; Pass normalized
                                        (let [val-str (format-value entry-data entry-schema)] ; Pass normalized
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
                                          ;; Use normalized entry-data for comparison
                                          (= entry-data positive-val)
                                          (conj acc (assoc part-base :type :statement :positive true))
                                          (= entry-data negative-val)
                                          (if (get control-def :omit-if-negative true)
                                            acc
                                            (conj acc (assoc part-base :type :statement :positive false :prefix (:negative-prefix control-def ""))))
                                          :else ; Data doesn't match positive or negative, treat as key-value
                                          ;; format-value expects the actual value, not a map containing it (unless schema expects map)
                                          (conj acc {:type :key-value :label full-label :value (format-value entry-data entry-schema)})))

                                      ;; Default handling for nested groups / key-values (when no other condition met)
                                      :else
                                      (let [child-parts (generate-description-parts entry-data entry-schema)]
                                        (if (seq child-parts)
                                          (conj acc {:type :nested-group :label label :content child-parts})
                                          (let [formatted-atomic-val (format-value entry-data entry-schema)]
                                            (if (is-val-present? formatted-atomic-val)
                                              (conj acc {:type :key-value :label label :value formatted-atomic-val})
                                              acc))))) ; closes: if is-val, let formatted, if seq, let child-parts
                                  ) ; close if-not
                                ) ; close let entry-data
                              ) ; close if original-entry-data
                            ) ; close let entry-schema
                          ) ; close fn
                          [] ; initial value
                          entries ; collection
                        ) ; close reduce
                       ] ; end of binding vector for let [parts ...]
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
        (when (seq content) ; C4.L1
          (cond ; C4.L2
            ;; Specific renderers for complex groups
            (= label "高血压") (render-hypertension-details content)
            ;; Add other custom group renderers here if needed
            ;; (= label "心律失常") (render-arrhythmia-details content)
            :else ;; 默认的嵌套组渲染逻辑
            (let [child-parts content ; 'content' 是子部件的向量
                  num-child-parts (count child-parts)]
              (if (and (= 1 num-child-parts)
                       (let [child-part (first child-parts)]
                         (or (= (:type child-part) :atomic-value)
                             (and (= (:type child-part) :key-value)
                                  (string? (:value child-part)))))) ; 检查子部件是否适合内联 ; if 的条件
                ;; --- 新的内联渲染逻辑 --- (if 的 then 分支)
                (let [child-part (first child-parts)
                      parent-label label] ; 'label' 是父 group 的标签
                  [:div.nested-group-inline ;; 可以用新 class 区分，或只用 :div
                   (if parent-label
                     (if (= (:type child-part) :atomic-value)
                       ;; 父标签：子原子值 (例如：父：值)
                       [:span [:strong parent-label "："] " " (:value child-part)]
                       ;; 父标签：子标签：子值 (例如：父：子标签：值)
                       [:span [:strong parent-label "：" (:label child-part) "："] " " (:value child-part)])
                     ;; 如果没有父标签 (通常 :nested-group 都有父标签，但作为健壮性考虑)
                     (if (= (:type child-part) :atomic-value)
                       [:span (:value child-part)]
                       [:span [:strong (:label child-part) "："] " " (:value child-part)])
                   ) ; 关闭 (if parent-label...)
                  ]) ; 关闭 [:div.nested-group-inline ...]
                ;; --- 回退到旧的多行渲染逻辑 --- (if 的 else 分支)
                (do ;; 使用 do 来明确这里是 if 的 else 分支
                  [:div.nested-group {:style {:margin-left "15px"}}
                   (when label [:div [:strong label "："]])
                   (parts->hiccup content)]) ; 关闭 [:div.nested-group ...]
              ) ; 关闭 (if (and (= 1 num-child-parts) ...))
            ) ; 关闭 (let [child-parts ...])
          ) ; 关闭 (cond ...)
        ) ; 关闭 (when (seq content) ...)

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
