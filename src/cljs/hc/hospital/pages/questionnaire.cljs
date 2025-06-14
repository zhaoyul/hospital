(ns hc.hospital.pages.questionnaire
  "问卷列表页面，展示患者提交的问卷。"
  (:require
   ["@ant-design/icons" :as icons]
   ["dayjs" :as dayjs]
   ["antd" :refer [Button DatePicker Input Pagination Space Table Tag]]
   [reagent.core :as r]))

;; Define columns for the questionnaire table
(def questionnaire-columns
  [{:title "填写时间" :dataIndex "submissionTime" :key "submissionTime"}
   {:title "姓名" :dataIndex "name" :key "name"}
   {:title "身份证号" :dataIndex "idCard" :key "idCard"}
   {:title "联系电话" :dataIndex "phone" :key "phone"}
   {:title "问卷状态" :dataIndex "status" :key "status"
    :render (fn [status-data]
              (r/as-element
               [:> Space {:direction "vertical"}
                [:> Tag {:color (if (= (:status status-data) "已关联") "green" "warning")}
                 (:status status-data)]
                (when-let [pid (:patientId status-data)]
                  [:span {:style {:color "gray" :fontSize "12px"}} (str "患者ID: " pid)])]))}
   {:title "操作" :key "action" :align "center"
    :render (fn [_ record]
              (r/as-element
               [:> Space {}
                [:> Button {:type "link"
                            :on-click #(js/console.log "View" record)} "查看"]
                (if (= (get-in record [:status :status]) "已关联")
                  [:> Button {:type "link"
                              :style {:color "#faad14"}
                              :on-click #(js/console.log "Unlink" record)} "解除关联"]
                  [:> Button {:type "link"
                              :style {:color "#52c41a"}
                              :on-click #(js/console.log "Link" record)} "关联患者"])
                [:> Button {:type "link"
                            :danger true
                            :on-click #(js/console.log "Delete" record)} "删除"]]))}])



;; Mock data for the questionnaire table
(def mock-questionnaire-data
  [{:key "1" :submissionTime "2025-05-06 10:30" :name "张三" :idCard "3101********1234" :phone "138****5678" :status {:status "已关联" :patientId "P20231025001"}}
   {:key "2" :submissionTime "2025-05-06 09:15" :name "李四" :idCard "3101********5678" :phone "139****1234" :status {:status "未关联"}}
   {:key "3" :submissionTime "2025-05-06 09:00" :name "王五" :idCard "3101********8901" :phone "137****2468" :status {:status "已关联" :patientId "P20231025003"}}
   {:key "4" :submissionTime "2025-05-06 08:45" :name "赵六" :idCard "3101********3456" :phone "136****1357" :status {:status "未关联"}}
   {:key "5" :submissionTime "2025-05-06 08:30" :name "孙七" :idCard "3101********7890" :phone "135****9876" :status {:status "已关联" :patientId "P20231025005"}}
   {:key "6" :submissionTime "2025-05-06 08:15" :name "周八" :idCard "3101********2345" :phone "134****5432" :status {:status "未关联"}}
   {:key "7" :submissionTime "2025-05-06 08:00" :name "吴九" :idCard "3101********6789" :phone "133****1111" :status {:status "已关联" :patientId "P20231025007"}}
   {:key "8" :submissionTime "2025-05-06 07:45" :name "郑十" :idCard "3101********4321" :phone "132****2222" :status {:status "未关联"}}])

(defn questionnaire-list-content []
  (let [start-date (r/atom "2025-05-06")
        end-date (r/atom "2025-05-06")
        search-text (r/atom "")]
    [:<>
     ;; Search Area
     [:div {:style {:background "#fff" :padding "16px" :border-radius "2px" :margin-bottom "16px"}}
      [:> Space {:style {:display "flex" :justifyContent "space-between" :alignItems "center"}}
       [:> Space {:align "center"} ; Ensure alignment for items in this Space
        [:span "填写日期:"]
        [:> DatePicker {:defaultValue (dayjs @start-date "YYYY-MM-DD") :onChange (fn [_ date-string] (reset! start-date date-string))}] ; Changed date to _
        [:span {:style {:margin "0 8px"}} "至"]
        [:> DatePicker {:defaultValue (dayjs @end-date "YYYY-MM-DD") :onChange (fn [_ date-string] (reset! end-date date-string))}]] ; Changed date to _
       [:> Input {:placeholder "输入姓名/身份证号搜索"
                  :style {:width "320px" :flexGrow 1 :maxWidth "320px"}
                  :prefix (r/as-element [:> icons/SearchOutlined])
                  :value @search-text
                  :on-change (fn [e] (reset! search-text (-> e .-target .-value)))}]
       [:> Space {}
        [:> Button {:on-click #(.log js/console "Reset filters" (reset! start-date "2025-05-06") (reset! end-date "2025-05-06") (reset! search-text ""))} "重置"]
        [:> Button {:type "primary" :icon (r/as-element [:> icons/SearchOutlined]) :on-click #(.log js/console "Search" @start-date @end-date @search-text)} "搜索"]]]]
     ;; Questionnaire Table
     [:div {:style {:background "#fff" :border-radius "2px"}} ; Removed padding: 0px
      [:> Table {:columns questionnaire-columns
                 :dataSource mock-questionnaire-data
                 :pagination false ;; We will use the separate Pagination component
                 :style {:padding "0 16px"}}]
      [:> Pagination {:style {:textAlign "right" :margin "16px 0" :padding "0 16px"}
                      :defaultCurrent 1
                      :total 24
                      :pageSize 8
                      :showTotal (fn [total _] (str "共 " total " 条"))}] ; Changed range to _
      ]
     ;; Modals (placeholders for now)
     ;; Link Patient Modal
     ;; [:> Modal {:title "关联患者" :visible false :onOk #() :onCancel #()}]
     ;; Questionnaire Preview Modal
     ;; [:> Modal {:title "问卷详情" :visible false :onOk #() :onCancel #() :width "800px"}]
     ]))
