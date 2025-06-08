(ns hc.hospital.specs.assessment-complete-cn-spec
  (:require [malli.core :as m]
            [malli.util :as mu]))

;; --- 可复用的 Predicates 或 Schemas ---
(def NonEmptyString (m/schema [:string {:min 1}]))
(def OptionalString (m/schema [:maybe :string]))
(def OptionalPositiveInt (m/schema [:maybe [:int {:min 0}]])) ; 用于年龄、年数等
(def OptionalNumber (m/schema [:maybe [:or :int :double]])) ; 用于带小数点的数值
(def OptionalBoolean (m/schema [:maybe :boolean]))

;; 日期和时间格式
(def 日期字符串Spec (m/schema [:re {:is-date? true} #"\d{4}-\d{2}-\d{2}"])) ; YYYY-MM-DD
(def Optional日期字符串 (m/schema [:maybe 日期字符串Spec]))
(def 日期时间字符串Spec (m/schema [:re {:is-date? true} #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?Z?"]))
(def Optional日期时间字符串 (m/schema [:maybe 日期时间字符串Spec]))

;; 常用枚举
(def 有无不详Enum (m/schema [:enum :无 :有 :不详]))
(def 有无Enum (m/schema [:enum :无 :有])) ; 直接对应 “有”、“无”
(def 治疗情况Enum (m/schema [:enum :治愈 :好转 :仍有症状 :未治疗 :病情稳定 :其他]))
(def 是否Enum (m/schema [:enum :是 :否])) ; 对应 "是" / "否"

;; --- 各系统评估 Spec 定义 ---

;; 1. 循环系统
(def 高血压详情Spec
  (m/schema
   [:map
    [:病史时长 {:optional true} [:maybe [:enum :小于1年 :1至5年 :5至10年 :大于10年]]]
    [:分级 {:optional true} [:maybe [:enum :Ⅰ级 :Ⅱ级 :Ⅲ级]]]
    [:治疗 {:optional true}
     [:map
      [:类型 {:optional true} [:maybe [:enum :规律服药血压控制良好 :规律服药血压波动大 :不监测血压 :不规律服药血压波动大 :未治疗 :其它]]]
      [:用药详情 {:optional true} OptionalString]]]]))

(def 血管疾病详情Spec
  (m/schema
   [:map
    [:高血压 {:optional true} 高血压详情Spec]
    [:高血脂 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]
    [:低血压 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]
    [:动脉瘤 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]
    [:血栓 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]
    [:其他先天性血管畸形 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]]))

(def 心脏疾病通用详情Spec
  (m/schema
   [:map
    [:有无 {:optional true} 有无Enum]
    [:描述 {:optional true} OptionalString] ; 对应 "（可输入内容）"
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 冠心病详情Spec
  (m/schema
   [:map
    [:有无 {:optional true} 有无Enum]
    [:症状 {:optional true} [:maybe [:enum :无症状 :稳定性心绞痛 :不稳定性心绞痛 :心梗]]]
    [:心脏支架 {:optional true} [:map [:有无 {:optional true} 有无Enum]]]
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 心律失常详情Spec
  (m/schema
   [:map
    [:有无 {:optional true} 有无不详Enum]
    [:类型 {:optional true} [:maybe [:enum :低危型 :中危型 :高危型]]]
    [:描述 {:optional true} OptionalString] ; "(可输入内容)"
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 充血性心力衰竭史详情Spec
  (m/schema
   [:map
    [:有无 {:optional true} 有无Enum]
    [:上次发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 循环系统Spec
  (m/schema
   [:map
    [:心电图 {:optional true} [:map [:描述 {:optional true} OptionalString]]]
    [:血管疾病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 血管疾病详情Spec]]]
    [:心脏疾病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true}
       [:map
        [:冠心病 {:optional true} 冠心病详情Spec]
        [:心律失常 {:optional true} 心律失常详情Spec]
        [:心肌病 {:optional true} 心脏疾病通用详情Spec]
        [:心脏瓣膜病变 {:optional true} 心脏疾病通用详情Spec]
        [:先天性心脏病 {:optional true} 心脏疾病通用详情Spec]
        [:充血性心力衰竭史 {:optional true} 充血性心力衰竭史详情Spec]
        [:肺动脉高压 {:optional true} 心脏疾病通用详情Spec]]]]]
    [:心脏起搏器植入史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true}
       [:map
        [:类型 {:optional true} [:maybe [:enum :临时起搏器 :永久起搏器]]]
        [:工作状态 {:optional true} OptionalString]]]]]
    [:心脏彩超检查 {:optional true} [:map [:结果 {:optional true} OptionalString]]]
    [:冠脉CTA或冠脉造影结果 {:optional true} [:map [:结果 {:optional true} OptionalString]]]
    [:心脏功能评估 {:optional true} [:map [:NYHA分级 {:optional true} [:maybe [:enum :Ⅰ级 :Ⅱ级 :Ⅲ级 :Ⅳ级]]]]]
    [:运动能力评估 {:optional true} [:map [:METs水平 {:optional true} [:maybe [:enum :大于6MET :3至6MET :小于3MET]]]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 2. 呼吸系统
(def 呼吸系统症状Enum (m/schema [:enum :咳嗽 :流涕清 :流涕浓 :发热 :咳痰 :咽痛 :身困乏力 :其他]))
(def 呼吸系统症状列表 (m/schema [:vector 呼吸系统症状Enum]))

(def 近两周内感冒病史详情Spec
  (m/schema
   [:map
    [:发病日期 {:optional true} Optional日期字符串]
    [:症状 {:optional true} 呼吸系统症状列表]
    [:其他不适症状 {:optional true} OptionalString]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 近一个月内支气管炎或肺炎病史详情Spec
  (m/schema
   [:map
    [:发病日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 哮喘病史详情Spec
  (m/schema
   [:map
    [:上次发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} [:maybe [:enum :临床缓解期 :急性发作期 :慢性持续期 :未治疗]]]
    [:用药情况 {:optional true} [:maybe [:enum :激素类药物 :非激素类药物 :不详]]]
    [:用药详情 {:optional true} OptionalString]]))

(def 慢性阻塞性肺疾病详情Spec
  (m/schema [:map [:治疗情况 {:optional true} [:maybe [:enum :稳定期 :急性加重期 :近1年反复急性发作大于3次]]]]))

(def 呼吸系统通用疾病详情Spec (m/schema [:map [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 呼吸系统Spec
  (m/schema
   [:map
    [:近两周内感冒病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 近两周内感冒病史详情Spec]]]
    [:近一个月内支气管炎或肺炎病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 近一个月内支气管炎或肺炎病史详情Spec]]]
    [:哮喘病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 哮喘病史详情Spec]]]
    [:慢性阻塞性肺疾病 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 慢性阻塞性肺疾病详情Spec]]]
    [:支气管扩张症 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 呼吸系统通用疾病详情Spec]]]
    [:肺部结节 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 呼吸系统通用疾病详情Spec]]]
    [:肺部肿瘤 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 呼吸系统通用疾病详情Spec]]]
    [:胸片 {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:胸部CT {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:肺功能 {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:血气分析 {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:是否有肺结核 {:optional true} 有无Enum]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 3. 精神及神经肌肉系统
(def 精神认知症状Enum (m/schema [:enum :智力发育迟缓 :焦虑症 :抑郁症 :精神分裂 :睡眠障碍 :孤独症 :病情稳定 :其他]))
(def 精神认知症状列表 (m/schema [:vector 精神认知症状Enum]))
(def 神经肌肉其他症状Enum (m/schema [:enum :重症肌无力 :格林巴利综合征 :帕金森病史 :脊髓灰质炎后综合征 :多发性硬化症 :肌营养不良 :其他]))
(def 神经肌肉其他症状列表 (m/schema [:vector 神经肌肉其他症状Enum]))


(def 精神认知相关疾病史详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 精神认知症状列表]
    [:其他症状描述 {:optional true} OptionalString] ; 对应 "其他（可输入内容）"
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 癫痫病史详情Spec
  (m/schema
   [:map
    [:近期发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:治疗用药 {:optional true} OptionalString]]))

(def 眩晕病史详情Spec
  (m/schema
   [:map
    [:近期发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 短暂性脑缺血发作病史详情Spec
  (m/schema
   [:map
    [:近期发作情况 {:optional true} [:maybe [:enum :近3月内无发作 :近3月内有发作]]]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 脑梗病史详情Spec
  (m/schema
   [:map
    [:近期发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:目前用药 {:optional true} OptionalString]]))

(def 脑出血病史详情Spec
  (m/schema
   [:map
    [:近期发作日期 {:optional true} Optional日期字符串]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 帕金森综合症详情Spec
  (m/schema
   [:map
    [:诊断年限 {:optional true} Optional日期字符串] ; 或 OptionalPositiveInt for 年数
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:用药情况 {:optional true} OptionalString]]))

(def 精神神经肌肉系统其他情况详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 神经肌肉其他症状列表]
    [:其他症状描述 {:optional true} OptionalString]]))

(def 精神及神经肌肉系统Spec
  (m/schema
   [:map
    [:精神认知相关疾病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 精神认知相关疾病史详情Spec]]]
    [:癫痫病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 癫痫病史详情Spec]]]
    [:眩晕病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum] ; 注意这里是 有/无
      [:详情 {:optional true} 眩晕病史详情Spec]]]
    [:短暂性脑缺血发作病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 短暂性脑缺血发作病史详情Spec]]]
    [:脑梗病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 脑梗病史详情Spec]]]
    [:脑出血病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 脑出血病史详情Spec]]]
    [:帕金森综合症 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 帕金森综合症详情Spec]]]
    [:颅脑和颈动脉狭窄 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:描述 {:optional true} OptionalString]]] ; 对应 "有（输入内容）"
    [:其他情况 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 精神神经肌肉系统其他情况详情Spec]]]]))

;; 4. 内分泌系统
(def 甲状腺疾病类型Enum (m/schema [:enum :甲亢 :甲减 :甲状腺术后甲状腺素替代治疗 :桥本 :其他]))

(def 甲状腺疾病史详情Spec
  (m/schema
   [:map
    [:类型 {:optional true} 甲状腺疾病类型Enum]
    [:其他类型描述 {:optional true} OptionalString]
    [:甲状腺功能检查 {:optional true} OptionalString]
    [:治疗情况 {:optional true} 治疗情况Enum]
    [:甲状腺是否肿大压迫气管 {:optional true} OptionalBoolean] ; True/False
    [:是否合并甲状腺心脏病 {:optional true} OptionalBoolean]]))

(def 糖尿病病史详情Spec
  (m/schema
   [:map
    [:类型 {:optional true} [:maybe [:enum :1型糖尿病 :2型糖尿病]]]
    [:控制方式 {:optional true} [:maybe [:enum :饮食控制 :药物控制 :胰岛素控制 :未控制]]]
    [:药物详情 {:optional true} OptionalString]
    [:血糖值 {:optional true} OptionalNumber]
    [:糖化血红蛋白值 {:optional true} OptionalNumber]]))

(def 嗜铬细胞瘤详情Spec
  (m/schema
   [:map
    [:控制情况 {:optional true} [:maybe [:enum :药物控制小于2周 :药物控制大于2周 :无症状 :当前存在症状]]]])) ; "当前存在下列症状"可以由UI处理，数据上记录一个状态即可

(def 内分泌系统Spec
  (m/schema
   [:map
    [:甲状腺疾病病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 甲状腺疾病史详情Spec]]]
    [:糖尿病病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 糖尿病病史详情Spec]]]
    [:嗜铬细胞瘤 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 嗜铬细胞瘤详情Spec]]]
    [:皮质醇增多症 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:类型 {:optional true} [:maybe [:enum :肾上腺皮质功能不全 :皮质醇增多症]]]]] ; 将选择放在详情中
    [:痛风 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:描述 {:optional true} OptionalString]]]
    [:垂体功能减退症 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:描述 {:optional true} OptionalString]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 5. 肝肾病史
(def 肝功能详情Spec
  (m/schema
   [:map
    [:谷丙转氨酶ALT {:optional true} OptionalNumber]
    [:谷草转氨酶AST {:optional true} OptionalNumber]
    [:总胆红素TBil {:optional true} OptionalNumber]
    [:直接胆红素DBil {:optional true} OptionalNumber]
    [:碱性磷酸酶ALP {:optional true} OptionalNumber]
    [:γ谷氨酰转肽酶GGT {:optional true} OptionalNumber]
    [:血清白蛋白 {:optional true} OptionalNumber]]))

(def ChildPugh评分项Spec
  (m/schema
   [:map
    [:肝性脑病分期 {:optional true} [:maybe [:enum :无 :1至2期 :3至4期]]] ; 对应1,2,3分
    [:腹水 {:optional true} [:maybe [:enum :无 :轻度 :中度以上]]]
    [:血清胆红素μmolL {:optional true} OptionalNumber] ; 或分段 [:enum :小于34.2 :34.2至51.3 :大于51.3]
    [:血清白蛋白gL {:optional true} OptionalNumber] ; 或分段 [:enum :大于等于35 :28至34 :小于28]
    [:凝血酶原时间延长秒 {:optional true} OptionalNumber]])) ; 或分段/INR

(def 肝脏疾病病史详情Spec
  (m/schema
   [:map
    [:类型 {:optional true} [:maybe [:enum :无 :药物性肝炎 :自身免疫性肝病 :肝囊肿 :原发性肝癌 :病毒性肝炎 :肝硬化 :肝腹水 :继发性肝癌 :肝移植术后 :肝性脑病 :其他]]]
    [:其他类型描述 {:optional true} OptionalString]
    [:ChildPugh评分输入 {:optional true} ChildPugh评分项Spec] ; 医生输入的用于计算Child-Pugh分级的项目
    [:ChildPugh分级结果 {:optional true} [:maybe [:enum :A级 :B级 :C级]]]])) ; 最终计算或选择的Child-Pugh分级

(def 肾功能详情Spec
  (m/schema
   [:map
    [:肌酐Cre {:optional true} OptionalNumber]
    [:血尿素氮BUN {:optional true} OptionalNumber]
    [:肾小球滤过率GFR {:optional true} OptionalNumber]
    [:钾离子K+ {:optional true} OptionalNumber]
    [:症状描述 {:optional true} OptionalString]])) ; "有无少尿、排尿困难、水肿等症状"

(def 肾脏疾病病史详情Spec
  (m/schema
   [:map
    [:类型 {:optional true} [:maybe [:enum :无 :肾小球肾炎 :肾病综合征 :糖尿病肾病 :肾移植术后 :慢性肾脏病 :其他]]]
    [:其他类型描述 {:optional true} OptionalString]
    [:慢性肾脏病分期 {:optional true} [:maybe [:enum :1期 :2期 :3期 :4期 :5期]]]
    [:尿毒症 {:optional true}
     [:map
      [:有无透析治疗 {:optional true} 有无Enum]
      [:最后一次透析时间 {:optional true} Optional日期时间字符串]
      [:透析后肾功能指标 {:optional true} OptionalString]]]]))

(def 肝肾病史Spec
  (m/schema
   [:map
    [:肝功能 {:optional true}
     [:map
      [:状态 {:optional true} [:enum :正常 :异常]]
      [:详情 {:optional true} 肝功能详情Spec]]]
    [:肝脏疾病病史 {:optional true} 肝脏疾病病史详情Spec] ; 已包含类型等
    [:肾功能 {:optional true}
     [:map
      [:状态 {:optional true} [:enum :正常 :异常]]
      [:详情 {:optional true} 肾功能详情Spec]]]
    [:肾脏疾病病史 {:optional true} 肾脏疾病病史详情Spec]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 6. 消化系统
(def 急性胃肠炎病史症状Enum (m/schema [:enum :恶心 :呕吐 :腹泻]))
(def 急性胃肠炎病史症状列表 (m/schema [:vector 急性胃肠炎病史症状Enum]))
(def 急性胃肠炎病史详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 急性胃肠炎病史症状列表]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 食管胃十二指肠疾病症状Enum (m/schema [:enum :慢性胃炎 :胃部肿瘤病史 :反流性食管炎 :慢性胃炎合并幽门螺旋杆菌感染 :溃疡 :食道肿瘤 :息肉 :穿孔 :应激性溃疡 :食管静脉曲张 :胃底静脉曲张]))
(def 食管胃十二指肠疾病症状列表 (m/schema [:vector 食管胃十二指肠疾病症状Enum]))
(def 食管胃十二指肠疾病详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 食管胃十二指肠疾病症状列表]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 慢性消化疾病症状Enum (m/schema [:enum :胰腺炎 :慢性肠炎 :溃疡性结肠炎 :克罗恩病 :肠易激综合征 :肠梗阻 :肠道肿瘤 :胆囊炎 :胆囊结石 :胰腺肿瘤 :其他]))
(def 慢性消化疾病症状列表 (m/schema [:vector 慢性消化疾病症状Enum]))
(def 慢性消化疾病详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 慢性消化疾病症状列表]
    [:其他症状描述 {:optional true} OptionalString]
    [:治疗情况 {:optional true} 治疗情况Enum]]))

(def 消化系统Spec
  (m/schema
   [:map
    [:急性胃肠炎病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 急性胃肠炎病史详情Spec]]]
    [:食管胃十二指肠疾病病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 食管胃十二指肠疾病详情Spec]]]
    [:慢性消化疾病病史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 慢性消化疾病详情Spec]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 7. 血液系统
(def 贫血详情Spec
  (m/schema
   [:map
    [:Hb值 {:optional true} OptionalString] ; 允许输入文本如 ">100"
    [:贫血原因及目前治疗方式 {:optional true} OptionalString]]))

(def 凝血功能障碍详情Spec
  (m/schema
   [:map
    [:PT值 {:optional true} OptionalString]
    [:APTT值 {:optional true} OptionalString]
    [:INR值 {:optional true} OptionalString]
    [:血小板计数 {:optional true} OptionalString]
    [:D二聚体值 {:optional true} OptionalString]]))

(def 血液系统Spec
  (m/schema
   [:map
    [:贫血 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 贫血详情Spec]]]
    [:凝血功能障碍 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:详情 {:optional true} 凝血功能障碍详情Spec]]]
    [:血栓史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:描述 {:optional true} OptionalString]]]
    [:下肢深静脉血栓 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:描述 {:optional true} OptionalString]]]
    [:血管超声 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 8. 免疫系统
(def 免疫功能障碍类型Enum (m/schema [:enum :获得性免疫缺陷 :先天性免疫缺陷 :其他]))
(def 免疫功能障碍详情Spec
  (m/schema
   [:map
    [:类型 {:optional true} 免疫功能障碍类型Enum]
    [:其他类型描述 {:optional true} OptionalString]]))

(def 自身免疫性疾病症状Enum (m/schema [:enum :系统性红斑狼疮 :类风湿性关节炎 :强直性脊柱炎 :过敏性紫癜 :其他]))
(def 自身免疫性疾病症状列表 (m/schema [:vector 自身免疫性疾病症状Enum]))
(def 自身免疫性疾病详情Spec
  (m/schema
   [:map
    [:症状 {:optional true} 自身免疫性疾病症状列表]
    [:其他症状描述 {:optional true} OptionalString]]))

(def 免疫系统Spec
  (m/schema
   [:map
    [:免疫功能障碍 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 免疫功能障碍详情Spec]]]
    [:自身免疫性疾病 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 自身免疫性疾病详情Spec]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 9. 特殊用药史
(def 特殊用药史Spec
  (m/schema
   [:map
    [:抗凝或抗血小板药物 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:糖皮质激素 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:肿瘤治疗 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:药物滥用依赖史 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:神经安定类药物 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:GLP1受体激动剂 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:其他药物使用 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 10. 特殊疾病病史
(def 马方综合征心血管病变Enum (m/schema [:enum :主动脉瘤 :主动脉夹层 :心脏二尖瓣病变 :其他]))
(def 马方综合征心血管病变列表 (m/schema [:vector 马方综合征心血管病变Enum]))
(def 马方综合征骨骼病变Enum (m/schema [:enum :脊柱侧弯 :胸廓畸形 :其他骨骼畸形]))
(def 马方综合征骨骼病变列表 (m/schema [:vector 马方综合征骨骼病变Enum]))

(def 马方综合征详情Spec
  (m/schema
   [:map
    [:眼部病变晶状体脱位 {:optional true} OptionalBoolean]
    [:心血管病变 {:optional true} 马方综合征心血管病变列表]
    [:骨骼病变 {:optional true} 马方综合征骨骼病变列表]]))

(def 特殊疾病病史Spec
  (m/schema
   [:map
    [:马方综合征 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 马方综合征详情Spec]]]
    [:其他特殊疾病 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 11. 营养评估
(def 营养评分Spec
  (m/schema
   [:map
    [:BMI小于20点5 {:optional true} 有无Enum]
    [:过去1至3个月体重下降 {:optional true} 有无Enum]
    [:过去1周摄食减少 {:optional true} 有无Enum]
    [:有严重疾病如ICU治疗 {:optional true} 有无Enum]
    [:总评分 {:optional true} OptionalPositiveInt] ; 医生计算或选择
    [:风险等级 {:optional true} [:maybe [:enum :营养正常 :轻度围术期营养风险 :存在围术期营养风险]] ]])) ; 对应说明

(def FRAIL衰弱评估Spec
  (m/schema
   [:map
    [:疲乏 {:optional true} 有无Enum]
    [:阻力增加或耐力减退 {:optional true} 有无Enum]
    [:自由活动下降 {:optional true} 有无Enum]
    [:存在5种以上疾病 {:optional true} 有无Enum] ; 具体疾病列表为参考
    [:体重下降1年或更短内大于5百分比 {:optional true} 有无Enum]
    [:总评分 {:optional true} OptionalPositiveInt]
    [:结论 {:optional true} [:maybe [:enum :营养正常无衰弱 :营养正常衰弱前期 :营养不良衰弱前期 :营养不良衰弱]]]]))

(def 营养评估Spec
  (m/schema
   [:map
    [:营养评分 {:optional true} 营养评分Spec]
    [:FRAIL针对大于60岁病人 {:optional true} FRAIL衰弱评估Spec]]))

;; 12. 妊娠
(def 妊娠合并产科情况Enum (m/schema [:enum :单胎 :多胎 :妊娠期糖尿病 :妊娠期高血压 :周围型前置胎盘 :中央型前置胎盘 :胎膜早破 :胎盘早剥 :胎盘植入 :先兆流产 :子痫前期 :子痫 :其他]))
(def 妊娠合并产科情况列表 (m/schema [:vector 妊娠合并产科情况Enum]))
(def 妊娠详情Spec
  (m/schema
   [:map
    [:孕周 {:optional true} [:maybe [:enum :0至12周 :13至28周 :大于28周]]]
    [:孕产史 {:optional true} [:map [:足月 {:optional true} OptionalPositiveInt]
                               [:早产 {:optional true} OptionalPositiveInt]
                               [:流产 {:optional true} OptionalPositiveInt]
                               [:存活 {:optional true} OptionalPositiveInt]]]
    [:合并产科情况 {:optional true} 妊娠合并产科情况列表]
    [:其他产科情况描述 {:optional true} OptionalString]]))

(def 妊娠Spec
  (m/schema
   [:map
    [:是否妊娠 {:optional true} 有无不详Enum]
    [:详情 {:optional true} 妊娠详情Spec]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 13. 手术麻醉史
(def 术后并发症Enum (m/schema [:enum :术后恶心呕吐 :术后疼痛 :声音嘶哑 :头晕头疼 :其他]))
(def 术后并发症列表 (m/schema [:vector 术后并发症Enum]))
(def 不良事件Enum (m/schema [:enum :过敏反应 :困难气道 :气管切开 :术中知晓 :术后认知功能障碍 :恶性高热 :其他]))
(def 不良事件列表 (m/schema [:vector 不良事件Enum]))

(def 手术麻醉史详情Spec
  (m/schema
   [:map
    [:上次麻醉日期范围 {:optional true} [:maybe [:enum :大于5年 :1至5年 :小于1年]]]
    [:具体上次麻醉日期 {:optional true} Optional日期字符串]
    [:麻醉方式 {:optional true} [:maybe [:enum :全身麻醉 :椎管内麻醉 :神经阻滞 :局部麻醉]]]
    [:术后并发症 {:optional true} 术后并发症列表]
    [:其他术后并发症描述 {:optional true} OptionalString]
    [:不良事件 {:optional true} 不良事件列表]
    [:其他不良事件描述 {:optional true} OptionalString]
    [:已行手术 {:optional true} OptionalString]]))

(def 手术麻醉史Spec
  (m/schema
   [:map
    [:手术麻醉史 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:详情 {:optional true} 手术麻醉史详情Spec]]]
    [:有血缘关系的人发生过恶性高热史 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:关系人描述 {:optional true} OptionalString]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 14. 气道评估
(def 张口度Enum (m/schema [:enum :大于等于3横指 :2点5横指 :2横指 :1点5横指 :1横指 :无法张口]))
(def 头颈活动度Enum (m/schema [:enum :正常活动 :前后活动受限 :左右活动受限]))
(def 下颌前突试验Enum (m/schema [:enum :正常下牙移过上牙 :中度上下牙平齐 :重度下牙无法移过上牙]))
(def 上唇咬合试验Enum (m/schema [:enum :1级 :2级 :3级]))
(def 改良Mallampati分级Enum (m/schema [:enum :Ⅰ级 :Ⅱ级 :Ⅲ级 :Ⅳ级]))
(def 牙齿评估Enum (m/schema [:enum :牙齿完好牙列整齐 :牙齿完好牙列不齐 :戴冠 :稳定 :松动 :缺失 :固定假牙 :活动假牙 :残根牙 :带气管导管入手术室 :未查]))
(def 综合征Enum (m/schema [:enum :颅面短小症 :VanDerWoude综合征 :TreacherCollins综合征 :PierreRobin序列征 :黏多糖病 :Nager综合征 :Beckwith综合征 :Apert综合征 :Binder综合征 :KlippelFeil综合征 :腭心面综合征 :Down综合征 :Stickler综合征 :Tunner综合征 :Marshall综合征 :呆小症 :其他]))
(def 综合征列表 (m/schema [:vector 综合征Enum]))
(def 鼾症症状Enum (m/schema [:enum :偶尔鼾声 :睡眠时有憋醒 :经常鼾声 :睡眠时无憋醒 :既往诊断为OSA]))
(def 鼾症症状列表 (m/schema [:vector 鼾症症状Enum]))
(def 上呼吸道疾病Enum (m/schema [:enum :声带麻痹 :声带息肉 :喉乳头状瘤 :口咽部肿物 :其他]))
(def 上呼吸道疾病列表 (m/schema [:vector 上呼吸道疾病Enum]))
(def 下呼吸道疾病Enum (m/schema [:enum :气道狭窄 :气管异物 :气管食管瘘 :气管憩室 :气切病史 :其他]))
(def 下呼吸道疾病列表 (m/schema [:vector 下呼吸道疾病Enum]))
(def 纵隔位置Enum (m/schema [:enum :前 :中 :后 :不详]))
(def 现存气道症状Enum (m/schema [:enum :呼吸困难 :吞咽困难 :口鼻出血 :声音嘶哑 :慢性咳嗽 :咽痛 :咳痰 :咽部异物感 :咯血痰中带血 :喉梗阻 :鼻塞 :喝水呛咳 :其他]))
(def 现存气道症状列表 (m/schema [:vector 现存气道症状Enum]))
(def 喉梗阻分级Enum (m/schema [:enum :Ⅰ度 :Ⅱ度 :Ⅲ度 :Ⅳ度]))

(def 气道评估Spec
  (m/schema
   [:map
    [:既往困难通气史 {:optional true} [:maybe [:enum :无 :有 :疑似 :不详]]]
    [:既往困难插管史 {:optional true} [:maybe [:enum :无 :有 :疑似 :不详]]]
    [:张口度 {:optional true}
     [:map
      [:分级 {:optional true} 张口度Enum]
      [:原因 {:optional true} OptionalString]
      [:颞颌关节强直 {:optional true} OptionalBoolean]
      [:外伤 {:optional true} OptionalBoolean]
      [:外科手术后 {:optional true} OptionalBoolean]
      [:手术类型描述 {:optional true} OptionalString]
      [:放疗史 {:optional true} 有无Enum]]]
    [:头颈活动度 {:optional true}
     [:map
      [:分级 {:optional true} 头颈活动度Enum]
      [:原因 {:optional true} OptionalString]
      [:颈部手术史 {:optional true} OptionalBoolean]
      [:手术类型描述 {:optional true} OptionalString]
      [:头颈放疗史 {:optional true} 有无Enum]
      [:最近一次放疗时间 {:optional true} Optional日期字符串]
      [:颈部肿物 {:optional true} OptionalBoolean]
      [:强制性脊柱炎 {:optional true} OptionalBoolean]
      [:其他原因 {:optional true} OptionalString]]]
    [:甲颏距离cm {:optional true} OptionalNumber]
    [:下颌前突试验 {:optional true} 下颌前突试验Enum]
    [:上唇咬合试验ULBT {:optional true} 上唇咬合试验Enum]
    [:改良Mallampati分级 {:optional true} 改良Mallampati分级Enum]
    [:牙齿评估 {:optional true}
     [:map
      [:状况 {:optional true} 牙齿评估Enum]
      [:松动具体位置 {:optional true} OptionalString]
      [:缺失具体位置 {:optional true} OptionalString]]]
    [:特殊面部特征 {:optional true}
     [:map
      [:络腮胡 {:optional true} 有无Enum]
      [:巨舌症 {:optional true} 有无Enum]
      [:小下颌 {:optional true} 有无Enum]
      [:综合征列表 {:optional true} 综合征列表]
      [:其他综合征描述 {:optional true} OptionalString]]]
    [:鼾症 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:症状 {:optional true} 鼾症症状列表]]]
    [:气道相关疾病 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:上呼吸道 {:optional true}
       [:map
        [:有无 {:optional true} 有无Enum]
        [:疾病列表 {:optional true} 上呼吸道疾病列表]
        [:其他疾病描述 {:optional true} OptionalString]]]
      [:下呼吸道 {:optional true}
       [:map
        [:有无 {:optional true} 有无Enum]
        [:疾病列表 {:optional true} 下呼吸道疾病列表]
        [:其他疾病描述 {:optional true} OptionalString]]]]]
    [:纵隔病史 {:optional true}
     [:map
      [:纵隔肿瘤 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:位置 {:optional true} 纵隔位置Enum]]]
      [:纵隔感染 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:位置 {:optional true} 纵隔位置Enum]]]]]
    [:现存气道症状 {:optional true}
     [:map
      [:有无 {:optional true} 有无不详Enum]
      [:症状列表 {:optional true} 现存气道症状列表]
      [:其他症状描述 {:optional true} OptionalString]
      [:喉梗阻分级 {:optional true} 喉梗阻分级Enum]]]
    [:气道压迫史 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:气道手术史 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:食管手术史 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:是否存在返流 {:optional true} OptionalBoolean]]]
    [:气切史 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:喘鸣 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]
    [:颈部放疗史总结 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]] ; 重复，但遵循org文档
    [:喉镜检查 {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:头颈或胸部CT检查 {:optional true} [:map [:内容 {:optional true} OptionalString]]]
    [:其他情况 {:optional true} [:map [:内容 {:optional true} OptionalString]]]]))

;; 15. 椎管内麻醉相关评估
(def 椎管内麻醉相关评估Spec
  (m/schema
   [:map
    [:中枢神经系统 {:optional true}
     [:map
      [:脑肿瘤 {:optional true} 有无Enum]
      [:脑出血 {:optional true} 有无Enum]
      [:严重颅脑外伤 {:optional true} 有无Enum]
      [:癫痫 {:optional true} 有无Enum]]]
    [:外周神经系统 {:optional true}
     [:map
      [:多发性硬化 {:optional true} 有无Enum]
      [:脊髓损伤 {:optional true} 有无Enum]
      [:脊柱侧弯 {:optional true} 有无Enum]
      [:脊柱畸形 {:optional true} 有无Enum]
      [:椎管内肿瘤 {:optional true} 有无Enum]
      [:强制性脊柱炎 {:optional true} 有无Enum]
      [:腰椎手术史 {:optional true} 有无Enum]]]
    [:腰椎间盘突出 {:optional true}
     [:map
      [:有无 {:optional true} 有无Enum]
      [:下肢麻木症状 {:optional true} 有无Enum]]]
    [:心血管系统 {:optional true} ; 与椎管内麻醉相关的心血管情况
     [:map
      [:主动脉瓣狭窄 {:optional true} 有无Enum]
      [:肥厚型梗阻型心肌病 {:optional true} 有无Enum]
      [:抗凝或抗血小板药物 {:optional true} [:map [:有无 {:optional true} 有无Enum] [:描述 {:optional true} OptionalString]]]]]
    [:穿刺点检查 {:optional true}
     [:map
      [:既往穿刺困难史 {:optional true} 有无Enum]
      [:局部感染 {:optional true} 有无Enum]
      [:畸形 {:optional true} 有无Enum]]]
    [:局麻药过敏 {:optional true} 有无Enum]]))


;; --- 整体评估数据 Spec (使用中文关键字) ---
(def 患者评估数据Spec
  (m/schema
   [:map {:closed true}               ; 推荐使用 closed，防止意外的键
    ;; 基本信息部分 (可能由患者填写，医生补充)
    [:基本信息
     [:map
      [:门诊号 NonEmptyString]
      [:姓名 NonEmptyString]
      [:身份证号 OptionalString]
      [:手机号 OptionalString]
      [:性别 [:enum "男" "女" "其他"]]  ; 必填
      [:年龄 [:int {:min 0 :max 150}]]  ; 必填
      [:院区 OptionalString]            ; 或使用枚举
      ;; 以下可能由系统或医生填写/更新
      [:患者提交时间 {:optional true} Optional日期时间字符串]
      [:评估更新时间 {:optional true} Optional日期时间字符串]
      [:评估状态 {:optional true} [:enum "待评估" "已批准" "已驳回" "已暂缓" "评估中"]]
      [:医生姓名 {:optional true} OptionalString]
      [:评估备注 {:optional true} OptionalString]
      [:身高cm {:optional true} OptionalNumber]          ; 医生补充
      [:体重kg {:optional true} OptionalNumber]          ; 医生补充
      [:精神状态 {:optional true} OptionalString]        ; 医生补充
      [:活动能力 {:optional true} OptionalString]        ; 医生补充
      [:血压mmHg {:optional true} OptionalString]        ; 如 "120/80"，医生补充
      [:脉搏次每分 {:optional true} OptionalPositiveInt] ; 医生补充
      [:呼吸次每分 {:optional true} OptionalPositiveInt] ; 医生补充
      [:体温摄氏度 {:optional true} OptionalNumber]      ; 医生补充
      [:SpO2百分比 {:optional true} OptionalPositiveInt] ; 医生补充
      [:术前诊断 {:optional true} OptionalString]        ; 医生补充
      [:拟施手术 {:optional true} OptionalString]        ; 医生补充
      ]]

    ;; 各系统评估 (大部分由医生填写或确认)
    [:循环系统 {:optional true} 循环系统Spec]
    [:呼吸系统 {:optional true} 呼吸系统Spec]
    [:精神及神经肌肉系统 {:optional true} 精神及神经肌肉系统Spec]
    [:内分泌系统 {:optional true} 内分泌系统Spec]
    [:肝肾病史 {:optional true} 肝肾病史Spec] ; 注意：org文件名与内容标题不完全对应，这里用内容标题
    [:消化系统 {:optional true} 消化系统Spec]
    [:血液系统 {:optional true} 血液系统Spec]
    [:免疫系统 {:optional true} 免疫系统Spec]
    [:特殊用药史 {:optional true} 特殊用药史Spec]
    [:特殊疾病病史 {:optional true} 特殊疾病病史Spec]
    [:营养评估 {:optional true} 营养评估Spec]
    [:妊娠 {:optional true} 妊娠Spec]
    [:手术麻醉史 {:optional true} 手术麻醉史Spec]
    [:气道评估 {:optional true} 气道评估Spec]
    [:椎管内麻醉相关评估 {:optional true} 椎管内麻醉相关评估Spec]

    ;; 麻醉评估与医嘱部分 (医生填写)
    [:麻醉评估与医嘱 {:optional true}
     [:map
      [:ASA分级 {:optional true} OptionalString] ; 或枚举 e.g., [:enum "Ⅰ级" "Ⅱ级" ...]
      [:心功能分级NYHA {:optional true} OptionalString] ; 或枚举
      [:拟行麻醉方式 {:optional true} NonEmptyString]
      [:监测项目 {:optional true} NonEmptyString]
      [:特殊技术 {:optional true} OptionalString]
      [:其他麻醉相关 {:optional true} OptionalString] ; "其他"
      [:术前麻醉医嘱 {:optional true} NonEmptyString]
      [:术日晨继续应用药物 {:optional true} NonEmptyString]
      [:术日晨停用药物 {:optional true} NonEmptyString]
      [:麻醉前用药 {:optional true} NonEmptyString]
      [:麻醉中需注意的问题 {:optional true} NonEmptyString]]]

    ;; 审批状态等元数据
    [:审批状态 {:optional true}
     [:map
      [:状态 {:optional true} [:enum :待评估 :已批准 :已暂缓 :已驳回]]
      [:批准人 {:optional true} OptionalString]
      [:批准时间 {:optional true} Optional日期时间字符串]
      [:驳回人 {:optional true} OptionalString]
      [:驳回时间 {:optional true} Optional日期时间字符串]
      [:驳回原因 {:optional true} OptionalString]
      [:暂缓人 {:optional true} OptionalString]
      [:暂缓时间 {:optional true} Optional日期时间字符串]
      [:暂缓原因 {:optional true} OptionalString]]]
    [:知情同意书 {:optional true}       ; 根据需求文档添加
     [:map
      [:状态 {:optional true} [:maybe [:enum :未签署 :已签署 :待签署]]]
      [:签署时间 {:optional true} Optional日期时间字符串]
      [:签署医生 {:optional true} OptionalString]
      [:内容或模板引用 {:optional true} OptionalString]]]]))
