(ns hc.hospital.patient.db)

;; 初始数据库状态
(def default-db
  {:patient-form {:basic-info {:outpatient-number "" ;; 门诊号
                               :name "" ;; 姓名
                               :gender nil ;; 性别
                               :age nil ;; 年龄
                               :ward "-" ;; 病区
                               :health-card-number "-" ;; 电子健康卡号
                               :pre-op-diagnosis "-" ;; 术前诊断
                               :planned-surgery "-"} ;; 拟施手术
                  :general-condition {:height nil ;; 身高
                                      :weight nil ;; 体重
                                      :mental-state "-" ;; 精神状态
                                      :activity-ability "-" ;; 活动能力
                                      :blood-pressure {:systolic nil ;; 血压-收缩压
                                                       :diastolic nil} ;; 血压-舒张压
                                      :pulse nil ;; 脉搏
                                      :respiration nil ;; 呼吸
                                      :temperature nil ;; 体温
                                      :spo2 nil} ;; 血氧饱和度
                  :medical-summary {:allergy-history false ;; 过敏史
                                    :allergens "未知" ;; 过敏原
                                    :allergy-time "2025-04-23" ;; 过敏时间
                                    :smoking-history false ;; 吸烟史
                                    :smoking-years nil ;; 吸烟年数
                                    :smoking-per-day nil ;; 每天吸烟支数
                                    :drinking-history false ;; 饮酒史
                                    :drinking-years nil ;; 饮酒年数
                                    :drinking-ml-per-day nil} ;; 每天饮酒量
                  :comorbidities {:respiratory-disease "-" ;; 呼吸系统疾病
                                 :cardiovascular-disease "-" ;; 心血管疾病
                                 :endocrine-disease "-" ;; 内分泌疾病
                                 :neuropsychiatric-disease "-" ;; 神经精神疾病
                                 :skeletal-system "-" ;; 关节骨骼系统
                                 :family-malignant-hyperthermia "-" ;; 家族恶性高热史
                                 :past-anesthesia-surgery "-" ;; 既往麻醉、手术史
                                 :special-medications {:used "-" ;; 使用的特殊药物
                                                      :last-time "2025-04-23 09:50"}} ;; 最后一次用药时间
                  :physical-examination {:heart "-" ;; 心脏
                                        :lungs "-" ;; 肺脏
                                        :airway "-" ;; 气道
                                        :teeth "-" ;; 牙齿
                                        :spine-limbs "-" ;; 脊柱四肢
                                        :other "-" ;; 其它
                                        :nervous "-"} ;; 神经
                  :auxiliary-examination {:chest-radiography "-" ;; 胸片
                                         :pulmonary-function "-" ;; 肺功能
                                         :cardiac-ultrasound "-" ;; 心脏彩超
                                         :ecg "-" ;; 心电图
                                         :other "-"} ;; 其他
                  :assessment {:asa-classification "-" ;; ASA分级
                              :nyha-classification "-" ;; 心功能分级(NYHA)
                              :planned-anesthesia "-" ;; 拟行麻醉方式
                              :monitoring-items "-" ;; 监测项目
                              :special-techniques "-"} ;; 特殊技术
                  :other-info {:pre-op-fasting "术前小时禁食，术前小时禁饮" ;; 术前麻醉医嘱
                              :continue-medication "需进一步检查" ;; 术日晨继续应用药物
                              :stop-medication "需进一步会诊" ;; 术日晨停用药物
                              :anesthesia-notes "-" ;; 麻醉中需注意的问题
                              :anesthesiologist-signature "-" ;; 麻醉医师签名
                              :assessment-date nil} ;; 评估日期
                  :form-errors {}
                  :submitting? false
                  :submit-success? false
                  :submit-error nil
                  :current-step 0}})