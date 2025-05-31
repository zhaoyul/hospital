(ns hc.hospital.specs.assessment-complete-cn-spec-test
  (:require [clojure.test :refer :all]
            [malli.core :as m]
            [malli.generator :as mg] ; Added for generator
            [hc.hospital.specs.assessment-complete-cn :as cn-spec]))

(deftest non-empty-string-test
  (is (m/validate cn-spec/NonEmptyString "hello"))
  (is (m/validate cn-spec/NonEmptyString " a "))
  (is (not (m/validate cn-spec/NonEmptyString "")))
  (is (not (m/validate cn-spec/NonEmptyString nil)))
  (is (not (m/validate cn-spec/NonEmptyString 123))))

(deftest optional-string-test
  (is (m/validate cn-spec/OptionalString "hello"))
  (is (m/validate cn-spec/OptionalString ""))
  (is (m/validate cn-spec/OptionalString nil))
  (is (not (m/validate cn-spec/OptionalString 123))))

(deftest optional-positive-int-test
  (is (m/validate cn-spec/OptionalPositiveInt 0))
  (is (m/validate cn-spec/OptionalPositiveInt 123))
  (is (m/validate cn-spec/OptionalPositiveInt nil))
  (is (not (m/validate cn-spec/OptionalPositiveInt -1)))
  (is (not (m/validate cn-spec/OptionalPositiveInt 1.5)))
  (is (not (m/validate cn-spec/OptionalPositiveInt "123"))))

(deftest optional-number-test
  (is (m/validate cn-spec/OptionalNumber 123))
  (is (m/validate cn-spec/OptionalNumber 1.23))
  (is (m/validate cn-spec/OptionalNumber -1.23))
  (is (m/validate cn-spec/OptionalNumber 0.0))
  (is (m/validate cn-spec/OptionalNumber nil))
  (is (not (m/validate cn-spec/OptionalNumber "1.23"))))

(deftest optional-boolean-test
  (is (m/validate cn-spec/OptionalBoolean true))
  (is (m/validate cn-spec/OptionalBoolean false))
  (is (m/validate cn-spec/OptionalBoolean nil))
  (is (not (m/validate cn-spec/OptionalBoolean "true")))
  (is (not (m/validate cn-spec/OptionalBoolean 0))))

(deftest date-string-spec-test
  (is (m/validate cn-spec/日期字符串Spec "2023-01-01"))
  (is (m/validate cn-spec/日期字符串Spec "1999-12-31"))
  (is (not (m/validate cn-spec/日期字符串Spec "2023/01/01")))
  (is (not (m/validate cn-spec/日期字符串Spec "01-01-2023")))
  (is (not (m/validate cn-spec/日期字符串Spec "2023-1-1")))
  (is (not (m/validate cn-spec/日期字符串Spec "2023-01-01T10:00:00")))
  (is (not (m/validate cn-spec/日期字符串Spec nil))))

(deftest optional-date-string-test
  (is (m/validate cn-spec/Optional日期字符串 "2023-01-01"))
  (is (m/validate cn-spec/Optional日期字符串 nil))
  (is (not (m/validate cn-spec/Optional日期字符串 "2023/01/01"))))

(deftest optional-datetime-string-test
  (is (m/validate cn-spec/Optional日期时间字符串 "2023-01-01T12:00:00Z"))
  (is (m/validate cn-spec/Optional日期时间字符串 "2023-01-01T12:00:00.123Z"))
  (is (m/validate cn-spec/Optional日期时间字符串 "2023-01-01T12:00Z")) ; No seconds
  (is (m/validate cn-spec/Optional日期时间字符串 "2023-01-01T12:00")) ; No Z
  (is (m/validate cn-spec/Optional日期时间字符串 nil))
  (is (not (m/validate cn-spec/Optional日期时间字符串 "2023-01-01 12:00:00")))
  (is (not (m/validate cn-spec/Optional日期时间字符串 "2023/01/01T12:00:00Z"))))

(deftest有-无-不详-enum-test
  (is (m/validate cn-spec/有无不详Enum :有))
  (is (m/validate cn-spec/有无不详Enum :无))
  (is (m/validate cn-spec/有无不详Enum :不详))
  (is (not (m/validate cn-spec/有无不详Enum "有")))
  (is (not (m/validate cn-spec/有无不详Enum :未知)))
  (is (not (m/validate cn-spec/有无不详Enum nil))))

(deftest有-无-enum-test
  (is (m/validate cn-spec/有无Enum :有))
  (is (m/validate cn-spec/有无Enum :无))
  (is (not (m/validate cn-spec/有无Enum :不详)))
  (is (not (m/validate cn-spec/有无Enum "有")))
  (is (not (m/validate cn-spec/有无Enum nil))))

(deftest 治疗情况-enum-test
  (is (m/validate cn-spec/治疗情况Enum :治愈))
  (is (m/validate cn-spec/治疗情况Enum :好转))
  (is (m/validate cn-spec/治疗情况Enum :仍有症状))
  (is (m/validate cn-spec/治疗情况Enum :未治疗))
  (is (m/validate cn-spec/治疗情况Enum :病情稳定))
  (is (m/validate cn-spec/治疗情况Enum :其他))
  (is (not (m/validate cn-spec/治疗情况Enum "治愈")))
  (is (not (m/validate cn-spec/治疗情况Enum :无效)))
  (is (not (m/validate cn-spec/治疗情况Enum nil))))

(deftest 是否-enum-test
  (is (m/validate cn-spec/是否Enum :是))
  (is (m/validate cn-spec/是否Enum :否))
  (is (not (m/validate cn-spec/是否Enum "是")))
  (is (not (m/validate cn-spec/是否Enum :不确定)))
  (is (not (m/validate cn-spec/是否Enum nil))))

;; --- 循环系统 Tests ---

(deftest 高血压详情Spec-test
  (is (m/validate cn-spec/高血压详情Spec
                  {:病史时长 :小于1年
                   :分级 :Ⅰ级
                   :治疗 {:类型 :规律服药血压控制良好 :用药详情 "Aspirin"}})
      "Valid full 高血压详情Spec")
  (is (m/validate cn-spec/高血压详情Spec {}) "Valid empty 高血压详情Spec (all optional)")
  (is (m/validate cn-spec/高血压详情Spec {:病史时长 :1至5年}) "Valid partially filled 高血压详情Spec")
  (is (m/validate cn-spec/高血压详情Spec (mg/generate cn-spec/高血压详情Spec)) "Generated valid 高血压详情Spec"))

(deftest 高血压详情Spec-invalid-test
  (is (not (m/validate cn-spec/高血压详情Spec {:病史时长 :invalid-enum})) "Invalid enum for 病史时长")
  (is (not (m/validate cn-spec/高血压详情Spec {:分级 123})) "Invalid type for 分级")
  (is (not (m/validate cn-spec/高血压详情Spec {:治疗 {:类型 :invalid-type}})) "Invalid enum for 治疗类型")
  (is (not (m/validate cn-spec/高血压详情Spec {:治疗 {:用药详情 123}})) "Invalid type for 用药详情"))

(deftest 血管疾病详情Spec-test
  (is (m/validate cn-spec/血管疾病详情Spec
                  {:高血压 {:病史时长 :小于1年 :分级 :Ⅰ级}
                   :高血脂 {:有无 :有}
                   :低血压 {:有无 :无}})
      "Valid full 血管疾病详情Spec")
  (is (m/validate cn-spec/血管疾病详情Spec {}) "Valid empty 血管疾病详情Spec")
  (is (m/validate cn-spec/血管疾病详情Spec {:动脉瘤 {:有无 :有}}) "Valid partial 血管疾病详情Spec")
  (is (m/validate cn-spec/血管疾病详情Spec (mg/generate cn-spec/血管疾病详情Spec)) "Generated 血管疾病详情Spec"))

(deftest 血管疾病详情Spec-invalid-test
  (is (not (m/validate cn-spec/血管疾病详情Spec {:高血压 {:病史时长 :invalid}})) "Invalid nested 高血压详情Spec")
  (is (not (m/validate cn-spec/血管疾病详情Spec {:高血脂 {:有无 :不详}})) "Invalid enum for 高血脂 有无")
  (is (not (m/validate cn-spec/血管疾病详情Spec {:动脉瘤 123})) "Invalid type for 动脉瘤"))

(deftest 心脏疾病通用详情Spec-test
  (is (m/validate cn-spec/心脏疾病通用详情Spec
                  {:有无 :有
                   :描述 "Mild condition"
                   :治疗情况 :好转
                   :治疗用药 "Beta blockers"})
      "Valid full 心脏疾病通用详情Spec")
  (is (m/validate cn-spec/心脏疾病通用详情Spec {:有无 :无}) "Valid minimal 心脏疾病通用详情Spec")
  (is (m/validate cn-spec/心脏疾病通用详情Spec {}) "Valid empty 心脏疾病通用详情Spec")
  (is (m/validate cn-spec/心脏疾病通用详情Spec (mg/generate cn-spec/心脏疾病通用详情Spec)) "Generated 心脏疾病通用详情Spec"))

(deftest 心脏疾病通用详情Spec-invalid-test
  (is (not (m/validate cn-spec/心脏疾病通用详情Spec {:有无 :不详})) "Invalid enum for 有无")
  (is (not (m/validate cn-spec/心脏疾病通用详情Spec {:描述 123})) "Invalid type for 描述")
  (is (not (m/validate cn-spec/心脏疾病通用详情Spec {:治疗情况 :invalid-enum})) "Invalid enum for 治疗情况"))

(deftest 冠心病详情Spec-test
  (is (m/validate cn-spec/冠心病详情Spec
                  {:有无 :有
                   :症状 :稳定性心绞痛
                   :心脏支架 {:有无 :有}
                   :治疗情况 :病情稳定
                   :治疗用药 "Nitroglycerin"})
      "Valid full 冠心病详情Spec")
  (is (m/validate cn-spec/冠心病详情Spec {:有无 :无}) "Valid minimal 冠心病详情Spec")
  (is (m/validate cn-spec/冠心病详情Spec {}) "Valid empty 冠心病详情Spec")
  (is (m/validate cn-spec/冠心病详情Spec (mg/generate cn-spec/冠心病详情Spec)) "Generated 冠心病详情Spec"))

(deftest 冠心病详情Spec-invalid-test
  (is (not (m/validate cn-spec/冠心病详情Spec {:有无 :不详})) "Invalid enum for 有无")
  (is (not (m/validate cn-spec/冠心病详情Spec {:症状 :invalid-symptom})) "Invalid enum for 症状")
  (is (not (m/validate cn-spec/冠心病详情Spec {:心脏支架 {:有无 :不详}})) "Invalid enum for 心脏支架 有无"))

(deftest 心律失常详情Spec-test
  (is (m/validate cn-spec/心律失常详情Spec
                  {:有无 :有
                   :类型 :低危型
                   :描述 "Occasional palpitations"
                   :治疗情况 :好转
                   :治疗用药 "Amiodarone"})
      "Valid full 心律失常详情Spec")
  (is (m/validate cn-spec/心律失常详情Spec {:有无 :不详}) "Valid minimal 心律失常详情Spec with 不详")
  (is (m/validate cn-spec/心律失常详情Spec {}) "Valid empty 心律失常详情Spec")
  (is (m/validate cn-spec/心律失常详情Spec (mg/generate cn-spec/心律失常详情Spec)) "Generated 心律失常详情Spec"))

(deftest 心律失常详情Spec-invalid-test
  (is (not (m/validate cn-spec/心律失常详情Spec {:类型 :invalid-type})) "Invalid enum for 类型")
  (is (not (m/validate cn-spec/心律失常详情Spec {:描述 123})) "Invalid type for 描述")
  (is (not (m/validate cn-spec/心律失常详情Spec {:治疗用药 []})) "Invalid type for 治疗用药"))

(deftest 充血性心力衰竭史详情Spec-test
  (is (m/validate cn-spec/充血性心力衰竭史详情Spec
                  {:有无 :有
                   :上次发作日期 "2022-01-01"
                   :治疗情况 :仍有症状
                   :治疗用药 "Diuretics"})
      "Valid full 充血性心力衰竭史详情Spec")
  (is (m/validate cn-spec/充血性心力衰竭史详情Spec {:有无 :无}) "Valid minimal 充血性心力衰竭史详情Spec")
  (is (m/validate cn-spec/充血性心力衰竭史详情Spec {}) "Valid empty 充血性心力衰竭史详情Spec")
  (is (m/validate cn-spec/充血性心力衰竭史详情Spec (mg/generate cn-spec/充血性心力衰竭史详情Spec)) "Generated 充血性心力衰竭史详情Spec"))

(deftest 充血性心力衰竭史详情Spec-invalid-test
  (is (not (m/validate cn-spec/充血性心力衰竭史详情Spec {:有无 :不详})) "Invalid enum for 有无")
  (is (not (m/validate cn-spec/充血性心力衰竭史详情Spec {:上次发作日期 "invalid-date"})) "Invalid format for 上次发作日期")
  (is (not (m/validate cn-spec/充血性心力衰竭史详情Spec {:治疗情况 :invalid-status})) "Invalid enum for 治疗情况"))

(deftest 循环系统Spec-test
  (is (m/validate cn-spec/循环系统Spec {}) "Valid empty 循环系统Spec")
  (is (m/validate cn-spec/循环系统Spec
                  {:心电图 {:描述 "正常"}
                   :血管疾病史 {:有无 :有 :详情 {:高血压 {:病史时长 :大于10年 :分级 :Ⅲ级}}}
                   :心脏疾病史 {:有无 :有
                                :详情 {:冠心病 {:有无 :有 :症状 :心梗 :治疗情况 :病情稳定}
                                        :心律失常 {:有无 :不详}
                                        :心肌病 {:有无 :无}
                                        :肺动脉高压 {:有无 :有 :描述 "轻度" :治疗情况 :好转}}}
                   :心脏起搏器植入史 {:有无 :无}
                   :心脏彩超检查 {:结果 "左室射血分数55%"}
                   :冠脉CTA或冠脉造影结果 {:结果 "左前降支狭窄50%"}
                   :心脏功能评估 {:NYHA分级 :Ⅱ级}
                   :运动能力评估 {:METs水平 :3至6MET}
                   :其他情况 {:内容 "患者自述活动后偶有胸闷"}})
      "Valid comprehensive 循环系统Spec")
  (is (m/validate cn-spec/循环系统Spec (mg/generate cn-spec/循环系统Spec {:size 5})) "Generated 循环系统Spec with size 5")
  (is (m/validate cn-spec/循环系统Spec (mg/generate cn-spec/循环系统Spec)) "Generated 循环系统Spec default size"))

(deftest 循环系统Spec-invalid-test
  (is (not (m/validate cn-spec/循环系统Spec {:心电图 {:描述 123}})) "Invalid 心电图描述 type")
  (is (not (m/validate cn-spec/循环系统Spec {:血管疾病史 {:有无 :maybe}})) "Invalid enum in 血管疾病史 有无")
  (is (not (m/validate cn-spec/循环系统Spec {:血管疾病史 {:详情 {:高血压 {:病史时长 :way-too-long}}}}))
      "Invalid enum in nested 高血压 病史时长")
  (is (not (m/validate cn-spec/循环系统Spec {:心脏疾病史 {:有无 :是的}})) "Invalid enum in 心脏疾病史 有无")
  (is (not (m/validate cn-spec/循环系统Spec {:心脏疾病史 {:详情 {:冠心病 {:有无 :是的}}}}))
      "Invalid enum in nested 冠心病 有无")
  (is (not (m/validate cn-spec/循环系统Spec {:心脏疾病史 {:详情 {:冠心病 {:症状 :non-existent-symptom}}}}))
      "Invalid enum in nested 冠心病 症状")
  (is (not (m/validate cn-spec/循环系统Spec {:心脏起搏器植入史 {:详情 {:类型 :super-pacemaker}}}))
      "Invalid enum in nested 心脏起搏器植入史 类型")
  (is (not (m/validate cn-spec/循环系统Spec {:心脏功能评估 {:NYHA分级 :V级}})) "Invalid enum in 心脏功能评估 NYHA分级")
  (is (not (m/validate cn-spec/循环系统Spec {:运动能力评估 {:METs水平 :over-9000MET}})) "Invalid enum in 运动能力评估 METs水平")
  (is (not (m/validate cn-spec/循环系统Spec {:其他情况 {:内容 []}})) "Invalid type for 其他情况 内容"))

;; --- 呼吸系统 Tests ---

(deftest 呼吸系统症状列表-test
  (is (m/validate cn-spec/呼吸系统症状列表 [:咳嗽 :发热 :咳痰]))
  (is (m/validate cn-spec/呼吸系统症状列表 [])) ; Empty list is valid
  (is (m/validate cn-spec/呼吸系统症状列表 [:其他]))
  (is (m/validate cn-spec/呼吸系统症状列表 (mg/generate cn-spec/呼吸系统症状列表 {:min 1 :max 3})))
  (is (not (m/validate cn-spec/呼吸系统症状列表 [:咳嗽 :invalid-symptom])))
  (is (not (m/validate cn-spec/呼吸系统症状列表 ["咳嗽"])))
  (is (not (m/validate cn-spec/呼吸系统症状列表 nil)))) ; nil is not a vector

(deftest 近两周内感冒病史详情Spec-test
  (is (m/validate cn-spec/近两周内感冒病史详情Spec
                  {:发病日期 "2023-01-01"
                   :症状 [:咳嗽 :发热]
                   :其他不适症状 "头痛"
                   :治疗情况 :好转})
      "Valid full 近两周内感冒病史详情Spec")
  (is (m/validate cn-spec/近两周内感冒病史详情Spec {}) "Valid empty 近两周内感冒病史详情Spec (all optional)")
  (is (m/validate cn-spec/近两周内感冒病史详情Spec {:症状 [:咽痛]}) "Valid partial 近两周内感冒病史详情Spec")
  (is (m/validate cn-spec/近两周内感冒病史详情Spec (mg/generate cn-spec/近两周内感冒病史详情Spec)) "Generated 近两周内感冒病史详情Spec"))

(deftest 近两周内感冒病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/近两周内感冒病史详情Spec {:发病日期 "invalid-date"})) "Invalid date format for 发病日期")
  (is (not (m/validate cn-spec/近两周内感冒病史详情Spec {:症状 [:invalid-symptom]})) "Invalid enum in 症状")
  (is (not (m/validate cn-spec/近两周内感冒病史详情Spec {:其他不适症状 123})) "Invalid type for 其他不适症状")
  (is (not (m/validate cn-spec/近两周内感冒病史详情Spec {:治疗情况 :invalid-enum})) "Invalid enum for 治疗情况"))

(deftest 近一个月内支气管炎或肺炎病史详情Spec-test
  (is (m/validate cn-spec/近一个月内支气管炎或肺炎病史详情Spec
                  {:发病日期 "2023-01-15"
                   :治疗情况 :治愈})
      "Valid full 近一个月内支气管炎或肺炎病史详情Spec")
  (is (m/validate cn-spec/近一个月内支气管炎或肺炎病史详情Spec {}) "Valid empty (all optional)")
  (is (m/validate cn-spec/近一个月内支气管炎或肺炎病史详情Spec (mg/generate cn-spec/近一个月内支气管炎或肺炎病史详情Spec)) "Generated 近一个月内支气管炎或肺炎病史详情Spec"))

(deftest 近一个月内支气管炎或肺炎病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/近一个月内支气管炎或肺炎病史详情Spec {:发病日期 123})) "Invalid type for 发病日期")
  (is (not (m/validate cn-spec/近一个月内支气管炎或肺炎病史详情Spec {:治疗情况 :non-existent})) "Invalid enum for 治疗情况"))

(deftest 哮喘病史详情Spec-test
  (is (m/validate cn-spec/哮喘病史详情Spec
                  {:上次发作日期 "2022-12-01"
                   :治疗情况 :临床缓解期
                   :用药情况 :激素类药物
                   :用药详情 "Budesonide inhaler"})
      "Valid full 哮喘病史详情Spec")
  (is (m/validate cn-spec/哮喘病史详情Spec {}) "Valid empty 哮喘病史详情Spec")
  (is (m/validate cn-spec/哮喘病史详情Spec (mg/generate cn-spec/哮喘病史详情Spec)) "Generated 哮喘病史详情Spec"))

(deftest 哮喘病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/哮喘病史详情Spec {:上次发作日期 "yesterday"})) "Invalid date format for 上次发作日期")
  (is (not (m/validate cn-spec/哮喘病史详情Spec {:治疗情况 :cured})) "Invalid enum for 治疗情况") ; English value
  (is (not (m/validate cn-spec/哮喘病史详情Spec {:用药情况 "pills"})) "Invalid enum for 用药情况") ; String instead of keyword
  (is (not (m/validate cn-spec/哮喘病史详情Spec {:用药详情 ["too", "many", "details"]})) "Invalid type for 用药详情"))

(deftest 慢性阻塞性肺疾病详情Spec-test
  (is (m/validate cn-spec/慢性阻塞性肺疾病详情Spec {:治疗情况 :稳定期}) "Valid full 慢性阻塞性肺疾病详情Spec")
  (is (m/validate cn-spec/慢性阻塞性肺疾病详情Spec {}) "Valid empty 慢性阻塞性肺疾病详情Spec")
  (is (m/validate cn-spec/慢性阻塞性肺疾病详情Spec (mg/generate cn-spec/慢性阻塞性肺疾病详情Spec)) "Generated 慢性阻塞性肺疾病详情Spec"))

(deftest 慢性阻塞性肺疾病详情Spec-invalid-test
  (is (not (m/validate cn-spec/慢性阻塞性肺疾病详情Spec {:治疗情况 :bad-state})) "Invalid enum for 治疗情况"))

(deftest 呼吸系统通用疾病详情Spec-test
  (is (m/validate cn-spec/呼吸系统通用疾病详情Spec {:治疗情况 :好转}) "Valid full 呼吸系统通用疾病详情Spec")
  (is (m/validate cn-spec/呼吸系统通用疾病详情Spec {}) "Valid empty 呼吸系统通用疾病详情Spec")
  (is (m/validate cn-spec/呼吸系统通用疾病详情Spec (mg/generate cn-spec/呼吸系统通用疾病详情Spec)) "Generated 呼吸系统通用疾病详情Spec"))

(deftest 呼吸系统通用疾病详情Spec-invalid-test
  (is (not (m/validate cn-spec/呼吸系统通用疾病详情Spec {:治疗情况 :unknown})) "Invalid enum for 治疗情况"))

(deftest 呼吸系统Spec-test
  (is (m/validate cn-spec/呼吸系统Spec {}) "Valid empty 呼吸系统Spec")
  (is (m/validate cn-spec/呼吸系统Spec
                  {:近两周内感冒病史 {:有无 :有 :详情 {:发病日期 "2023-10-10" :症状 [:咳嗽] :治疗情况 :好转}}
                   :近一个月内支气管炎或肺炎病史 {:有无 :无}
                   :哮喘病史 {:有无 :不详}
                   :慢性阻塞性肺疾病 {:有无 :有 :详情 {:治疗情况 :稳定期}}
                   :支气管扩张症 {:有无 :无}
                   :肺部结节 {:有无 :有 :详情 {:治疗情况 :仍有症状}}
                   :肺部肿瘤 {:有无 :无}
                   :胸片 {:内容 "未见异常"}
                   :胸部CT {:内容 "肺纹理清晰"}
                   :肺功能 {:内容 "通气功能良好"}
                   :血气分析 {:内容 "氧分压正常"}
                   :是否有肺结核 :否
                   :其他情况 {:内容 "患者冬季易发呼吸道感染"}})
      "Valid comprehensive 呼吸系统Spec")
  (is (m/validate cn-spec/呼吸系统Spec (mg/generate cn-spec/呼吸系统Spec {:size 5})) "Generated 呼吸系统Spec with size 5")
   (is (m/validate cn-spec/呼吸系统Spec (mg/generate cn-spec/呼吸系统Spec)) "Generated 呼吸系统Spec default size"))

(deftest 呼吸系统Spec-invalid-test
  (is (not (m/validate cn-spec/呼吸系统Spec {:近两周内感冒病史 {:有无 :yes}})) "Invalid enum in 近两周内感冒病史 有无")
  (is (not (m/validate cn-spec/呼吸系统Spec {:近两周内感冒病史 {:有无 :有 :详情 {:症状 ["string-instead-of-keyword"]}}}))
      "Invalid type in nested 近两周内感冒病史 症状")
  (is (not (m/validate cn-spec/呼吸系统Spec {:哮喘病史 {:详情 {:上次发作日期 true}}}))
      "Invalid type in nested 哮喘病史 上次发作日期")
  (is (not (m/validate cn-spec/呼吸系统Spec {:慢性阻塞性肺疾病 {:有无 :不详 :详情 {:治疗情况 :very-stable}}}))
      "Invalid enum in nested 慢性阻塞性肺疾病 治疗情况")
  (is (not (m/validate cn-spec/呼吸系统Spec {:是否有肺结核 :不详})) "Invalid enum for 是否有肺结核 (expects 有无Enum)")
  (is (not (m/validate cn-spec/呼吸系统Spec {:胸片 {:内容 []}})) "Invalid type for 胸片 内容")
  (is (not (m/validate cn-spec/呼吸系统Spec {:其他情况 {:内容 123213}})) "Invalid type for 其他情况 内容"))

;; --- 精神及神经肌肉系统 Tests ---

(deftest 精神认知症状列表-test
  (is (m/validate cn-spec/精神认知症状列表 [:焦虑症 :抑郁症]))
  (is (m/validate cn-spec/精神认知症状列表 []))
  (is (m/validate cn-spec/精神认知症状列表 [:其他]))
  (is (m/validate cn-spec/精神认知症状列表 (mg/generate cn-spec/精神认知症状列表 {:min 1 :max 2})))
  (is (not (m/validate cn-spec/精神认知症状列表 [:not-a-symptom])))
  (is (not (m/validate cn-spec/精神认知症状列表 ["焦虑症"]))))

(deftest 神经肌肉其他症状列表-test
  (is (m/validate cn-spec/神经肌肉其他症状列表 [:重症肌无力 :帕金森病史]))
  (is (m/validate cn-spec/神经肌肉其他症状列表 []))
  (is (m/validate cn-spec/神经肌肉其他症状列表 [:其他]))
  (is (m/validate cn-spec/神经肌肉其他症状列表 (mg/generate cn-spec/神经肌肉其他症状列表 {:min 1 :max 2})))
  (is (not (m/validate cn-spec/神经肌肉其他症状列表 [:not-a-symptom])))
  (is (not (m/validate cn-spec/神经肌肉其他症状列表 ["重症肌无力"]))))

(deftest 精神认知相关疾病史详情Spec-test
  (is (m/validate cn-spec/精神认知相关疾病史详情Spec
                  {:症状 [:焦虑症 :睡眠障碍]
                   :其他症状描述 "轻微记忆力下降"
                   :治疗情况 :病情稳定
                   :治疗用药 "Sertraline"})
      "Valid full 精神认知相关疾病史详情Spec")
  (is (m/validate cn-spec/精神认知相关疾病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/精神认知相关疾病史详情Spec (mg/generate cn-spec/精神认知相关疾病史详情Spec)) "Generated"))

(deftest 精神认知相关疾病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/精神认知相关疾病史详情Spec {:症状 [:invalid-cognitive-symptom]})))
  (is (not (m/validate cn-spec/精神认知相关疾病史详情Spec {:其他症状描述 123})))
  (is (not (m/validate cn-spec/精神认知相关疾病史详情Spec {:治疗情况 :bad}))))

(deftest 癫痫病史详情Spec-test
  (is (m/validate cn-spec/癫痫病史详情Spec
                  {:近期发作日期 "2023-05-15"
                   :治疗情况 :病情稳定 ; Uses 治疗情况Enum
                   :治疗用药 "Valproate"})
      "Valid full 癫痫病史详情Spec")
  (is (m/validate cn-spec/癫痫病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/癫痫病史详情Spec (mg/generate cn-spec/癫痫病史详情Spec)) "Generated"))

(deftest 癫痫病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/癫痫病史详情Spec {:近期发作日期 12345})))
  (is (not (m/validate cn-spec/癫痫病史详情Spec {:治疗情况 :non-existent-enum})))
  (is (not (m/validate cn-spec/癫痫病史详情Spec {:治疗用药 []})))))

(deftest 眩晕病史详情Spec-test
  (is (m/validate cn-spec/眩晕病史详情Spec
                  {:近期发作日期 "2023-06-01"
                   :治疗情况 :好转})
      "Valid full 眩晕病史详情Spec")
  (is (m/validate cn-spec/眩晕病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/眩晕病史详情Spec (mg/generate cn-spec/眩晕病史详情Spec)) "Generated"))

(deftest 眩晕病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/眩晕病史详情Spec {:近期发作日期 "today"})))
  (is (not (m/validate cn-spec/眩晕病史详情Spec {:治疗情况 :dizzy})))))

(deftest 短暂性脑缺血发作病史详情Spec-test
  (is (m/validate cn-spec/短暂性脑缺血发作病史详情Spec
                  {:近期发作情况 :近3月内无发作
                   :治疗情况 :病情稳定})
      "Valid full 短暂性脑缺血发作病史详情Spec")
  (is (m/validate cn-spec/短暂性脑缺血发作病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/短暂性脑缺血发作病史详情Spec (mg/generate cn-spec/短暂性脑缺血发作病史详情Spec)) "Generated"))

(deftest 短暂性脑缺血发作病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/短暂性脑缺血发作病史详情Spec {:近期发作情况 :sometimes})))
  (is (not (m/validate cn-spec/短暂性脑缺血发作病史详情Spec {:治疗情况 123})))))

(deftest 脑梗病史详情Spec-test
  (is (m/validate cn-spec/脑梗病史详情Spec
                  {:近期发作日期 "2022-11-20"
                   :治疗情况 :仍有症状
                   :目前用药 "Aspirin, Clopidogrel"})
      "Valid full 脑梗病史详情Spec")
  (is (m/validate cn-spec/脑梗病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/脑梗病史详情Spec (mg/generate cn-spec/脑梗病史详情Spec)) "Generated"))

(deftest 脑梗病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/脑梗病史详情Spec {:近期发作日期 false})))
  (is (not (m/validate cn-spec/脑梗病史详情Spec {:治疗情况 :recovering})))
  (is (not (m/validate cn-spec/脑梗病史详情Spec {:目前用药 ["Aspirin", "Clopidogrel"]})))))

(deftest 脑出血病史详情Spec-test
  (is (m/validate cn-spec/脑出血病史详情Spec
                  {:近期发作日期 "2021-03-10"
                   :治疗情况 :好转})
      "Valid full 脑出血病史详情Spec")
  (is (m/validate cn-spec/脑出血病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/脑出血病史详情Spec (mg/generate cn-spec/脑出血病史详情Spec)) "Generated"))

(deftest 脑出血病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/脑出血病史详情Spec {:近期发作日期 "unknown"})))
  (is (not (m/validate cn-spec/脑出血病史详情Spec {:治疗情况 :severe})))))

(deftest 帕金森综合症详情Spec-test
  (is (m/validate cn-spec/帕金森综合症详情Spec
                  {:诊断年限 "2020-01-01" ; Assuming date of diagnosis, could also be number of years
                   :治疗情况 :病情稳定
                   :用药情况 "Levodopa"})
      "Valid full 帕金森综合症详情Spec")
  (is (m/validate cn-spec/帕金森综合症详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/帕金森综合症详情Spec (mg/generate cn-spec/帕金森综合症详情Spec)) "Generated"))

(deftest 帕金森综合症详情Spec-invalid-test
  (is (not (m/validate cn-spec/帕金森综合症详情Spec {:诊断年限 5}))) ; Assuming string for date or description
  (is (not (m/validate cn-spec/帕金森综合症详情Spec {:治疗情况 :shaky})))
  (is (not (m/validate cn-spec/帕金森综合症详情Spec {:用药情况 {:meds ["Levodopa"]}})))))

(deftest 精神神经肌肉系统其他情况详情Spec-test
  (is (m/validate cn-spec/精神神经肌肉系统其他情况详情Spec
                  {:症状 [:重症肌无力 :其他]
                   :其他症状描述 "肌无力，晨轻暮重"})
      "Valid full 精神神经肌肉系统其他情况详情Spec")
  (is (m/validate cn-spec/精神神经肌肉系统其他情况详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/精神神经肌肉系统其他情况详情Spec (mg/generate cn-spec/精神神经肌肉系统其他情况详情Spec)) "Generated"))

(deftest 精神神经肌肉系统其他情况详情Spec-invalid-test
  (is (not (m/validate cn-spec/精神神经肌肉系统其他情况详情Spec {:症状 [:not-a-neuro-symptom]})))
  (is (not (m/validate cn-spec/精神神经肌肉系统其他情况详情Spec {:其他症状描述 true})))))

(deftest 精神及神经肌肉系统Spec-test
  (is (m/validate cn-spec/精神及神经肌肉系统Spec {}) "Valid empty 精神及神经肌肉系统Spec")
  (is (m/validate cn-spec/精神及神经肌肉系统Spec
                  {:精神认知相关疾病史 {:有无 :有 :详情 {:症状 [:焦虑症 :抑郁症] :治疗情况 :好转 :治疗用药 "Fluoxetine"}}
                   :癫痫病史 {:有无 :不详}
                   :眩晕病史 {:有无 :无}
                   :短暂性脑缺血发作病史 {:有无 :有 :详情 {:近期发作情况 :近3月内无发作 :治疗情况 :病情稳定}}
                   :脑梗病史 {:有无 :无}
                   :脑出血病史 {:有无 :否} ; Assuming :否 is valid for 有无Enum
                   :帕金森综合症 {:有无 :是 :详情 {:诊断年限 "2019-01-01" :治疗情况 :病情稳定 :用药情况 "Madopar"}}
                   :颅脑和颈动脉狭窄 {:有无 :不详 :描述 "颈内动脉轻度狭窄"}
                   :其他情况 {:有无 :无}})
      "Valid comprehensive 精神及神经肌肉系统Spec")
  (is (m/validate cn-spec/精神及神经肌肉系统Spec (mg/generate cn-spec/精神及神经肌肉系统Spec {:size 6})) "Generated 精神及神经肌肉系统Spec"))

(deftest 精神及神经肌肉系统Spec-invalid-test
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:精神认知相关疾病史 {:有无 :maybe}})) "Invalid enum in 精神认知相关疾病史 有无")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:精神认知相关疾病史 {:有无 :有 :详情 {:症状 ["not-a-keyword"]}}}))
      "Invalid type in nested 精神认知相关疾病史 症状")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:癫痫病史 {:有无 :有 :详情 {:近期发作日期 123}}}))
      "Invalid type in nested 癫痫病史 近期发作日期")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:眩晕病史 {:有无 :不详}}))
      "Invalid enum for 眩晕病史 有无 (expects 有无Enum, not 有无不详Enum)")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:短暂性脑缺血发作病史 {:有无 :有 :详情 {:近期发作情况 :always}}}))
      "Invalid enum in nested 短暂性脑缺血发作病史 近期发作情况")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:脑梗病史 {:有无 :不详}}))
      "Invalid enum for 脑梗病史 有无 (expects 有无Enum)")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:颅脑和颈动脉狭窄 {:描述 []}})) "Invalid type for 颅脑和颈动脉狭窄 描述")
  (is (not (m/validate cn-spec/精神及神经肌肉系统Spec {:其他情况 {:有无 :有 :详情 {:症状 ["wrong-place"]}}}))
      "Invalid structure for 其他情况 详情 (symptoms do not belong here directly without a proper key)")))

;; --- 内分泌系统 Tests ---

(deftest 甲状腺疾病类型Enum-test
  (is (m/validate cn-spec/甲状腺疾病类型Enum :甲亢))
  (is (m/validate cn-spec/甲状腺疾病类型Enum :甲减))
  (is (m/validate cn-spec/甲状腺疾病类型Enum :甲状腺术后甲状腺素替代治疗))
  (is (m/validate cn-spec/甲状腺疾病类型Enum :桥本))
  (is (m/validate cn-spec/甲状腺疾病类型Enum :其他))
  (is (not (m/validate cn-spec/甲状腺疾病类型Enum :甲状腺炎)))
  (is (not (m/validate cn-spec/甲状腺疾病类型Enum "甲亢"))))

(deftest 甲状腺疾病史详情Spec-test
  (is (m/validate cn-spec/甲状腺疾病史详情Spec
                  {:类型 :甲亢
                   :其他类型描述 nil ; or "Graves' disease"
                   :甲状腺功能检查 "T3 T4 TSH normal"
                   :治疗情况 :病情稳定
                   :甲状腺是否肿大压迫气管 false
                   :是否合并甲状腺心脏病 false})
      "Valid full 甲状腺疾病史详情Spec")
  (is (m/validate cn-spec/甲状腺疾病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/甲状腺疾病史详情Spec {:类型 :其他 :其他类型描述 "单纯性甲状腺肿"}) "Valid with 其他 type")
  (is (m/validate cn-spec/甲状腺疾病史详情Spec (mg/generate cn-spec/甲状腺疾病史详情Spec)) "Generated"))

(deftest 甲状腺疾病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:类型 :甲状腺肿大})) "Invalid enum for 类型")
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:其他类型描述 123})) "Invalid type for 其他类型描述")
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:甲状腺功能检查 []})) "Invalid type for 甲状腺功能检查")
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:治疗情况 :cured})) "Invalid enum for 治疗情况")
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:甲状腺是否肿大压迫气管 "no"})) "Invalid type for 甲状腺是否肿大压迫气管")
  (is (not (m/validate cn-spec/甲状腺疾病史详情Spec {:是否合并甲状腺心脏病 "unknown"})) "Invalid type for 是否合并甲状腺心脏病"))

(deftest 糖尿病病史详情Spec-test
  (is (m/validate cn-spec/糖尿病病史详情Spec
                  {:类型 :1型糖尿病
                   :控制方式 :胰岛素控制
                   :药物详情 "Insulin Regular, Lantus"
                   :血糖值 7.8
                   :糖化血红蛋白值 6.5})
      "Valid full 糖尿病病史详情Spec")
  (is (m/validate cn-spec/糖尿病病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/糖尿病病史详情Spec (mg/generate cn-spec/糖尿病病史详情Spec)) "Generated"))

(deftest 糖尿病病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/糖尿病病史详情Spec {:类型 :type3})) "Invalid enum for 类型")
  (is (not (m/validate cn-spec/糖尿病病史详情Spec {:控制方式 :magic})) "Invalid enum for 控制方式")
  (is (not (m/validate cn-spec/糖尿病病史详情Spec {:药物详情 100})) "Invalid type for 药物详情")
  (is (not (m/validate cn-spec/糖尿病病史详情Spec {:血糖值 "high"})) "Invalid type for 血糖值")
  (is (not (m/validate cn-spec/糖尿病病史详情Spec {:糖化血红蛋白值 "6.5%"})) "Invalid type for 糖化血红蛋白值 (should be number)"))

(deftest 嗜铬细胞瘤详情Spec-test
  (is (m/validate cn-spec/嗜铬细胞瘤详情Spec {:控制情况 :药物控制大于2周}) "Valid full 嗜铬细胞瘤详情Spec")
  (is (m/validate cn-spec/嗜铬细胞瘤详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/嗜铬细胞瘤详情Spec (mg/generate cn-spec/嗜铬细胞瘤详情Spec)) "Generated"))

(deftest 嗜铬细胞瘤详情Spec-invalid-test
  (is (not (m/validate cn-spec/嗜铬细胞瘤详情Spec {:控制情况 :not-sure})) "Invalid enum for 控制情况"))

(deftest 内分泌系统Spec-test
  (is (m/validate cn-spec/内分泌系统Spec {}) "Valid empty 内分泌系统Spec")
  (is (m/validate cn-spec/内分泌系统Spec
                  {:甲状腺疾病病史 {:有无 :有 :详情 {:类型 :甲亢 :治疗情况 :好转 :甲状腺是否肿大压迫气管 false}}
                   :糖尿病病史 {:有无 :不详}
                   :嗜铬细胞瘤 {:有无 :无}
                   :皮质醇增多症 {:有无 :有 :类型 :皮质醇增多症}
                   :痛风 {:有无 :不详 :描述 "发作频率低"}
                   :垂体功能减退症 {:有无 :无}
                   :其他情况 {:内容 "无特殊"}})
      "Valid comprehensive 内分泌系统Spec")
  (is (m/validate cn-spec/内分泌系统Spec (mg/generate cn-spec/内分泌系统Spec {:size 4})) "Generated 内分泌系统Spec"))

(deftest 内分泌系统Spec-invalid-test
  (is (not (m/validate cn-spec/内分泌系统Spec {:甲状腺疾病病史 {:有无 :maybe}})) "Invalid enum in 甲状腺疾病病史 有无")
  (is (not (m/validate cn-spec/内分泌系统Spec {:甲状腺疾病病史 {:有无 :有 :详情 {:类型 :hyper}}})) "Invalid enum in nested 甲状腺疾病病史 类型")
  (is (not (m/validate cn-spec/内分泌系统Spec {:甲状腺疾病病史 {:有无 :有 :详情 {:甲状腺是否肿大压迫气管 "yes"}}}))
      "Invalid type for nested 甲状腺是否肿大压迫气管 (expects boolean)")
  (is (not (m/validate cn-spec/内分泌系统Spec {:糖尿病病史 {:有无 :有 :详情 {:血糖值 "high"}}}))
      "Invalid type for nested 血糖值")
  (is (not (m/validate cn-spec/内分泌系统Spec {:糖尿病病史 {:详情 {:血糖值 7.5}}}))
      "Missing :有无 for 糖尿病病史 when :详情 is present (though schema allows optional :有无, good practice to have it if details exist)")
  (is (not (m/validate cn-spec/内分泌系统Spec {:嗜铬细胞瘤 {:有无 :yes}})) "Invalid enum in 嗜铬细胞瘤 有无 (expects :有 or :无)")
  (is (not (m/validate cn-spec/内分泌系统Spec {:皮质醇增多症 {:有无 :有 :类型 :invalid-cortisol-type}}))
      "Invalid enum in 皮质醇增多症 类型")
  (is (not (m/validate cn-spec/内分泌系统Spec {:痛风 {:有无 :有 :描述 []}})) "Invalid type for 痛风 描述")
  (is (not (m/validate cn-spec/内分泌系统Spec {:其他情况 {:内容 123}})) "Invalid type for 其他情况 内容"))

;; --- 肝肾病史 Tests ---

(deftest 肝功能详情Spec-test
  (is (m/validate cn-spec/肝功能详情Spec
                  {:谷丙转氨酶ALT 40.5
                   :谷草转氨酶AST 35.2
                   :总胆红素TBil 10.1
                   :直接胆红素DBil 5.0
                   :碱性磷酸酶ALP 100.0
                   :γ谷氨酰转肽酶GGT 45.0
                   :血清白蛋白 4.2})
      "Valid full 肝功能详情Spec")
  (is (m/validate cn-spec/肝功能详情Spec {}) "Valid empty (all optional)")
  (is (m/validate cn-spec/肝功能详情Spec {:谷丙转氨酶ALT 25.0}) "Valid partial")
  (is (m/validate cn-spec/肝功能详情Spec (mg/generate cn-spec/肝功能详情Spec)) "Generated"))

(deftest 肝功能详情Spec-invalid-test
  (is (not (m/validate cn-spec/肝功能详情Spec {:谷丙转氨酶ALT "high"})) "Invalid type for 谷丙转氨酶ALT")
  (is (not (m/validate cn-spec/肝功能详情Spec {:总胆红素TBil "normal"})) "Invalid type for 总胆红素TBil")
  (is (not (m/validate cn-spec/肝功能详情Spec {:血清白蛋白 "4.0g/dL"})) "Invalid type for 血清白蛋白"))

(deftest ChildPugh评分项Spec-test
  (is (m/validate cn-spec/ChildPugh评分项Spec
                  {:肝性脑病分期 :无
                   :腹水 :轻度
                   :血清胆红素μmolL 30.0
                   :血清白蛋白gL 36.0
                   :凝血酶原时间延长秒 3.0})
      "Valid full ChildPugh评分项Spec")
  (is (m/validate cn-spec/ChildPugh评分项Spec {}) "Valid empty")
  (is (m/validate cn-spec/ChildPugh评分项Spec (mg/generate cn-spec/ChildPugh评分项Spec)) "Generated"))

(deftest ChildPugh评分项Spec-invalid-test
  (is (not (m/validate cn-spec/ChildPugh评分项Spec {:肝性脑病分期 :stage1})) "Invalid enum for 肝性脑病分期")
  (is (not (m/validate cn-spec/ChildPugh评分项Spec {:腹水 "none"})) "Invalid enum for 腹水")
  (is (not (m/validate cn-spec/ChildPugh评分项Spec {:血清胆红素μmolL "high"})) "Invalid type for 血清胆红素μmolL"))

(deftest 肝脏疾病病史详情Spec-test
  (is (m/validate cn-spec/肝脏疾病病史详情Spec
                  {:类型 :肝硬化
                   :其他类型描述 nil
                   :ChildPugh评分输入 {:肝性脑病分期 :1至2期 :腹水 :中度 :血清胆红素μmolL 40.0}
                   :ChildPugh分级结果 :B级})
      "Valid full 肝脏疾病病史详情Spec")
  (is (m/validate cn-spec/肝脏疾病病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/肝脏疾病病史详情Spec {:类型 :其他 :其他类型描述 "脂肪肝"}) "Valid with 其他 type")
  (is (m/validate cn-spec/肝脏疾病病史详情Spec (mg/generate cn-spec/肝脏疾病病史详情Spec)) "Generated"))

(deftest 肝脏疾病病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/肝脏疾病病史详情Spec {:类型 :fatty-liver})) "Invalid enum for 类型")
  (is (not (m/validate cn-spec/肝脏疾病病史详情Spec {:ChildPugh评分输入 {:腹水 :severe}})) "Invalid enum in nested ChildPugh评分输入")
  (is (not (m/validate cn-spec/肝脏疾病病史详情Spec {:ChildPugh分级结果 :D级})) "Invalid enum for ChildPugh分级结果"))

(deftest 肾功能详情Spec-test
  (is (m/validate cn-spec/肾功能详情Spec
                  {:肌酐Cre 1.2
                   :血尿素氮BUN 20.0
                   :肾小球滤过率GFR 60.0
                   :钾离子K+ 4.0
                   :症状描述 "无明显水肿"})
      "Valid full 肾功能详情Spec")
  (is (m/validate cn-spec/肾功能详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/肾功能详情Spec (mg/generate cn-spec/肾功能详情Spec)) "Generated"))

(deftest 肾功能详情Spec-invalid-test
  (is (not (m/validate cn-spec/肾功能详情Spec {:肌酐Cre "normal"})) "Invalid type for 肌酐Cre")
  (is (not (m/validate cn-spec/肾功能详情Spec {:肾小球滤过率GFR "low"})) "Invalid type for 肾小球滤过率GFR")
  (is (not (m/validate cn-spec/肾功能详情Spec {:症状描述 123})) "Invalid type for 症状描述"))

(deftest 肾脏疾病病史详情Spec-test
  (is (m/validate cn-spec/肾脏疾病病史详情Spec
                  {:类型 :慢性肾脏病
                   :其他类型描述 nil
                   :慢性肾脏病分期 :3期
                   :尿毒症 {:有无透析治疗 :有
                             :最后一次透析时间 "2023-10-01T14:30:00Z"
                             :透析后肾功能指标 "肌酐 2.0"}})
      "Valid full 肾脏疾病病史详情Spec")
  (is (m/validate cn-spec/肾脏疾病病史详情Spec {}) "Valid empty")
  (is (m/validate cn-spec/肾脏疾病病史详情Spec {:类型 :其他 :其他类型描述 "肾结石"}) "Valid with 其他 type")
  (is (m/validate cn-spec/肾脏疾病病史详情Spec (mg/generate cn-spec/肾脏疾病病史详情Spec)) "Generated"))

(deftest 肾脏疾病病史详情Spec-invalid-test
  (is (not (m/validate cn-spec/肾脏疾病病史详情Spec {:类型 :kidney-stone})) "Invalid enum for 类型")
  (is (not (m/validate cn-spec/肾脏疾病病史详情Spec {:慢性肾脏病分期 :stage3})) "Invalid enum for 慢性肾脏病分期")
  (is (not (m/validate cn-spec/肾脏疾病病史详情Spec {:尿毒症 {:有无透析治疗 :是的}})) "Invalid enum in nested 尿毒症 有无透析治疗")
  (is (not (m/validate cn-spec/肾脏疾病病史详情Spec {:尿毒症 {:最后一次透析时间 "yesterday"}})) "Invalid datetime format in 尿毒症"))

(deftest 肝肾病史Spec-test
  (is (m/validate cn-spec/肝肾病史Spec {}) "Valid empty 肝肾病史Spec")
  (is (m/validate cn-spec/肝肾病史Spec
                  {:肝功能 {:状态 :异常 :详情 {:谷丙转氨酶ALT 150.0 :血清白蛋白 3.0}}
                   :肝脏疾病病史 {:类型 :肝硬化 :ChildPugh分级结果 :B级 :ChildPugh评分输入 {:腹水 :中度}}
                   :肾功能 {:状态 :正常 :详情 {:肌酐Cre 0.9}}
                   :肾脏疾病病史 {:类型 :糖尿病肾病 :慢性肾脏病分期 :3期 :尿毒症 {:有无透析治疗 :无}}
                   :其他情况 {:内容 "患者有长期饮酒史"}})
      "Valid comprehensive 肝肾病史Spec")
  (is (m/validate cn-spec/肝肾病史Spec (mg/generate cn-spec/肝肾病史Spec {:size 5})) "Generated 肝肾病史Spec"))

(deftest 肝肾病史Spec-invalid-test
  (is (not (m/validate cn-spec/肝肾病史Spec {:肝功能 {:状态 :invalid-status}})) "Invalid enum in 肝功能 状态")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肝功能 {:详情 {:谷丙转氨酶ALT "very high"}}}))
      "Invalid type in nested 肝功能详情Spec, and missing :状态 for 肝功能")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肝脏疾病病史 {:类型 :unknown-liver-disease}})) "Invalid enum in 肝脏疾病病史 类型")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肝脏疾病病史 {:ChildPugh分级结果 :D级}})) "Invalid enum in 肝脏疾病病史 ChildPugh分级结果")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肾功能 {:状态 :ok}})) "Invalid enum in 肾功能 状态")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肾脏疾病病史 {:类型 :无 :尿毒症 {:有无透析治疗 :是的}}}))
      "Invalid enum :是的 in nested 尿毒症 (expects :有 or :无 from 有无Enum)")
  (is (not (m/validate cn-spec/肝肾病史Spec {:肾脏疾病病史 {:慢性肾脏病分期 :6期}})) "Invalid enum in 肾脏疾病病史 慢性肾脏病分期")
  (is (not (m/validate cn-spec/肝肾病史Spec {:其他情况 {:内容 ["details"]}})) "Invalid type for 其他情况 内容"))
