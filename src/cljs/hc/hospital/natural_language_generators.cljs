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
          :negative-prefix "无"}
   :是否 {:positive :是,
          :negative :否,
          :label-suffix "",
          :positive-prefix "",
          :negative-prefix "非"}
   :状态 {:positive :异常,
          :negative :正常,
          :label-suffix "状态",
          :positive-prefix "",
          :negative-prefix ""}
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
  [value field-schema]
  (cond
    (nil? value) ""
    (keyword? value) (schema-key->display-label value)
    (vector? value) (str/join "、" (map #(format-value % nil) value))
    (boolean? value) (if value "是" "否")
    :else (str value)))

(declare generate-description-parts)

(defn generate-description-parts
  "递归函数，接收数据和 Malli schema，输出一个“文本部分”的 vector。
   Part types: :statement, :key-value, :list-items, :nested-group"
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
              parts (reduce
                     (fn [acc [field-key entry-schema-wrapper props-map]]
                       (let [entry-schema (m/schema entry-schema-wrapper)
                             entry-data (get data field-key)
                             label (schema-key->display-label field-key)
                             entry-props (:properties entry-schema-wrapper)]
                         (if-not (is-val-present? entry-data)
                           acc
                           (if-let [control-def (get control-word-config field-key)]
                             (let [positive-val (:positive control-def)
                                   negative-val (:negative control-def)
                                   label-suffix (:label-suffix control-def "")
                                   full-label (str label label-suffix)
                                   details-parts nil]  ; Simplified, details handled by subsequent parts
                               (cond
                                 (= entry-data positive-val)
                                 (conj acc {:type :statement :positive true :label full-label :prefix (:positive-prefix control-def "") :details details-parts})
                                 (= entry-data negative-val)
                                 (if (:omit-if-negative control-def)
                                   acc
                                   (conj acc {:type :statement :positive false :label full-label :prefix (:negative-prefix control-def "")}))
                                 :else
                                 (conj acc {:type :key-value :label full-label :value (format-value entry-data entry-schema)})))
                             ;; Not a control field
                             (let [child-parts (generate-description-parts entry-data entry-schema)]
                               (if (seq child-parts)
                                 (conj acc {:type :nested-group :label label :content child-parts})
                                 (if-let [formatted-atomic-val (format-value entry-data entry-schema)]
                                   (if (str/blank? formatted-atomic-val)
                                     acc
                                     (conj acc {:type :key-value :label label :value formatted-atomic-val}))
                                   acc)))))))
                     []
                     entries)]
          (when (seq parts) parts)))

      (= :vector schema-type)
      (when (is-val-present? data)
        (let [element-schema (first (m/children actual-schema))
              item-parts (->> data
                              (mapcat #(generate-description-parts % element-schema))
                              (filter is-val-present?))]
          (when (seq item-parts)
            (if (every? #(= (:type %) :atomic-value) item-parts)
              [{:type :list-items :items (map :value item-parts)}]
              item-parts))))

      :else
      (when (is-val-present? data)
        (let [val-str (format-value data actual-schema)]
          (when (is-val-present? val-str)
            [{:type :atomic-value :value val-str}]))))))


(defn- join-sentences [sentences]
  (->> sentences
       (map str/trim)
       (filter #(not (str/blank? %)))
       (map #(if (re-find #"[。！？]$" %) % (str % "。")))
       (str/join " ")))

(defn- format-key-value-pair [label value]
  (str label "：" value))

(defn- format-list-items [items]
  (if (and items (seq items))
    (str/join "、" items)
    ""))

(defn- process-statement-part [part]
  (let [{:keys [positive label prefix]} part]
    (str prefix (when (and prefix (not (str/blank? prefix))) " ") label)))


(defn parts->natural-language
  "接收“文本部分”的 vector，将其组装成自然语言描述字符串。"
  [parts]
  (if (empty? parts)
    ""
    (let [processed-strings (volatile! []) ;; Use volatile vector
          add-str! (fn [s] (when (is-val-present? s) (vswap! processed-strings conj (str/trim s))))
          peek-last-str #(peek @processed-strings)

          ensure-ends-with-punctuation! (fn []
                                          (let [current-coll @processed-strings]
                                            (when (seq current-coll)
                                              (let [last-str (peek current-coll)]
                                                (when (and last-str (not (some #{(last last-str)} [\. \。 \， \、 \：])) ) ; Corrected to char literals
                                                  (vswap! processed-strings assoc (- (count current-coll) 1) (str last-str "。")))))))]

      (doseq [[idx part] (map-indexed vector parts)]
        (let [{:keys [type label value items positive content prefix]} part
              prev-part (get parts (dec idx) nil)]

          (when (and (pos? idx)
                     (seq @processed-strings) ; Check if there's something to add punctuation to
                     (or (= type :statement)
                         (and (= type :nested-group) (not (= (:type prev-part) :statement)))
                         (and (= type :key-value) (not= (:type prev-part) :statement))))
            (let [last-str-val (peek @processed-strings)]
              (when-not (or (str/ends-with? last-str-val "：")
                            (str/ends-with? last-str-val "，")
                            (str/ends-with? last-str-val "、"))
                (ensure-ends-with-punctuation!) ; Ensure previous sentence ends, then add comma if needed
                (vswap! processed-strings assoc (- (count @processed-strings) 1) (str (peek @processed-strings) "，"))
                )))


          (condp = type
            :statement
            (let [stmt (process-statement-part part)]
              ;; If previous string doesn't end with punctuation, add one.
              (when (and (pos? idx) (seq @processed-strings) (not (re-find #"[。！？：]$" (peek @processed-strings))))
                 (ensure-ends-with-punctuation!))
              (add-str! stmt)
              (let [next-part (get parts (inc idx) nil)]
                (when (and positive
                           next-part
                           (or (= (:type next-part) :nested-group) ;; Changed from :nested-map-details
                               (= (:type next-part) :list-items)
                               (= (:type next-part) :key-value))) ;; If a statement is followed by a simple KV
                  (let [last-str (peek-last-str)]
                    (if (and last-str (not (str/ends-with? last-str "：")))
                       (vswap! processed-strings assoc (- (count @processed-strings) 1) (str last-str "，具体包括："))
                       (add-str! "具体包括："))))))

            :key-value
            (let [kv-str (format-key-value-pair label value)]
              (add-str! kv-str))

            :list-items
            (let [list-str (format-list-items items)]
              (when (is-val-present? list-str)
                (if (and prev-part (str/ends-with? (peek @processed-strings) "："))
                   (add-str! (str " " list-str)) ; Add space if after colon
                   (add-str! list-str))))

            :nested-group
            (let [nested-str (parts->natural-language content)]
              (when (is-val-present? nested-str)
                (add-str! (str label "：" nested-str))))

            :atomic-value
            (add-str! (str value))

            (warn "Unhandled part type in parts->natural-language:" type part)
            )))

      (join-sentences @processed-strings))))

(defn generate-natural-language-summary
  "Generates a natural language summary string for the given data and schema."
  [data schema system-name-key]
  (if-not (and data schema system-name-key)
    (do (warn "generate-natural-language-summary called with nil data, schema, or system-name-key") "")
    (let [system-label (schema-key->display-label system-name-key)
          parts (generate-description-parts data schema)]
      (if (seq parts)
        (let [language (parts->natural-language parts)]
          (if (str/blank? language)
            (str system-label "：无特殊或异常情况记录。")
            (str system-label "：" language))) ;; join-sentences now adds trailing punctuation
        (str system-label "：无特殊或异常情况记录。")
        ))))
