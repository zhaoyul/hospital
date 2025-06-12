(ns hc.hospital.components.assessment-summary
  "通用评估摘要视图组件，生成各系统的摘要文本。"
  (:require [hc.hospital.natural-language-generators :as nlg]))

(defn assessment-summary-view
  "根据传入的数据和 Malli Spec 生成摘要显示。"
  [{:keys [data spec section-key empty-text empty-hint]}]
  (let [label (nlg/schema-key->display-label section-key)
        default-empty (str "暂无" label "评估数据可供总结。")
        default-hint  (str "暂无" label "评估数据可供总结 (内容为空)。")]
    (if (seq data)
      (let [summary-hiccup (nlg/generate-summary-component data spec section-key)]
        (if (or (not (vector? summary-hiccup)) (seq summary-hiccup))
          summary-hiccup
          [:div {:style {:padding "10px"}}
           (or empty-hint default-hint)]))
      [:div {:style {:padding "10px"}}
       (or empty-text default-empty)])))
