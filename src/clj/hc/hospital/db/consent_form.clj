(ns hc.hospital.db.consent-form)


(defn save-consent-form!
  [query-fn form]
  (query-fn :upsert-consent-form! form))

(defn get-consent-form
  [query-fn assessment-id]
  (query-fn :get-consent-form-by-assessment {:assessment_id assessment-id}))

(defn update-sedation-consent!
  [query-fn assessment-id sedation-html]
  (query-fn :update-sedation-consent!
            {:assessment_id assessment-id
             :sedation_form sedation-html}))

(defn update-pre-anesthesia-consent!
  [query-fn assessment-id form-html]
  (query-fn :update-pre-anesthesia-consent!
            {:assessment_id assessment-id
             :pre_anesthesia_form form-html}))

(defn update-anesthesia-consent!
  [query-fn assessment-id form-html]
  (query-fn :update-anesthesia-consent!
            {:assessment_id assessment-id
             :anesthesia_form form-html}))
