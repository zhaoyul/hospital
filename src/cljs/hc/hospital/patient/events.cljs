(ns hc.hospital.patient.events
  (:require [re-frame.core :as rf]
            [hc.hospital.patient.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(def patient-form-total-steps 3)
(def patient-form-max-step-idx (dec patient-form-total-steps))

(defn- validate-basic-info [basic-info]
  (let [errors (atom {})
        phone (:手机号 basic-info)
        phone-regex #"^1[0-9]{10}$"]
    (when (str/blank? (:门诊号 basic-info))
      (swap! errors assoc-in [:基本信息 :门诊号] "门诊号不能为空"))
    (when (str/blank? (:姓名 basic-info))
      (swap! errors assoc-in [:基本信息 :姓名] "姓名不能为空"))
    (when (str/blank? (:身份证号 basic-info))
      (swap! errors assoc-in [:基本信息 :身份证号] "身份证号不能为空"))
    (cond
      (str/blank? phone) (swap! errors assoc-in [:基本信息 :手机号] "手机号不能为空")
      (not (re-matches phone-regex phone)) (swap! errors assoc-in [:基本信息 :手机号] "请输入正确的手机号（1开头的11位数字）"))
    (when (nil? (:性别 basic-info))
      (swap! errors assoc-in [:基本信息 :性别] "性别不能为空"))
    (when (nil? (:年龄 basic-info))
      (swap! errors assoc-in [:基本信息 :年龄] "年龄不能为空"))
    (when (str/blank? (:院区 basic-info))
      (swap! errors assoc-in [:基本信息 :院区] "院区不能为空"))
    @errors))

(rf/reg-event-db ::initialize-db (fn [_ _] db/default-db))

(rf/reg-event-db ::update-form-field (fn [db [_ path value]] (assoc-in db (into [:patient-form] path) value)))

(rf/reg-event-fx
 ::next-step
 (fn [{:keys [db]} _]
   (let [current-step (get-in db [:patient-form :current-step])]
     (if (= current-step 0)
       (let [basic-info (get-in db [:patient-form :基本信息])
             validation-errors (validate-basic-info basic-info)]
         (if (empty? validation-errors)
           {:db (-> db
                    (update-in [:patient-form :current-step] inc)
                    (assoc-in [:patient-form :form-errors] {}))}
           {:db (assoc-in db [:patient-form :form-errors] validation-errors)}))
       (if (< current-step patient-form-max-step-idx)
         {:db (-> db
                  (update-in [:patient-form :current-step] inc)
                  (assoc-in [:patient-form :form-errors] {}))}
         {:db db})))))

(rf/reg-event-db ::prev-step (fn [db _] (update-in db [:patient-form :current-step] #(max 0 (dec %)))))

(rf/reg-event-db ::goto-step (fn [db [_ step]] (if (and (>= step 0) (< step patient-form-total-steps)) (assoc-in db [:patient-form :current-step] step) db)))

(rf/reg-event-fx
 ::validate-and-submit
 (fn [{:keys [db]} _]
   (let [form-data (get-in db [:patient-form])
         basic-info (:基本信息 form-data)
         errors (transient {})]
     (when (str/blank? (:姓名 basic-info))
       (assoc! errors [:基本信息 :姓名] "姓名不能为空"))
     (when (str/blank? (:门诊号 basic-info))
       (assoc! errors [:基本信息 :门诊号] "门诊号不能为空"))
     (when (nil? (:年龄 basic-info))
       (assoc! errors [:基本信息 :年龄] "年龄不能为空"))
     (when (nil? (:性别 basic-info))
       (assoc! errors [:基本信息 :性别] "性别不能为空"))
     (let [final-errors (persistent! errors)]
       (if (empty? final-errors)
         {:db (-> db
                  (assoc-in [:patient-form :submitting?] true)
                  (assoc-in [:patient-form :form-errors] {}))
          :dispatch [::submit-form]}
         {:db (assoc-in db [:patient-form :form-errors] final-errors)})))))

(rf/reg-event-fx
  ::submit-form
  (fn [{:keys [db]} _]
    (let [raw-form (get-in db [:patient-form])
          cleaned (dissoc raw-form :form-errors :submitting? :submit-success? :submit-error :current-step)]
      {:http-xhrio {:method :post
                    :uri "/api/patient/assessment"
                    :params cleaned
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [::submit-success]
                    :on-failure [::submit-failure]}})))

(rf/reg-event-db ::submit-success (fn [db [_ resp]] (timbre/debug "表单提交成功:" resp) (-> db (assoc-in [:patient-form :submitting?] false) (assoc-in [:patient-form :submit-success?] true))))

(rf/reg-event-db ::submit-failure (fn [db [_ resp]] (timbre/error "表单提交失败:" resp) (-> db (assoc-in [:patient-form :submitting?] false) (assoc-in [:patient-form :submit-success?] false) (assoc-in [:patient-form :submit-error] (or (:message (:response resp)) "提交失败，请稍后再试")))))
