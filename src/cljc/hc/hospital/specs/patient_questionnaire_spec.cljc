(ns hc.hospital.specs.patient-questionnaire-spec
  "患者端问卷表单所需字段的 Malli Schema，保持与后端中文字段一致。"
  (:require [malli.core :as m]))

(def PhoneNumberSpec
  "手机号应以 1 开头并且为 11 位数字。"
  (m/schema
   [:re
    {:error/message {:zh "手机号格式不正确"}}
    #"^1[0-9]{10}$"]))

(def IdCardSpec
  "中国大陆身份证号，15 位或以 X 结尾的 18 位数字。"
  (m/schema
   [:re
    {:error/message {:zh "身份证号格式不正确"}}
    #"^(\d{15}|\d{17}[0-9X])$"]))

(def PatientBasicInfoSpec
  (m/schema
   [:map
    [:门诊号 [:string {:min 1
                    :error/message {:zh "门诊号不能为空"}}]]
    [:姓名 [:string {:min 1
                   :error/message {:zh "姓名不能为空"}}]]
    [:身份证号 IdCardSpec]
    [:手机号 PhoneNumberSpec]
    [:性别 [:enum {:error/message {:zh "性别必须为男或女"}}
          "男" "女"]]
    [:年龄 [:int {:min 0 :max 120
                :error/message {:zh "年龄必须在 0 到 120 之间"}}]]
    [:院区 [:enum {:error/message {:zh "院区必须为中心院区或积水潭院区"}}
          "中心院区" "积水潭院区"]]]))

(def 过敏史Spec
  "过敏史结构，同时用于生成疾病标签"
  (m/schema
   [:map {:disease/tag {:label "过敏史" :color "magenta"}
          :disease/predicate true?}
    [:有无 :boolean]
    [:过敏源 [:maybe :string]]
    [:过敏时间 [:maybe :string]]]))

(def PatientMedicalSummarySpec
  (m/schema
   [:map
    [:过敏史 过敏史Spec]
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
