(ns hc.hospital.db)

(def default-db
  { ;; Root map for the entire default database state
   :anesthesia {}
   ;; 用户管理相关状态
   :users []
   :roles []
   :user-modal-visible? false
   :editing-user nil
   :role-modal-visible? false
   :editing-role nil
   :current-doctor nil
   :is-logged-in false
   :login-error nil
   :session-check-pending? true
   })
