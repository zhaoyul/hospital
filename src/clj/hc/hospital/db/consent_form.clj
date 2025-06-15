(ns hc.hospital.db.consent-form)


(defn save-consent-form!
  [query-fn form]
  (query-fn :upsert-consent-form! form))

(defn get-consent-form
  [query-fn assessment-id]
  (query-fn :get-consent-form-by-assessment {:assessment_id assessment-id}))
