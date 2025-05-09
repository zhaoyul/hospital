(ns hc.hospital.patient.db)

;; 初始数据库状态
(def default-db
  {:patient-form {:basic-info {:outpatient-number ""    ;; 门诊号
                               :name ""                 ;; 姓名
                               :id-number ""            ;; 身份证号
                               :phone ""                ;; 手机号
                               :gender nil              ;; 性别 (nil, "male", "female")
                               :age nil                 ;; 年龄
                               :hospital-district ""}    ;; 院区 ("main", "jst", or "")

                  :medical-summary {:allergy-history false
                                    :allergen ""
                                    :allergy-date nil
                                    :smoking-history false
                                    :smoking-years nil
                                    :cigarettes-per-day nil
                                    :drinking-history false
                                    :drinking-years nil
                                    :alcohol-per-day ""}

                  :comorbidities {:respiratory-disease {:has false :details ""}
                                  :neuromuscular-disease {:has false :details ""}
                                  :cardiovascular-disease {:has false :details ""}
                                  :liver-disease {:has false :details ""}
                                  :endocrine-disease {:has false :details ""}
                                  :kidney-disease {:has false :details ""}
                                  :neuropsychiatric-disease {:has false :details ""}
                                  :skeletal-system {:has false :details ""}
                                  :past-anesthesia-surgery {:has false :details ""}
                                  :family-malignant-hyperthermia {:has false :details ""}
                                  :special-medications {:used false
                                                        :details ""
                                                        :last-time nil}}

                  :physical-examination {:heart "normal"
                                         :heart-detail ""
                                         :lungs "normal"
                                         :lungs-detail ""
                                         :airway "normal"
                                         :airway-detail ""
                                         :teeth "normal"
                                         :teeth-detail ""
                                         :spine-limbs "normal"
                                         :spine-limbs-detail ""
                                         :nervous "normal"
                                         :nervous-detail ""
                                         :other ""}

                  :auxiliary-examination {:general-aux-report nil ;; 或 "-"
                                          :chest-radiography nil  ;; 或 "-"
                                          :pulmonary-function nil ;; 或 "-"
                                          :cardiac-ultrasound nil ;; 或 "-"
                                          :ecg nil                ;; 或 "-"
                                          :other nil}             ;; 或 "-"

                  :current-step 0
                  :form-errors {}
                  :submitting? false
                  :submit-success? false
                  :submit-error nil}})
