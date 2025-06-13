(ns hc.hospital.components.summary-components
  "通用评估摘要组件"
  (:require [hc.hospital.natural-language-generators :as nlg]))

(defn assessment-summary
  "根据数据和 schema 生成统一的评估摘要视图。"
  [{:keys [data schema section-key]}]
  (let [label (nlg/schema-key->display-label section-key)]
    (if (seq data)
      (let [summary-hiccup (nlg/generate-summary-component data schema section-key)]
        (if (and summary-hiccup (vector? summary-hiccup) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}}
           (str "暂无" label "评估数据可供总结 (内容为空)。")]))
      [:div {:style {:padding "10px"}}
       (str "暂无" label "评估数据可供总结。")])))
