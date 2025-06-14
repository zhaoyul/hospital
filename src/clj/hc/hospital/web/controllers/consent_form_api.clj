(ns hc.hospital.web.controllers.consent-form-api
  (:require [ring.util.http-response :as http-response]
            [taoensso.timbre :as log]
            [hc.hospital.db.consent-form :as db]))

(defn save-consent-form!
  [{:keys [query-fn body-params]}]
  (log/info "保存知情同意书:" body-params)
  (try
    (db/save-consent-form! query-fn body-params)
    (http-response/ok {:message "保存成功"})
    (catch Exception e
      (log/error e "保存知情同意书失败")
      (http-response/internal-server-error {:message "保存失败"}))))

(defn get-consent-form
  [{:keys [query-fn parameters]}]
  (let [assessment-id (get-in parameters [:path :assessment-id])]
    (log/info "查询知情同意书, assessment-id:" assessment-id)
    (try
      (if-let [form (db/get-consent-form query-fn (Integer/parseInt assessment-id))]
        (http-response/ok form)
        (http-response/not-found {:message "未找到知情同意书"}))
      (catch Exception e
        (log/error e "查询知情同意书失败")
        (http-response/internal-server-error {:message "查询失败"})))))
