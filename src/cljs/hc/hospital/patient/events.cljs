(ns hc.hospital.patient.events
  (:require [re-frame.core :as rf]
            [hc.hospital.patient.db :as db]
            [hc.hospital.specs.patient-questionnaire-spec :as pq-spec]
            [malli.core :as m]
            [malli.error :as me]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [taoensso.timbre :as timbre]))

(def patient-form-total-steps 3)
(def patient-form-max-step-idx (dec patient-form-total-steps))

(def cn-errors
  (-> me/default-errors
      (assoc ::m/missing-key {:error/fn {:zh (fn [{:keys [in]} _]
                                           (str (last in) "不能为空"))}})))

(defn- humanize-zh [ex]
  (when ex
    (me/humanize ex {:locale :zh :errors cn-errors})))

(defn- prefix-errors
  "给 Malli 验证返回的错误信息加上 [:基本信息] 前缀。"
  [errors]
  (reduce-kv (fn [m k v]
               (let [path (if (vector? k) k [k])]
                 (assoc-in m (into [:基本信息] path) v)))
             {}
             errors))

(defn- validate-basic-info [basic-info]
  (if-let [ex (m/explain pq-spec/PatientBasicInfoSpec basic-info)]
    (prefix-errors (humanize-zh ex))
    {}))

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
         errors (humanize-zh (m/explain pq-spec/PatientQuestionnaireSpec form-data))]
     (if (empty? errors)
       {:db (-> db
                (assoc-in [:patient-form :submitting?] true)
                (assoc-in [:patient-form :form-errors] {}))
        :dispatch [::submit-form]}
       {:db (assoc-in db [:patient-form :form-errors] errors)}))))

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
