
(ns hc.hospital.db
  (:require [hc.hospital.utils :as utils]))

(def default-overview-stats
  "首页概览默认统计数据"
  [{:label "总就诊人数" :value 156 :trend :up :description "比去年增加12人"}
   {:label "今日患者人数" :value 42 :trend :up :description "比昨日增加3人"}
   {:label "手术患者人数" :value 18 :trend :down :description "比昨日减少2人"}
   {:label "已签字人数" :value 15 :trend :up :description "比昨日增加5人"}
   {:label "糖尿病人数" :value 7 :trend :same :description "与昨日持平"}
   {:label "高血压人数" :value 9 :trend :up :description "比昨日增加2人"}
   {:label "过敏史人数" :value 3 :trend :down :description "比昨日减少1人"}])

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
   :overview-date (utils/date->iso-string (utils/now))
   :overview-stats default-overview-stats})
