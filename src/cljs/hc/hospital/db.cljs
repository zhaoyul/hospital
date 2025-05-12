(ns hc.hospital.db)

(def default-db
  { ;; Root map for the entire default database state
   :anesthesia
   {:active-tab "patients"  ;; 当前选中的主标签页：patients, assessment, history
    :all-patient-assessments []
    :current-patient-id nil ;; 当前选中的患者ID
    :search-term ""         ;; 患者搜索词
    :date-range nil         ;; 日期过滤范围
    :assessment-status-filter "all"
    :patients               ;; 患者列表示例数据
    [{:key "P001" :name "张三" :age 45 :gender "男" :type "择期手术" :status "待评估"
      :department "骨科" :diagnosis "股骨颈骨折" :doctor "李医生" :date "2025-04-28"
      :patient-id-display "01864297 | Inp01864297" :anesthesia-type "静脉复合麻醉"}
     {:key "P002" :name "李四" :age 32 :gender "女" :type "紧急手术" :status "已评估"
      :department "普外科" :diagnosis "急性阑尾炎" :doctor "王医生" :date "2025-04-29"
      :patient-id-display "01864297 | Inp01864297" :anesthesia-type "静脉复合麻醉"}
     {:key "P003" :name "王五" :age 67 :gender "男" :type "择期手术" :status "评估中"
      :department "心胸外科" :diagnosis "冠心病" :doctor "张医生" :date "2025-04-30"
      :patient-id-display "01864297 | Inp01864297" :anesthesia-type "静脉复合麻醉"}]

    :ui
    {:active-assessment-tab "brief-history"  ;; 评估页面的标签：brief-history, physical-exam, lab-tests
     :show-advanced-options false}           ;; 是否显示高级选项

    :fetch-assessments-error nil

    :assessment ;; This level holds both form-data and anesthesia-plan
    {;; New structure for the four cards under :form-data
     :form-data
     {:allergy {:has "no" :allergen "" :last-reaction-date nil}
      :habits {:smoking {:has "no" :years nil :per-day nil}
               :drinking {:has "no" :years nil :per-day nil}}
      :comorbidities
      {:respiratory {:has "no" :details ""}
       :cardiovascular {:has "no" :details ""}
       :endocrine {:has "no" :details ""}
       :neuro-psychiatric {:has "no" :details ""}
       :neuromuscular {:has "no" :details ""}
       :hepatic {:has "no" :details ""}
       :renal {:has "no" :details ""}
       :musculoskeletal {:has "no" :details ""}
       :malignant-hyperthermia {:has "no" :details ""}
       :anesthesia-surgery-history {:has "no" :details ""}
       :special-medications {:has "no" :details "" :last-dose-time nil}}
      :physical-exam
      {:heart {:status "normal" :notes ""}
       :lungs {:status "normal" :notes ""}
       :airway {:status "normal" :notes ""}
       :teeth {:status "normal" :notes ""}
       :spine-limbs {:status "normal" :notes ""}
       :neuro {:status "normal" :notes ""}
       :other {:notes ""}} ; For the "其它" textarea in physical exam
      :aux-exams ; For "相关辅助检查检验结果"
      {;; Each key will store a list of Ant Design file objects
       ;; e.g., [{:uid "-1" :name "image.png" :status "done" :url "..."}]
       :chest-xray []
       :pulmonary-function []
       :cardiac-echo []
       :ecg []
       :other-results ""}} ; Textarea for "其他" in auxiliary exams

     ;; Preserving :anesthesia-plan from the original db.cljs structure,
     ;; as it seems separate from the four new cards.
     :anesthesia-plan
     {:anesthesia-type "全身麻醉"
      :risk-assessment
      {:cardiac-risk "低风险"
       :respiratory-risk "低风险"
       :bleeding-risk "低风险"}
      :special-considerations []
      :medications
      {:premedication []
       :induction []
       :maintenance []
       :emergence []}
      :monitoring
      {:standard true
       :invasive-bp false
       :central-venous false
       :others []}
      :notes ""} ; This is likely the overall assessment notes/remarks
     }}
   ;; Root level keys for doctor management etc.
   :doctors []
   :doctor-modal-visible? false
   :editing-doctor nil
   })
