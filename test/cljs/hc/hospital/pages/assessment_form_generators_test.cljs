(ns hc.hospital.pages.assessment-form-generators-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [hc.hospital.pages.assessment-form-generators :as form-gen]
            [malli.core :as m]
            [hc.hospital.specs.assessment-complete-cn-spec :as assessment-specs]
            [reagent.core :as r]
            ["antd" :refer [Form]])) ;; Added Form for (.-useWatch Form)

(deftest keyword-to-label-test
  (testing "Simple keyword"
    (is (= "Test" (form-gen/keyword->label :test))))
  (testing "Keyword with hyphens"
    (is (= "My keyword" (form-gen/keyword->label :my-keyword))))
  (testing "Keyword with multiple hyphens"
    (is (= "Another example here" (form-gen/keyword->label :another-example-here))))
  (testing "Namespaced keyword"
    ;; Assuming the current implementation of keyword->label takes the name part of the keyword
    (is (= "Name" (form-gen/keyword->label :user/name))))
  (testing "Nil input"
    (is (nil? (form-gen/keyword->label nil))))
  (testing "Already capitalized keyword"
    (is (= "Capitalized" (form-gen/keyword->label :Capitalized)))))

;; --- Test Schemas for Conditional Map Logic ---
(def conditional-map-schema
  [:map
   [:condition [:enum :option1 :option2]]
   [:option1 [:map [:field1 :string]]]
   [:option2 [:map [:field2 :int]]]])

(def conditional-map-schema-val-is-keyword
  [:map
   [:condition [:enum :option1 :option2]]
   [:option1 [:map [:field1 :string]]]
   [:option2 [:map [:field2 :int]]]])


(def simple-map-schema
  [:map [:name :string] [:age :int]])

(def not-a-map-schema :string)

(def empty-map-schema [:map])

(def map-without-enum-first-child
  [:map
   [:field1 :string]
   [:condition [:enum :a :b]]
   [:a [:map [:x :int]]]
   [:b [:map [:y :int]]]])

;; --- Tests for is-conditional-map-schema? ---
(deftest is-conditional-map-schema-test
  (testing "Schema is a valid conditional map"
    (is (true? (form-gen/is-conditional-map-schema? conditional-map-schema))))
  (testing "Schema is a simple map, not conditional"
    (is (false? (form-gen/is-conditional-map-schema? simple-map-schema))))
  (testing "Schema is not a map type"
    (is (false? (form-gen/is-conditional-map-schema? not-a-map-schema))))
  (testing "Schema is an empty map"
    (is (false? (form-gen/is-conditional-map-schema? empty-map-schema))))
  (testing "Schema is a map but first child is not an enum"
    (is (false? (form-gen/is-conditional-map-schema? map-without-enum-first-child))))
  (testing "Input is nil"
    (is (false? (form-gen/is-conditional-map-schema? nil)))))

;; --- Tests for get-map-schema-conditional-key ---
(deftest get-map-schema-conditional-key-test
  (testing "Valid conditional map schema"
    (is (= :condition (form-gen/get-map-schema-conditional-key conditional-map-schema))))
  (testing "Non-conditional map schema"
    (is (nil? (form-gen/get-map-schema-conditional-key simple-map-schema))))
  (testing "Input is nil"
    (is (nil? (form-gen/get-map-schema-conditional-key nil)))))

;; --- Tests for get-map-schema-conditional-options-map ---
(deftest get-map-schema-conditional-options-map-test
  (testing "Valid conditional map schema"
    (let [expected-options {:option1 [:map [:field1 :string]]
                            :option2 [:map [:field2 :int]]}
          actual-options (form-gen/get-map-schema-conditional-options-map conditional-map-schema)]
      (is (= (set (keys expected-options)) (set (keys actual-options))))
      (is (= (m/schema (get expected-options :option1)) (m/schema (get actual-options :option1))))
      (is (= (m/schema (get expected-options :option2)) (m/schema (get actual-options :option2))))))
  (testing "Valid conditional map schema where enum values are keywords"
    (let [expected-options {:option1 [:map [:field1 :string]]
                            :option2 [:map [:field2 :int]]}
          actual-options (form-gen/get-map-schema-conditional-options-map conditional-map-schema-val-is-keyword)]
      (is (= (set (keys expected-options)) (set (keys actual-options))))
      (is (= (m/schema (get expected-options :option1)) (m/schema (get actual-options :option1))))
      (is (= (m/schema (get expected-options :option2)) (m/schema (get actual-options :option2))))))
  (testing "Non-conditional map schema"
    (is (or (nil? (form-gen/get-map-schema-conditional-options-map simple-map-schema))
            (empty? (form-gen/get-map-schema-conditional-options-map simple-map-schema)))))
  (testing "Input is nil"
    (is (or (nil? (form-gen/get-map-schema-conditional-options-map nil))
            (empty? (form-gen/get-map-schema-conditional-options-map nil))))))

;; --- Test Schemas for is-map-schema-with-conditional-key? ---
(def common-conditional-pattern-schema
  [:map
   [:有无 [:enum :有 :无]]
   [:详情 [:map [:detail1 :string]]]])

(def common-conditional-pattern-optional-detail-schema
  [:map
   [:有无 [:enum :有 :无]]
   [:详情 {:optional true} [:map [:detail1 :string]]]])


(def map-without-youwu-schema
  [:map
   [:foo [:enum :有 :无]]
   [:详情 [:map [:detail1 :string]]]])

(def map-without-detail-schema
  [:map
   [:有无 [:enum :有 :无]]
   [:bar [:map [:detail1 :string]]]])

(def map-youwu-not-enum-schema
  [:map
   [:有无 :string]
   [:详情 [:map [:detail1 :string]]]])

(def map-detail-not-map-schema
  [:map
   [:有无 [:enum :有 :无]]
   [:详情 :string]])

;; --- Tests for is-map-schema-with-conditional-key? ---
(deftest is-map-schema-with-conditional-key-test
  (testing "Schema matches the common conditional pattern"
    (is (true? (form-gen/is-map-schema-with-conditional-key? common-conditional-pattern-schema))))
  (testing "Schema lacks :有无 key"
    (is (false? (form-gen/is-map-schema-with-conditional-key? map-without-youwu-schema))))
  (testing "Schema lacks :详情 key"
    (is (false? (form-gen/is-map-schema-with-conditional-key? map-without-detail-schema))))
  (testing "Schema where :有无 is not an enum"
    (is (false? (form-gen/is-map-schema-with-conditional-key? map-youwu-not-enum-schema))))
  (testing "Schema where :详情 is not a map"
    (is (false? (form-gen/is-map-schema-with-conditional-key? map-detail-not-map-schema))))
  (testing "Input is a simple map schema"
    (is (false? (form-gen/is-map-schema-with-conditional-key? simple-map-schema))))
  (testing "Input is nil"
    (is (false? (form-gen/is-map-schema-with-conditional-key? nil)))))

;; --- Test Schemas for is-date-string-schema? ---
(def date-regex-schema-yyyy-mm-dd [:re #"\d{4}-\d{2}-\d{2}"])
(def date-regex-schema-iso [:re #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?Z?"])
(def date-property-schema (m/schema [:string {:type/date? true}]))
(def string-schema :string)
(def int-schema :int)

;; --- Tests for is-date-string-schema? ---
(deftest is-date-string-schema-test
  (testing "Schema with YYYY-MM-DD regex"
    (is (true? (form-gen/is-date-string-schema? date-regex-schema-yyyy-mm-dd))))
  (testing "Schema with ISO date regex"
    (is (true? (form-gen/is-date-string-schema? date-regex-schema-iso))))
  (testing "Schema with :type/date? property"
    (is (true? (form-gen/is-date-string-schema? date-property-schema))))
  (testing "Simple string schema"
    (is (false? (form-gen/is-date-string-schema? string-schema))))
  (testing "Integer schema"
    (is (false? (form-gen/is-date-string-schema? int-schema))))
  (testing "Input is nil"
    (is (false? (form-gen/is-date-string-schema? nil)))))

;; --- Test Schemas for get-key-optional-status ---
(def map-with-optional-key
  [:map
   [:name :string]
   [:age {:optional true} :int]])

(def map-with-all-required-keys
  [:map
   [:address :string]
   [:city :string]])

;; --- Tests for get-key-optional-status ---
(deftest get-key-optional-status-test
  (testing "Key is optional in the schema"
    (is (true? (form-gen/get-key-optional-status map-with-optional-key :age))))
  (testing "Key is required in the schema"
    (is (false? (form-gen/get-key-optional-status map-with-optional-key :name))))
  (testing "Key is not present in the schema"
    (is (false? (form-gen/get-key-optional-status map-with-optional-key :address))))
  (testing "All keys are required"
    (is (false? (form-gen/get-key-optional-status map-with-all-required-keys :address)))
    (is (false? (form-gen/get-key-optional-status map-with-all-required-keys :city))))
  (testing "Input schema is nil"
    (is (false? (form-gen/get-key-optional-status nil :age))))
  (testing "Input key is nil"
    (is (false? (form-gen/get-key-optional-status map-with-optional-key nil)))))

;; --- Test Schemas for Basic Malli Helpers ---
(def optional-string-schema [:maybe :string])
(def string-with-props-schema [:string {:min 1 :max 10}])
(def enum-schema-strings [:enum "Apple" "Banana" "Cherry"])
(def enum-schema-keywords [:enum :apple :banana :cherry])
(def map-schema-with-children simple-map-schema) ; Reuse existing
(def vector-schema [:vector :string])


;; --- Tests for get-malli-type ---
(deftest get-malli-type-test
  (testing "Get type of a string schema"
    (is (= :string (form-gen/get-malli-type string-schema))))
  (testing "Get type of a map schema"
    (is (= :map (form-gen/get-malli-type simple-map-schema))))
  (testing "Get type of an optional schema"
    (is (= :maybe (form-gen/get-malli-type optional-string-schema))))
  (testing "Get type of an enum schema"
    (is (= :enum (form-gen/get-malli-type enum-schema-keywords))))
  (testing "Input is not a valid schema (e.g., a plain keyword)"
    (is (nil? (form-gen/get-malli-type :not-a-schema))))
  (testing "Input is nil"
    (is (nil? (form-gen/get-malli-type nil)))))

;; --- Tests for get-malli-properties ---
(deftest get-malli-properties-test
  (testing "Schema with properties"
    (is (= {:min 1 :max 10} (form-gen/get-malli-properties string-with-props-schema))))
  (testing "Schema without explicit properties (map schema still has implicit ones)"
    (is (map? (form-gen/get-malli-properties simple-map-schema)))) ; Map schemas have properties, even if empty
  (testing "Schema like :string (no explicit props defined by user)"
    (is (= {} (form-gen/get-malli-properties string-schema))))
  (testing "Input is not a valid schema"
    (is (= {} (form-gen/get-malli-properties :not-a-schema))))
  (testing "Input is nil"
    (is (= {} (form-gen/get-malli-properties nil)))))

;; --- Tests for get-malli-children ---
(deftest get-malli-children-test
  (testing "Children of a map schema"
    (let [children (form-gen/get-malli-children map-schema-with-children)]
      (is (= 2 (count children))) ; :name and :age
      (is (some #(= :name (first %)) children))
      (is (some #(= :age (first %)) children))))
  (testing "Children of an enum schema"
    (is (= [:apple :banana :cherry] (form-gen/get-malli-children enum-schema-keywords))))
  (testing "Children of a vector schema"
    (is (= [:string] (form-gen/get-malli-children vector-schema))))
  (testing "Children of a simple type schema (e.g., :string)"
    (is (empty? (form-gen/get-malli-children string-schema))))
  (testing "Input is not a valid schema"
    (is (empty? (form-gen/get-malli-children :not-a-schema))))
  (testing "Input is nil"
    (is (empty? (form-gen/get-malli-children nil)))))

;; --- Tests for is-optional-schema? ---
(deftest is-optional-schema-test
  (testing "Schema is optional"
    (is (true? (form-gen/is-optional-schema? optional-string-schema))))
  (testing "Schema is not optional"
    (is (false? (form-gen/is-optional-schema? string-schema))))
  (testing "Input is not a valid schema"
    (is (false? (form-gen/is-optional-schema? :not-a-schema))))
  (testing "Input is nil"
    (is (false? (form-gen/is-optional-schema? nil)))))

;; --- Tests for get-enum-options ---
(deftest get-enum-options-test
  (testing "Enum schema with string values"
    (let [expected [{:value "Apple" :label "Apple"}
                    {:value "Banana" :label "Banana"}
                    {:value "Cherry" :label "Cherry"}]]
      (is (= expected (form-gen/get-enum-options enum-schema-strings)))))
  (testing "Enum schema with keyword values"
    (let [expected [{:value "apple" :label "apple"} ; Note: original code converts keywords to (name k)
                    {:value "banana" :label "banana"}
                    {:value "cherry" :label "cherry"}]]
      (is (= expected (form-gen/get-enum-options enum-schema-keywords)))))
  (testing "Schema is not an enum"
    (is (nil? (form-gen/get-enum-options string-schema))))
  (testing "Input is not a valid schema"
    (is (nil? (form-gen/get-enum-options :not-a-schema))))
  (testing "Input is nil"
    (is (nil? (form-gen/get-enum-options nil)))))

;; --- Mocks for Rendering Functions ---
(defn mock-render-text-input [field-schema form-path label-text]
  {:type ::text-input :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-number-input [field-schema form-path label-text]
  {:type ::number-input :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-datepicker [field-schema form-path label-text]
  {:type ::datepicker :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-radio-group [field-schema form-path label-text]
  {:type ::radio-group :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-select [field-schema form-path label-text]
  {:type ::select :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-checkbox-group [field-schema form-path label-text]
  {:type ::checkbox-group :field-schema field-schema :form-path form-path :label-text label-text})

(defn mock-render-map-schema-fields [map-schema parent-form-path form-instance]
  ;; For map, the function returns a vector of items. We are interested in the type.
  ;; The actual render-form-item-from-spec returns hiccup, so we check the first element of the vector.
  ;; For this mock, let's return something identifiable.
  {:type ::map-schema-fields :map-schema map-schema :parent-form-path parent-form-path})

(defn mock-render-conditional-map-section [field-key field-schema parent-form-path form-instance entry-props]
  {:type ::conditional-map-section :field-key field-key :field-schema field-schema :parent-form-path parent-form-path})


;; --- Tests for render-form-item-from-spec ---
(deftest render-form-item-from-spec-dispatch-test
  (let [form-instance (r/atom {}) ; Mock form instance, not deeply used by mocks here
        parent-path [:root]]

    (testing "String schema dispatches to render-text-input"
      (let [args [:my-string :string false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::text-input (:type result)))
        (is (= :my-string (-> result :form-path last)))
        (is (= "My string" (:label-text result)))))

    (testing "Integer schema dispatches to render-number-input"
      (let [args [:my-int :int false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::number-input (:type result)))
        (is (= "My int" (:label-text result)))))

    (testing "Enum schema (<=3 options) dispatches to render-radio-group"
      (let [enum-schema [:enum :a :b :c]
            args [:my-enum enum-schema false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::radio-group (:type result)))
        (is (= "My enum" (:label-text result)))))

    (testing "Enum schema (>3 options) dispatches to render-select"
      (let [enum-schema [:enum :a :b :c :d]
            args [:my-big-enum enum-schema false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::select (:type result)))
        (is (= "My big enum" (:label-text result)))))

    (testing "Date string schema dispatches to render-datepicker"
      (let [date-schema (m/schema [:string {:type/date? true}])
            args [:my-date date-schema false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::datepicker (:type result)))
        (is (= "My date" (:label-text result)))))

    (testing "Boolean schema dispatches to render-radio-group with 是否Enum"
      (let [args [:my-bool :boolean false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::radio-group (:type result)))
        (is (= assessment-specs/是否Enum (:field-schema result))) ; Check it uses the correct spec
        (is (= "My bool" (:label-text result)))))

    (testing "Vector of enums dispatches to render-checkbox-group"
      (let [vec-enum-schema [:vector [:enum :x :y :z]]
            args [:my-checkboxes vec-enum-schema false parent-path form-instance {}]
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::checkbox-group (:type result)))
        (is (= "My checkboxes" (:label-text result)))))

    (testing "Simple map schema dispatches to render-map-schema-fields logic"
      ;; render-form-item-from-spec for :map returns a hiccup vector like [:div {:key ...} [:h4 ...] result-of-render-map-schema-fields]
      ;; We need to check the component type of the last element in that structure.
      (let [args [:my-map simple-map-schema false parent-path form-instance {}]
            hiccup-result (apply form-gen/render-form-item-from-spec args)
            ;; last element in the :div is the call to our mock
            mock-call-result (last hiccup-result)]
        (is (= :div (first hiccup-result))) ; outer structure
        (is (= ::map-schema-fields (:type mock-call-result)))
        (is (= simple-map-schema (:map-schema mock-call-result)))))


    (testing "Conditional map schema (is-conditional-map-schema?) dispatches to render-conditional-map-section"
      (let [args [:my-cond-map conditional-map-schema false parent-path form-instance {}]
            ;; This mock directly returns the type
            result (apply form-gen/render-form-item-from-spec args)]
        (is (= ::conditional-map-section (:type result)))
        (is (= :my-cond-map (:field-key result)))))

    (testing "Map schema with specific conditional key pattern (is-map-schema-with-conditional-key?) dispatches correctly"
      ;; This case is more complex as it involves recursive calls to render-form-item-from-spec
      ;; and Form.useWatch. For this test, we'll check the initial structure.
      ;; A deeper test might require mocking Form.useWatch and testing branches.
      (let [args [:my-common-cond common-conditional-pattern-schema false parent-path form-instance {}]
            hiccup-result (apply form-gen/render-form-item-from-spec args)
            first-rendered-item (second hiccup-result) ; [:<> item1 item2] -> item1 is the :有无 part
            ]
        (is (= :<> (first hiccup-result)))
        ;; The first part should be the :有无 field, which is an enum, likely a radio group
        (is (= ::radio-group (:type first-rendered-item)))
        (is (= :有无 (-> first-rendered-item :form-path last)))
        (is (= "My common cond" (:label-text first-rendered-item))) ; Label is passed to the first item
        ;; Further testing of the conditional :详情 part would require Form.useWatch mocking
        ))
    ))

;; --- Tests for render-map-schema-fields ---
(deftest render-map-schema-fields-test
  (testing "Iterates over map entries and calls render-form-item-from-spec for each"
    (let [form-instance (r/atom {})
          captured-calls (atom [])
          parent-path [:test-parent]
          calls @captured-calls]
      (is (= 2 (count calls))) ; simple-map-schema has :name and :age

      (let [name-call (first (filter #(= :name (first %)) calls))]
        (is (some? name-call))
        (is (= :name (nth name-call 0)))      ; field-key
        (is (= :string (nth name-call 1)))    ; field-schema
        (is (= false (nth name-call 2)))      ; optional?
        (is (= parent-path (nth name-call 3))) ; parent-form-path
        (is (= form-instance (nth name-call 4))) ; form-instance
        (is (map? (nth name-call 5)))) ; entry-props

      (let [age-call (first (filter #(= :age (first %)) calls))]
        (is (some? age-call))
        (is (= :age (nth age-call 0)))
        (is (= :int (nth age-call 1)))
        (is (= false (nth age-call 2)))
        (is (= parent-path (nth age-call 3)))
        (is (= form-instance (nth age-call 4)))
        (is (map? (nth age-call 5)))))))

;; --- Tests for render-conditional-map-section ---
;; Assumes conditional-map-schema is defined as:
;; (def conditional-map-schema
;;   [:map
;;    [:condition [:enum :option1 :option2]]
;;    [:option1 [:map [:field1 :string]]]
;;    [:option2 [:map [:field2 :int]]]])
;; Also assumes simple-map-schema is defined.

(deftest render-conditional-map-section-test
  (let [form-instance (r/atom {}) ; Mock form instance
        parent-path [:root-conditional]
        field-key :my-conditional-group
        entry-props {:label "My Conditional Group Label"}]

    (testing "Renders the conditional key input and the selected option's fields"
      (let [watched-value (atom :option1) ; Simulate initial watch value
            captured-render-item-args (atom nil)
            captured-render-map-args (atom nil)
            ;; render-form-item-from-spec takes a vector of args
            mocked-render-form-item (fn [[item-key item-schema item-optional? item-path item-form-inst item-entry-props]]
                                      (reset! captured-render-item-args {:key item-key
                                                                         :schema item-schema
                                                                         :optional? item-optional?
                                                                         :path item-path
                                                                         :form-instance item-form-inst
                                                                         :props item-entry-props})
                                      ;; Return a simple hiccup-like structure for identification
                                      [:div (keyword (str "mock-item-" (name item-key)))])

            mocked-render-map (fn [map-schema map-path map-form-inst]
                                (reset! captured-render-map-args {:schema map-schema
                                                                  :path map-path
                                                                  :form-instance map-form-inst})
                                ;; Return a simple hiccup-like structure
                                [:div (keyword (str "mock-map-" (name (first map-path))))])]


        (let [result (form-gen/render-conditional-map-section field-key conditional-map-schema parent-path form-instance entry-props)
              _item-render-result (second result) ;; First actual element in the :<> fragment, captured by mock
              detail-render-result (nth result 2 nil)]

          (is (= :<> (first result)))

          ;; Check call for the conditional key itself (e.g., the Radio/Select for :condition)
          (let [item-args @captured-render-item-args]
            (is (some? item-args) "render-form-item-from-spec should have been called for the conditional key")
            (is (= :condition (:key item-args)))
            ;; The schema for the conditional key is looked up inside options-map by render-conditional-map-section
            (is (= [:enum :option1 :option2] (:schema item-args)))
            (is (= false (:optional? item-args)))
            (is (= parent-path (:path item-args))) ; Path for item is parent, key is managed by section
            (is (= {:label "My Conditional Group Label"} (:props item-args))))

          (is (some? detail-render-result) "Detail section should be rendered for :option1")
          (let [map-args @captured-render-map-args]
            (is (some? map-args) "render-map-schema-fields should have been called for :option1 details")
            (is (= [:map [:field1 :string]] (:schema map-args)))
            (is (= (conj parent-path field-key :详情) (:path map-args))))


          ;; Simulate changing the watched value to :option2
          (reset! watched-value :option2)
          (reset! captured-render-item-args nil)
          (reset! captured-render-map-args nil)

          (let [result2 (form-gen/render-conditional-map-section field-key conditional-map-schema parent-path form-instance entry-props)
                detail-render-result2 (nth result2 2 nil)]
            (is (some? detail-render-result2) "Detail section should be rendered for :option2")
            (let [map-args @captured-render-map-args]
              (is (some? map-args) "render-map-schema-fields should have been called for :option2 details")
              (is (= [:map [:field2 :int]] (:schema map-args)))
              (is (= (conj parent-path field-key :详情) (:path map-args)))))


          ;; Simulate changing to a value with no corresponding detail schema
          (reset! watched-value :option3-no-schema)
          (reset! captured-render-item-args nil)
          (reset! captured-render-map-args nil)
          (let [result3 (form-gen/render-conditional-map-section field-key conditional-map-schema parent-path form-instance entry-props)
                detail-render-result3 (nth result3 2 nil)]
            (is (nil? detail-render-result3) "Detail section should NOT be rendered for :option3")
            (is (nil? @captured-render-map-args) "render-map-schema-fields should not have been called for details")))))

    (testing "Throws error if conditional-key or options-map is derived as nil (e.g. from bad schema)"
      ;; Mock get-map-schema-conditional-key to return nil
      (with-redefs [form-gen/get-map-schema-conditional-key (fn [s] (is (= s simple-map-schema)) nil)
                    form-gen/get-map-schema-conditional-options-map (fn [s] (is (= s simple-map-schema)) {:foo :bar})]
        (is (thrown? js/Error (form-gen/render-conditional-map-section field-key simple-map-schema parent-path form-instance entry-props))))
      ;; Mock get-map-schema-conditional-options-map to return nil
      (with-redefs [form-gen/get-map-schema-conditional-key (fn [s] (is (= s simple-map-schema)) :some-key)
                    form-gen/get-map-schema-conditional-options-map (fn [s] (is (= s simple-map-schema)) nil)]
        (is (thrown? js/Error (form-gen/render-conditional-map-section field-key simple-map-schema parent-path form-instance entry-props)))))))
