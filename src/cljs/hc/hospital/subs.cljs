(ns hc.hospital.subs
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [hc.hospital.utils :as utils]
            [dayjs :as dayjs])) ; 确保引入 dayjs

(rf/reg-sub
 ::all-patient-assessments
 (fn [db _]
   (:all-patient-assessments db)))

(rf/reg-sub
 ::current-patient-id
 (fn [db _]
   (get-in db [:anesthesia :current-patient-id]))) ; 从 :anesthesia 模块获取

(rf/reg-sub
 ::active-tab
 (fn [db _]
   (get-in db [:anesthesia :active-tab] "patients")))

(rf/reg-sub
 ::search-term
 (fn [db _]
   (get-in db [:anesthesia :search-term])))

(rf/reg-sub
 ::date-range
 (fn [db _]
   (get-in db [:anesthesia :date-range])))

(rf/reg-sub
 ::assessment-status-filter
 (fn [db _]
   (get-in db [:anesthesia :assessment-status-filter] "all")))

(rf/reg-sub
 ::anesthesia-example-patients
 (fn [db _]
   (get-in db [:anesthesia :patients])))

(rf/reg-sub
 ::filtered-patients
 :<- [::all-patient-assessments]
 :<- [::search-term]
 :<- [::date-range]
 :<- [::assessment-status-filter]
 :<- [::anesthesia-example-patients]
 (fn [[api-assessments search-term date-range-moments status-filter example-patients] _]
   (letfn [(format-gender [g] (case g "male" "男" "female" "女" "未知"))
           (format-date-str [d] (when d (utils/format-date d "YYYY-MM-DD")))
           (patient-from-api-assessment [assessment]
             (let [assessment-data (:assessment_data assessment {})
                   basic-info (:basic-info assessment-data {})
                   anesthesia-plan (:anesthesia-plan assessment-data {})]
               {:key (:patient_id assessment)
                :name (:name basic-info "未知姓名")
                :patient-id-display (or (:outpatient-number basic-info) (:patient_id assessment))
                :sex (format-gender (:gender basic-info))
                :age (str (:age basic-info "未知") "岁")
                :anesthesia-type (:anesthesia-type anesthesia-plan "未知麻醉方式")
                :date (format-date-str (:updated_at assessment)) ; 或 :created_at
                :status (or (:doctor_status assessment) "待评估")})) ; 假设API有 doctor_status
           (patient-from-example [ex-patient] ; 确保示例数据字段与映射后的一致
             (merge {:sex (format-gender (:gender ex-patient))
                     :age (str (:age ex-patient) "岁")}
                    ex-patient))]

     (let [patients-from-api (if (seq api-assessments)
                               (mapv patient-from-api-assessment api-assessments)
                               [])
           display-patients (if (empty? patients-from-api)
                              (mapv patient-from-example example-patients)
                              patients-from-api)

           [start-moment end-moment] date-range-moments
           start-date-str (when start-moment (.format start-moment "YYYY-MM-DD"))
           end-date-str (when end-moment (.format end-moment "YYYY-MM-DD"))]

       (cond->> display-patients
         (and search-term (not (str/blank? search-term)))
         (filterv (fn [p]
                    (let [search-lower (str/lower-case search-term)]
                      (or (str/includes? (str/lower-case (str (:name p))) search-lower)
                          (str/includes? (str/lower-case (str (:patient-id-display p))) search-lower)))))

         (and start-date-str end-date-str)
         (filterv (fn [p] (when-let [p-date (:date p)]
                            (and (>= p-date start-date-str)
                                 (<= p-date end-date-str)))))

         (and status-filter (not= status-filter "all"))
         (filterv (fn [p] (= (:status p) status-filter)))

         true identity)))))


;; ---- 评估表单相关订阅 ----
(rf/reg-sub
 ::selected-patient-assessment-forms-data
 (fn [db _]
   (get-in db [:anesthesia :assessment])))

(rf/reg-sub
 ::doctor-form-brief-medical-history
 :<- [::selected-patient-assessment-forms-data]
 (fn [assessment-data _]
   (:brief-medical-history assessment-data)))

(rf/reg-sub
 ::doctor-form-physical-examination
 :<- [::selected-patient-assessment-forms-data]
 (fn [assessment-data _]
   (:physical-examination assessment-data)))

(rf/reg-sub
 ::doctor-form-lab-tests
 :<- [::selected-patient-assessment-forms-data]
 (fn [assessment-data _]
   (:lab-tests assessment-data)))
