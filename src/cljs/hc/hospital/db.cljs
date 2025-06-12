(ns hc.hospital.db)

(def default-db
  { ;; Root map for the entire default database state
   :anesthesia {}
   ;; Root level keys for doctor management etc.
   :doctors []
   :doctor-modal-visible? false
   :editing-doctor nil
   :current-doctor nil
   :is-logged-in false
   :login-error nil
   :session-check-pending? true
   })
