(ns hc.hospital.pages.assessment-cards-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [hc.hospital.pages.assessment-cards :as cards]))

(deftest all-summaries-test
  (testing "generate-circulatory-summary"
    (is (= (cards/generate-circulatory-summary nil) "循环系统: 无数据"))
    (is (= (cards/generate-circulatory-summary {}) "循环系统: 无数据"))
    (is (= (cards/generate-circulatory-summary
            {:cardiac_disease_history {:has "无"}
             :pacemaker_history {:has "无"}
             :cardiac_function_assessment {:class "Ⅰ 级"}
             :exercise_capacity_assessment {:level "运动能力正常"}})
           "循环系统: 心脏病史:无, 起搏器:无, 心功能:正常, 运动能力:正常"))
    (is (= (cards/generate-circulatory-summary
            {:cardiac_disease_history {:has "有"}
             :pacemaker_history {:has "有"}
             :cardiac_function_assessment {:class "Ⅲ 级"}
             :exercise_capacity_assessment {:level "运动能力明显下降"}})
           "循环系统: 心脏病史:异常, 起搏器:有, 心功能:Ⅲ 级, 运动能力:运动能力明显下降"))
    (is (= (cards/generate-circulatory-summary
            {:cardiac_disease_history {:has "无"}})
           "循环系统: 心脏病史:无, 起搏器:无, 心功能:未评估, 运动能力:未评估")))

  (testing "generate-respiratory-summary"
    (is (= (cards/generate-respiratory-summary nil) "呼吸系统: 无数据"))
    (is (= (cards/generate-respiratory-summary {}) "呼吸系统: 无数据"))
    (is (= (cards/generate-respiratory-summary
            {:cold_history_last_2_weeks {:present "无"}
             :bronchitis_pneumonia_last_month {:present "无"}
             :asthma_history {:present "无"}
             :copd_history {:present "无"}
             :tuberculosis_history {:present "无"}})
           "呼吸系统: 近期感冒:无, 近期支气管炎/肺炎:无, 哮喘病史:无, COPD病史:无, 肺结核:无"))
    (is (= (cards/generate-respiratory-summary
            {:cold_history_last_2_weeks {:present "有"}
             :bronchitis_pneumonia_last_month {:present "无"}
             :asthma_history {:present "有"}
             :copd_history {:present "无"}
             :tuberculosis_history {:present "无"}})
           "呼吸系统: 近期感冒:有, 近期支气管炎/肺炎:无, 哮喘病史:有, COPD病史:无, 肺结核:无"))
    (is (= (cards/generate-respiratory-summary
            {:cold_history_last_2_weeks {:present "有"}
             :bronchitis_pneumonia_last_month {:present "有"}
             :asthma_history {:present "有"}
             :copd_history {:present "有"}
             :tuberculosis_history {:present "有"}})
           "呼吸系统: 近期感冒:有, 近期支气管炎/肺炎:有, 哮喘病史:有, COPD病史:有, 肺结核:有")))

  (testing "generate-mental-neuromuscular-summary"
    (is (= (cards/generate-mental-neuromuscular-summary nil) "精神及神经肌肉系统: 无数据"))
    (is (= (cards/generate-mental-neuromuscular-summary {}) "精神及神经肌肉系统: 无数据"))
    (is (= (cards/generate-mental-neuromuscular-summary
            {:psycho_cognitive_history {:present "无"}
             :epilepsy_history {:present "无"}
             :cerebral_infarction_history {:present "无"}})
           "精神及神经肌肉系统: 精神认知史:无, 癫痫史:无, 脑梗史:无"))
    (is (= (cards/generate-mental-neuromuscular-summary
            {:psycho_cognitive_history {:present "有"}
             :epilepsy_history {:present "无"}
             :cerebral_infarction_history {:present "有"}})
           "精神及神经肌肉系统: 精神认知史:有, 癫痫史:无, 脑梗史:有")))

  (testing "generate-endocrine-summary"
    (is (= (cards/generate-endocrine-summary nil) "内分泌系统: 无数据"))
    (is (= (cards/generate-endocrine-summary {}) "内分泌系统: 无数据"))
    (is (= (cards/generate-endocrine-summary
            {:thyroid_disease_history {:present "无"}
             :diabetes_history {:present "无"}
             :pheochromocytoma {:present "无"}})
           "内分泌系统: 甲状腺疾病:无, 糖尿病:无, 嗜铬细胞瘤:无"))
    (is (= (cards/generate-endocrine-summary
            {:thyroid_disease_history {:present "有"}
             :diabetes_history {:present "有"}
             :pheochromocytoma {:present "无"}})
           "内分泌系统: 甲状腺疾病:有, 糖尿病:有, 嗜铬细胞瘤:无")))

  (testing "generate-liver-kidney-summary"
    (is (= (cards/generate-liver-kidney-summary nil) "肝肾病史: 无数据"))
    (is (= (cards/generate-liver-kidney-summary {}) "肝肾病史: 无数据"))
    (is (= (cards/generate-liver-kidney-summary
            {:liver_function {:status "正常"}
             :liver_disease_history {:types ["none"]}})
           "肝肾病史: 肝功能:正常, 肝脏疾病:无"))
    (is (= (cards/generate-liver-kidney-summary
            {:liver_function {:status "正常"}
             :liver_disease_history {:types []}})
           "肝肾病史: 肝功能:正常, 肝脏疾病:无"))
    (is (= (cards/generate-liver-kidney-summary
            {:liver_function {:status "异常"}
             :liver_disease_history {:types ["drug_induced_hepatitis"]}})
           "肝肾病史: 肝功能:异常, 肝脏疾病:有"))
    (is (= (cards/generate-liver-kidney-summary
            {:liver_function {:status "正常"}
             :liver_disease_history {:types ["none" "autoimmune_liver_disease"]}})
           "肝肾病史: 肝功能:正常, 肝脏疾病:有")))

  (testing "generate-digestive-summary"
    (is (= (cards/generate-digestive-summary nil) "消化系统: 无数据"))
    (is (= (cards/generate-digestive-summary {}) "消化系统: 无数据"))
    (is (= (cards/generate-digestive-summary
            {:acute_gastroenteritis_history {:has "无"}
             :esophageal_gastric_duodenal_history {:has "无"}
             :chronic_digestive_history {:has "无"}})
           "消化系统: 急性胃肠炎:无, 食管胃十二指肠疾病:无, 慢性消化疾病:无"))
    (is (= (cards/generate-digestive-summary
            {:acute_gastroenteritis_history {:has "有"}
             :esophageal_gastric_duodenal_history {:has "无"}
             :chronic_digestive_history {:has "有"}})
           "消化系统: 急性胃肠炎:有, 食管胃十二指肠疾病:无, 慢性消化疾病:有")))

  (testing "generate-hematologic-summary"
    (is (= (cards/generate-hematologic-summary nil) "血液系统: 无数据"))
    (is (= (cards/generate-hematologic-summary {}) "血液系统: 无数据"))
    (is (= (cards/generate-hematologic-summary
            {:anemia {:has "无"}
             :coagulation_dysfunction {:has "无"}
             :thrombosis_history {:has "无"}})
           "血液系统: 贫血:无, 凝血功能障碍:无, 血栓史:无"))
    (is (= (cards/generate-hematologic-summary
            {:anemia {:has "有"}
             :coagulation_dysfunction {:has "有"}
             :thrombosis_history {:has "无"}})
           "血液系统: 贫血:有, 凝血功能障碍:有, 血栓史:无")))

  (testing "generate-immune-summary"
    (is (= (cards/generate-immune-summary nil) "免疫系统: 无数据"))
    (is (= (cards/generate-immune-summary {}) "免疫系统: 无数据"))
    (is (= (cards/generate-immune-summary
            {:immune_dysfunction {:has "无"}
             :autoimmune_disease {:has "无"}})
           "免疫系统: 免疫功能障碍:无, 自身免疫性疾病:无"))
    (is (= (cards/generate-immune-summary
            {:immune_dysfunction {:has "有"}
             :autoimmune_disease {:has "无"}})
           "免疫系统: 免疫功能障碍:有, 自身免疫性疾病:无")))

  (testing "generate-special-medication-history-summary"
    (is (= (cards/generate-special-medication-summary nil) "特殊用药史: 无数据"))
    (is (= (cards/generate-special-medication-summary {}) "特殊用药史: 无数据"))
    (is (= (cards/generate-special-medication-summary
            {:anticoagulant_antiplatelet {:present "无"}
             :glucocorticoids {:present "无"}
             :cancer_treatment {:present "无"}
             :drug_abuse_dependence {:present "无"}})
           "特殊用药史: 抗凝/抗血小板:无, 糖皮质激素:无, 肿瘤治疗:无, 药物滥用:无"))
    (is (= (cards/generate-special-medication-summary
            {:anticoagulant_antiplatelet {:present "有"}
             :glucocorticoids {:present "有"}
             :cancer_treatment {:present "无"}
             :drug_abuse_dependence {:present "无"}})
           "特殊用药史: 抗凝/抗血小板:有, 糖皮质激素:有, 肿瘤治疗:无, 药物滥用:无")))

  (testing "generate-special-disease-history-summary"
    (is (= (cards/generate-special-disease-history-summary nil) "特殊疾病病史: 无数据"))
    (is (= (cards/generate-special-disease-history-summary {}) "特殊疾病病史: 无数据"))
    (is (= (cards/generate-special-disease-history-summary
            {:marfan_syndrome {:present "无"}})
           "特殊疾病病史: 马方综合征:无"))
    (is (= (cards/generate-special-disease-history-summary
            {:marfan_syndrome {:present "有"} :other_special_diseases "Lupus"})
           "特殊疾病病史: 马方综合征:有, 其他特殊疾病:有"))
     (is (= (cards/generate-special-disease-history-summary
            {:marfan_syndrome {:present "无"} :other_special_diseases ""})
           "特殊疾病病史: 马方综合征:无")))

  (testing "generate-nutritional-assessment-summary"
    (is (= (cards/generate-nutritional-assessment-summary nil) "营养评估: 无数据"))
    (is (= (cards/generate-nutritional-assessment-summary {}) "营养评估: 无数据"))
    (is (= (cards/generate-nutritional-assessment-summary
            {:nutritional_score {:bmi_lt_20_5 "无" :weight_loss_last_3_months "无" :reduced_intake_last_week "无" :severe_illness "无"}
             :frail_score {:fatigue "无" :resistance "无" :ambulation "无" :illness_gt_5 "无" :loss_of_weight_gt_5_percent "无"}})
           "营养评估: 营养评分风险:无, FRAIL评估风险:无"))
    (is (= (cards/generate-nutritional-assessment-summary
            {:nutritional_score {:bmi_lt_20_5 "有"}
             :frail_score {:fatigue "无"}})
           "营养评估: 营养评分风险:有, FRAIL评估风险:无"))
    (is (= (cards/generate-nutritional-assessment-summary
            {:nutritional_score {:bmi_lt_20_5 "无"}
             :frail_score {:fatigue "有"}})
           "营养评估: 营养评分风险:无, FRAIL评估风险:有"))
    (is (= (cards/generate-nutritional-assessment-summary
            {:nutritional_score {:bmi_lt_20_5 "有"}
             :frail_score {:fatigue "有"}})
           "营养评估: 营养评分风险:有, FRAIL评估风险:有")))

  (testing "generate-pregnancy-assessment-summary"
    (is (= (cards/generate-pregnancy-assessment-summary nil) "妊娠: 无数据"))
    (is (= (cards/generate-pregnancy-assessment-summary {}) "妊娠: 无数据"))
    (is (= (cards/generate-pregnancy-assessment-summary {:is_pregnant "无"}) "妊娠:否"))
    (is (= (cards/generate-pregnancy-assessment-summary {:is_pregnant "不祥"}) "妊娠:不祥"))
    (is (= (cards/generate-pregnancy-assessment-summary {:is_pregnant "有"})
           "妊娠:是, 孕周:未提供, 合并情况:无"))
    (is (= (cards/generate-pregnancy-assessment-summary {:is_pregnant "有"
                                                       :gestational_week "13-28 周"
                                                       :comorbid_obstetric_conditions ["gestational_diabetes"]})
           "妊娠:是, 孕周:13-28 周, 合并情况:有")))

  (testing "generate-surgical-anesthesia-history-summary"
    (is (= (cards/generate-surgical-anesthesia-history-summary nil) "手术麻醉史: 无数据"))
    (is (= (cards/generate-surgical-anesthesia-summary {}) "手术麻醉史: 无数据"))
    (is (= (cards/generate-surgical-anesthesia-history-summary
            {:history {:present "无"}
             :family_history_malignant_hyperthermia {:present "无"}})
           "手术麻醉史: 手术麻醉史:无, 恶性高热家族史:无"))
    (is (= (cards/generate-surgical-anesthesia-history-summary
            {:history {:present "有"}
             :family_history_malignant_hyperthermia {:present "有"}})
           "手术麻醉史: 手术麻醉史:有, 恶性高热家族史:有"))
    (is (= (cards/generate-surgical-anesthesia-history-summary
            {:history {:present "不祥"}
             :family_history_malignant_hyperthermia {:present "无"}})
           "手术麻醉史: 手术麻醉史:不祥, 恶性高热家族史:无")))

  (testing "generate-airway-summary"
    (is (= (cards/generate-airway-summary nil) "气道评估: 无数据"))
    (is (= (cards/generate-airway-summary {}) "气道评估: 无数据"))
    (is (= (cards/generate-airway-summary {:detailed_assessment {:difficult_intubation_history "无"
                                                               :mallampati_classification "Ⅰ级"
                                                               :mouth_opening {:degree "gte_3_fingers"}
                                                               :thyromental_distance_class "gt_6_5_cm"}})
           "气道评估: 未见明显异常"))
    (is (= (cards/generate-airway-summary {:detailed_assessment {:mallampati_classification "Ⅲ级"}})
           "气道评估: Mallampati:Ⅲ级"))
    (is (= (cards/generate-airway-summary
            {:detailed_assessment {:difficult_intubation_history "有"
                                   :mallampati_classification "Ⅲ级"}})
           "气道评估: 困难插管史:有, Mallampati:Ⅲ级")))

  (testing "generate-spinal-anesthesia-assessment-summary"
    (is (= (cards/generate-spinal-anesthesia-assessment-summary nil) "椎管内麻醉相关评估: 无数据"))
    (is (= (cards/generate-spinal-anesthesia-assessment-summary {}) "椎管内麻醉风险: 无明确风险因素"))
    (is (= (cards/generate-spinal-anesthesia-assessment-summary
            {:central_nervous_system {:brain_tumor "无"}
             :peripheral_nervous_system {:spinal_cord_injury "无"}
             :lumbar_disc_herniation {:present "无"}
             :cardiovascular_system {:aortic_stenosis "无"}
             :puncture_site_inspection {:local_infection "无"}
             :local_anesthetic_allergy "无"})
           "椎管内麻醉风险: 无明确风险因素"))
    (is (= (cards/generate-spinal-anesthesia-assessment-summary
            {:central_nervous_system {:brain_tumor "有"}
             :local_anesthetic_allergy "有"})
           "椎管内麻醉风险: 脑肿瘤, 局麻药过敏"))
    (is (= (cards/generate-spinal-anesthesia-assessment-summary
            {:peripheral_nervous_system {:spinal_cord_injury "有"}
             :lumbar_disc_herniation {:present "有"}
             :cardiovascular_system {:aortic_stenosis "有"}})
           "椎管内麻醉风险: 脊髓损伤, 腰椎间盘突出, 主动脉瓣狭窄"))))
