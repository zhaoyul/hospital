(ns hc.hospital.components.signature_pad
  "通用签字板组件，用于采集手写签名并返回图片数据"
  (:require
   ["signature_pad" :as SignaturePad]
   ["antd" :refer [Button]]
   [reagent.core :as r]))

(defn signature-pad
  "渲染签字板并在确认时调用 `on-confirm` 回传 base64 图片数据。"
  [{:keys [on-confirm]}]
  (let [canvas-ref (r/atom nil)
        pad-inst   (r/atom nil)
        init-pad   (fn []
                     (when @canvas-ref
                       (reset! pad-inst (SignaturePad. @canvas-ref #js {:penColor "black"}))))]
    (r/create-class
     {:component-did-mount init-pad
      :reagent-render
      (fn []
        [:div
         [:canvas {:ref   #(reset! canvas-ref %)
                   :style {:border "1px solid #eee"
                           :width  "300px"
                           :height "150px"}}]
         [:div {:style {:marginTop "10px"}}
          [:> Button {:on-click #(when @pad-inst (.clear @pad-inst))
                      :style    {:marginRight "10px"}}
           "清除签名"]
          [:> Button {:type    "primary"
                      :on-click (fn []
                                  (when (and @pad-inst (not (.isEmpty @pad-inst)))
                                    (on-confirm (.toDataURL @pad-inst))))}
           "确认签名"]]])}))
