(ns hc.hospital.ui-helpers-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [reagent.core :as r]
            [hc.hospital.ui-helpers :as ui-helpers]))

;; Note: We are not directly mocking "antd/Card" here.
;; We will inspect the structure produced by ui-helpers/custom-styled-card,
;; which should be a Reagent vector describing the Card component and its props.

(deftest custom-styled-card-test
  (testing "custom-styled-card correctly processes props and styles"
    (let [double-click-fn (fn [] "double-clicked")
          props {:icon [:span "test-icon"] ; Using a simple hiccup vector for icon
                 :title-text "Test Title"
                 :header-bg-color "blue"
                 :content [:p "Test Content"] ; Using a simple hiccup vector for content
                 :on-double-click double-click-fn
                 :card-style {:margin "10px" :border "1px solid green"}
                 :card-body-style {:padding "20px" :color "red"}}
          component (ui-helpers/custom-styled-card props)
          card-element (first component) ; Should be :> antd/Card or similar, we check type via keyword
          card-props (second component)
          card-content (nth component 2)]

      (is (= :> (:tag card-element)) "Card component is used with :> syntax")
      ;; The actual Card component reference might be more complex depending on how "antd" :refer [Card] resolves.
      ;; For now, we assume it's a keyword or a direct reference that can be checked if needed,
      ;; but the key is that custom-styled-card returns a Reagent structure for a Card.

      (testing "props are passed correctly"
        (is (= double-click-fn (:on-double-click card-props)) "on-double-click handler is passed")
        (is (r/element? (:title card-props)) "title is a Reagent element")
        ;; Inspecting the title structure (assuming it's [:span icon title-text])
        (let [title-children (-> card-props :title :props :children)]
          (is (= [:span "test-icon"] (first title-children)) "icon is passed correctly in title")
          (is (= "Test Title" (second title-children)) "title-text is passed correctly in title"))
        (is (= [:p "Test Content"] card-content) "content is passed as a child"))

      (testing "styles are merged correctly"
        (is (= "blue" (-> card-props :styles :header :background)) "header-bg-color is set")
        (is (= "10px" (-> card-props :style :margin)) "custom card-style :margin is merged")
        (is (= "1px solid green" (-> card-props :style :border)) "custom card-style :border is merged")
        (is (= "12px" (-> card-props :style :marginBottom)) "default card-style :marginBottom is retained")
        (is (= "20px" (-> card-props :styles :body :padding)) "custom card-body-style :padding is merged")
        (is (= "red" (-> card-props :styles :body :color)) "custom card-body-style :color is merged")
        (is (= "#ffffff" (-> card-props :styles :body :background)) "default card-body-style :background is retained"))))

  (testing "with minimal props (no on-double-click, no custom styles)"
    (let [props {:icon [:span "another-icon"]
                 :title-text "Another Title"
                 :header-bg-color "red"
                 :content [:p "Another Content"]}
          component (ui-helpers/custom-styled-card props)
          card-props (second component)
          card-content (nth component 2)]

      (is (nil? (:on-double-click card-props)) "on-double-click is nil when not provided")
      (is (= [:span "another-icon"] (-> card-props :title :props :children first)) "icon is passed")
      (is (= "Another Title" (-> card-props :title :props :children second)) "title-text is passed")
      (is (= "red" (-> card-props :styles :header :background)) "header-bg-color is passed")
      (is (= [:p "Another Content"] card-content) "content is passed")

      (testing "default styles are applied when no custom styles provided"
        (is (= {:marginBottom "12px"} (:style card-props)) "default card-style is used")
        (is (= {:background "#ffffff"} (-> card-props :styles :body)) "default card-body-style is used"))))

  (testing "card-style and card-body-style are optional"
    (let [props {:icon [:span "optional-icon"]
                 :title-text "Optional Title"
                 :header-bg-color "green"
                 :content [:p "Optional Content"]
                 :on-double-click (fn [] "clicked")}
          component (ui-helpers/custom-styled-card props)
          card-props (second component)]
      (is (= {:marginBottom "12px"} (:style card-props)) "default card-style is used when :card-style is omitted")
      (is (= {:background "#ffffff"} (-> card-props :styles :body)) "default card-body-style is used when :card-body-style is omitted")))

  (testing "only custom card-style provided"
    (let [props {:icon [:span "custom-style-icon"]
                 :title-text "Custom Style Title"
                 :header-bg-color "purple"
                 :content [:p "Custom Style Content"]
                 :card-style {:padding "5px"}}
          component (ui-helpers/custom-styled-card props)
          card-props (second component)]
      (is (= "5px" (-> card-props :style :padding)) "custom card-style :padding is applied")
      (is (= "12px" (-> card-props :style :marginBottom)) "default card-style :marginBottom is retained")
      (is (= {:background "#ffffff"} (-> card-props :styles :body)) "default card-body-style is used")))

  (testing "only custom card-body-style provided"
    (let [props {:icon [:span "custom-body-icon"]
                 :title-text "Custom Body Title"
                 :header-bg-color "orange"
                 :content [:p "Custom Body Content"]
                 :card-body-style {:fontSize "14px"}}
          component (ui-helpers/custom-styled-card props)
          card-props (second component)]
      (is (= {:marginBottom "12px"} (:style card-props)) "default card-style is used")
      (is (= "14px" (-> card-props :styles :body :fontSize)) "custom card-body-style :fontSize is applied")
      (is (= "#ffffff" (-> card-props :styles :body :background)) "default card-body-style :background is retained"))))

;; To run tests (example, adapt to your project's setup):
;; npx shadow-cljs watch test
;; or specific command for your test runner e.g.
;; npx shadow-cljs compile test && npx karma start --single-run
;; (if Karma is configured for the project)
