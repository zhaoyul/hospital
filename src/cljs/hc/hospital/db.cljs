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
     {:cardiovascular        {:value false :description ""} ;; 心血管疾病
      :respiratory           {:value false :description ""} ;; 呼吸系统疾病
      :digestive             {:value false :description ""} ;; 消化系统疾病
      :urinary               {:value false :description ""} ;; 泌尿系统疾病
      :nervous               {:value false :description ""} ;; 神经系统疾病
      :endocrine             {:value false :description ""} ;; 内分泌系统疾病
      :allergies             {:value false :description ""} ;; 过敏史
      :previous-surgeries    {:value false :description ""} ;; 既往手术史
      :medications           {:value false :description ""} ;; 目前用药情况
      :family-history        {:value false :description ""} ;; 家族史
      :personal-history      {:smoking false                ;; 个人史
                              :drinking false
                              :drug-use false}
      :other-description     ""}                            ;; 其他描述

     :physical-examination   ;; 体格检查
     {:height 170.0                          ;; 身高（cm）
      :weight 65.0                           ;; 体重（kg）
      :bmi 22.5                              ;; BMI 自动计算
      :bp {:systolic 120                     ;; 血压（mmHg）
           :diastolic 80}
      :heart-rate 75                         ;; 心率（次/分）
      :respiratory-rate 16                   ;; 呼吸频率（次/分）
      :temperature 36.5                      ;; 体温（℃）
      :oxygen-saturation 98                  ;; 血氧饱和度（%）
      :airway-assessment                     ;; 气道评估
      {:mallampati-score "I"                ;; Mallampati 评分（I-IV）
       :mouth-opening "正常"                ;; 张口度
       :thyromental-distance "正常"         ;; 甲颏间距
       :neck-mobility "正常"}               ;; 颈部活动度
      :cardiovascular-exam                   ;; 心血管检查
      {:heart-sounds "正常"                 ;; 心音
       :murmurs "无"}                       ;; 杂音
      :respiratory-exam                      ;; 呼吸系统检查
      {:lung-sounds "正常"                  ;; 肺部听诊
       :abnormalities "无"}                 ;; 异常情况
      :asa-classification "II"               ;; ASA 分级（I-VI）
      :mallampati-score "I"                  ;; Mallampati 评分（I-IV）
      :related-history                       ;; 相关病史内容
      {:difficult-airway false              ;; 困难气道史
       :postoperative-nausea false          ;; 术后恶心呕吐史
       :malignant-hyperthermia false}}      ;; 恶性高热史

     :lab-tests              ;; 实验室检查
     {:complete-blood-count               ;; 血常规
      {:hemoglobin 135                    ;; 血红蛋白（g/L）
       :hematocrit 0.4                    ;; 红细胞压积
       :platelets 200                     ;; 血小板（×10^9/L）
       :wbc 6.5}                          ;; 白细胞（×10^9/L）
      :coagulation-profile                ;; 凝血功能
      {:pt 12                             ;; 凝血酶原时间（秒）
       :inr 1.0                           ;; 国际标准化比率
       :aptt 30}                          ;; 活化部分凝血活酶时间（秒）
      :biochemistry                       ;; 生化指标
      {:glucose 5.0                       ;; 血糖（mmol/L）
       :urea 5.0                          ;; 尿素（mmol/L）
       :creatinine 70                     ;; 肌酐（μmol/L）
       :sodium 140                        ;; 钠（mmol/L）
       :potassium 4.0                     ;; 钾（mmol/L）
       :alt 20                            ;; 丙氨酸氨基转移酶（U/L）
       :ast 25}                           ;; 天门冬氨酸氨基转移酶（U/L）
      :ecg "正常窦性心律，无明显异常"      ;; 心电图结果
      :chest-xray "无明显异常"            ;; 胸片结果
      :additional-tests {}}               ;; 其他检查结果

     :anesthesia-plan        ;; 麻醉计划
     {:anesthesia-type "全身麻醉"          ;; 麻醉类型
      :risk-assessment                     ;; 风险评估
      {:cardiac-risk "低风险"             ;; 心脏风险
       :respiratory-risk "低风险"         ;; 呼吸系统风险
       :bleeding-risk "低风险"}           ;; 出血风险
      :special-considerations []           ;; 特殊注意事项
      :medications                         ;; 用药计划
      {:premedication []                  ;; 术前用药
       :induction []                      ;; 麻醉诱导
       :maintenance []                    ;; 麻醉维持
       :emergence []}                     ;; 麻醉苏醒
      :monitoring                          ;; 监测项目
      {:standard true                     ;; 标准监测
       :invasive-bp false                 ;; 有创血压
       :central-venous false              ;; 中心静脉
       :others []}                        ;; 其他监测
      :notes ""}                           ;; 备注
     }}})
