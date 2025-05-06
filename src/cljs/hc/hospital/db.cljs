(ns hc.hospital.db)

(def default-db
  {:anesthesia
   {:active-tab "patients"  ;; 当前选中的主标签页：patients, assessment, history
    :current-patient-id nil ;; 当前选中的患者ID
    :search-term ""         ;; 患者搜索词
    :date-range nil         ;; 日期过滤范围

    :patients               ;; 患者列表示例数据
    [{:key "P001" :name "张三" :age 45 :gender "男" :type "择期手术" :status "待评估"
      :department "骨科" :diagnosis "股骨颈骨折" :doctor "李医生" :date "2025-04-28"}
     {:key "P002" :name "李四" :age 32 :gender "女" :type "紧急手术" :status "已评估"
      :department "普外科" :diagnosis "急性阑尾炎" :doctor "王医生" :date "2025-04-29"}
     {:key "P003" :name "王五" :age 67 :gender "男" :type "择期手术" :status "评估中"
      :department "心胸外科" :diagnosis "冠心病" :doctor "张医生" :date "2025-04-30"}]

    :ui
    {:active-assessment-tab "brief-history"  ;; 评估页面的标签：brief-history, physical-exam, lab-tests
     :show-advanced-options false}           ;; 是否显示高级选项

    :assessment
    {:brief-medical-history     ;; 简要病史
     {;; Fields based on anesthesia_home.cljs and structured for antd-form
      :past-history {:yes-no nil :description ""} ;; from form-comp/yes-no-with-description
      :allergic-history {:yes-no nil :description ""}
      :surgery-anesthesia-history {:yes-no nil :description ""}
      :pregnancy {:yes-no nil :description ""}
      :blood-transfusion-history {:yes-no nil :description ""}
      :menstrual-period {:yes-no nil :description ""}
      :personal-history []      ;; from antd/checkbox-group, name :personal-history e.g. ["smoke", "drink"]
      :other {:checkbox false :description ""} ;; from form-comp/checkbox-with-conditional-input
      }

     :physical-examination   ;; 体格检查
     {;; Fields based on anesthesia_home.cljs and structured for antd-form
      :general-condition nil  ;; radio: "bad", "fair", "average", "good"
      :height nil             ;; number input
      :weight nil             ;; number input
      :bp {:systolic nil :diastolic nil} ;; number inputs
      :heart-rate nil         ;; number input
      :respiratory-rate nil   ;; number input
      :temperature nil        ;; number input
      :mental-state nil       ;; radio: "normal", "drowsy", etc.
      :head-neck nil          ;; radio: "normal", "scar", etc.
      :mouth-opening nil      ;; number input
      :mallampati-score nil   ;; radio: "I", "II", "III", "IV"
      :thyromental-distance nil ;; number input
      :related-history {      ;; antd/form-item names like [:related-history :difficult-airway]
       :difficult-airway false
       :postoperative-nausea false
       :malignant-hyperthermia false
       :other-checkbox false
       :other ""
       }
      :chest nil              ;; radio: "normal", "barrel", etc.
      }

     :lab-tests              ;; 实验室检查
     {;; Fields based on anesthesia_home.cljs and structured for antd-form
      :complete-blood-count { ;; names like [:complete-blood-count :hemoglobin]
       :hemoglobin nil       ;; was "RBC" in old label, but seems to be hemoglobin value
       :hematocrit nil       ;; was "Hct"
       :platelets nil        ;; was "PLT"
       :wbc nil              ;; was "WBC"
       }
      :blood-type nil         ;; radio: "A", "B", "AB", "O"
      :rh nil                 ;; radio: "negative", "positive"
      :coagulation nil        ;; radio: "normal", "abnormal"
      :biochemistry {         ;; names like [:biochemistry :glucose]
       :glucose nil
       :alt nil
       :ast nil
       :sodium nil
       :potassium nil
       }
      :ecg ""                 ;; text input
      :chest-xray ""          ;; text input
      }
      ;; Preserving :anesthesia-plan from the original db.cljs structure
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
      :notes ""}
     }}})
