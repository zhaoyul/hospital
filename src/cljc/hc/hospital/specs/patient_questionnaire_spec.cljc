(ns hc.hospital.specs.patient-questionnaire-spec
  "患者端问卷表单所需字段的 Malli Schema，保持与后端中文字段一致。"
  (:require [malli.core :as m]))

(def PatientBasicInfoSpec
  (m/schema
   [:map
    [:门诊号 [:string {:min 1}]]
    [:姓名 [:string {:min 1}]]
    [:身份证号 [:maybe :string]]
    [:手机号 [:maybe :string]]
    [:性别 [:enum "男" "女"]]
    [:年龄 [:int {:min 0 :max 120}]]
    [:院区 [:enum "main" "jst"]]]))

(def PatientMedicalSummarySpec
  (m/schema
   [:map
    [:过敏史 [:map
            [:有无 :boolean]
            [:过敏源 [:maybe :string]]
            [:过敏时间 [:maybe :string]]]]
    [:吸烟史 [:map
            [:有无 :boolean]
            [:年数 [:maybe :int]]
            [:每天支数 [:maybe :int]]]]
    [:饮酒史 [:map
            [:有无 :boolean]
            [:年数 [:maybe :int]]
            [:每天量 [:maybe :string]]]]]))

(def PatientComorbiditiesSpec
  (m/schema
   [:map
    [:呼吸系统疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:神经肌肉疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:心血管疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:肝脏疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:内分泌疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:肾脏疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:神经精神疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:关节骨骼系统疾病 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:既往麻醉手术史 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:家族恶性高热史 [:map [:有无 :boolean] [:详情 [:maybe :string]]]]
    [:特殊用药史 [:map
               [:使用过 :boolean]
               [:药物名称 [:maybe :string]]
               [:最后时间 [:maybe :string]]]]]))

(def PatientPhysicalExamSpec
  (m/schema
   [:map
    [:心脏 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:肺脏 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:气道 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:牙齿 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:脊柱四肢 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:神经 [:map [:状态 [:enum "normal" "abnormal"]] [:描述 [:maybe :string]]]]
    [:其它 [:maybe :string]]]))

(def PatientAuxiliaryExamSpec
  (m/schema
   [:map
    [:相关辅助检查检验结果 [:maybe :string]]
    [:胸片 [:maybe :string]]
    [:肺功能 [:maybe :string]]
    [:心脏彩超 [:maybe :string]]
    [:心电图 [:maybe :string]]
    [:其他 [:maybe :string]]]))

(def PatientQuestionnaireSpec
  (m/schema
   [:map
    [:基本信息 PatientBasicInfoSpec]
    [:病情摘要 PatientMedicalSummarySpec]
    [:合并症 PatientComorbiditiesSpec]
    [:体格检查 PatientPhysicalExamSpec]
    [:辅助检查 PatientAuxiliaryExamSpec]]))
