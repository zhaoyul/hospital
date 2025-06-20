(ns hc.hospital.patient.db)

(def default-db
  {:patient-form {:基本信息 {:门诊号 ""
                         :姓名 ""
                         :身份证号 ""
                         :手机号 ""
                         :性别 nil
                         :年龄 nil
                         :院区 ""}
                  :病情摘要 {:过敏史 {:有无 false :过敏源 "" :过敏时间 nil}
                         :吸烟史 {:有无 false :年数 nil :每天支数 nil}
                         :饮酒史 {:有无 false :年数 nil :每天量 ""}}
                  :合并症 {:呼吸系统疾病 {:有无 false :详情 ""}
                        :神经肌肉疾病 {:有无 false :详情 ""}
                        :心血管疾病 {:有无 false :详情 ""}
                        :肝脏疾病 {:有无 false :详情 ""}
                        :内分泌疾病 {:有无 false :详情 ""}
                        :肾脏疾病 {:有无 false :详情 ""}
                        :神经精神疾病 {:有无 false :详情 ""}
                        :关节骨骼系统疾病 {:有无 false :详情 ""}
                        :既往麻醉手术史 {:有无 false :详情 ""}
                        :家族恶性高热史 {:有无 false :详情 ""}
                        :特殊用药史 {:使用过 false :药物名称 "" :最后时间 nil}}
                  :体格检查 {:心脏 {:状态 "normal" :描述 ""}
                         :肺脏 {:状态 "normal" :描述 ""}
                         :气道 {:状态 "normal" :描述 ""}
                         :牙齿 {:状态 "normal" :描述 ""}
                         :脊柱四肢 {:状态 "normal" :描述 ""}
                         :神经 {:状态 "normal" :描述 ""}
                         :其它 ""}
                  :辅助检查 {:相关辅助检查检验结果 nil
                         :胸片 nil
                         :肺功能 nil
                         :心脏彩超 nil
                         :心电图 nil
                         :其他 nil}
                  :current-step 0
                  :form-errors {}
                  :submitting? false
                  :submit-success? false
                  :submit-error nil}})
