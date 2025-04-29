(ns hc.hospital.subs
  (:require
   [re-frame.core :as rf]))

;; 获取患者列表
(rf/reg-sub
 ::patients
 (fn [db]
   (get-in db [:anesthesia :patients])))

;; 获取当前选中的患者ID
(rf/reg-sub
 ::current-patient-id
 (fn [db]
   (get-in db [:anesthesia :current-patient-id])))

;; 获取当前选中的患者详细信息
(rf/reg-sub
 ::current-patient
 :<- [::patients]
 :<- [::current-patient-id]
 (fn [[patients current-id] _]
   (when current-id
     (first (filter #(= (:key %) current-id) patients)))))

;; 获取当前的标签页
(rf/reg-sub
 ::active-tab
 (fn [db]
   (get-in db [:anesthesia :active-tab])))

;; 获取评估标签页
(rf/reg-sub
 ::active-assessment-tab
 (fn [db]
   (get-in db [:anesthesia :ui :active-assessment-tab])))

;; 获取搜索词
(rf/reg-sub
 ::search-term
 (fn [db]
   (get-in db [:anesthesia :search-term])))

;; 获取日期范围
(rf/reg-sub
 ::date-range
 (fn [db]
   (get-in db [:anesthesia :date-range])))

;; 过滤后的患者列表
(rf/reg-sub
 ::filtered-patients
 :<- [::patients]
 :<- [::search-term]
 :<- [::date-range]
 (fn [[patients search-term date-range] _]
   (let [filtered-by-search
         (if (empty? search-term)
           patients
           (filter #(or (clojure.string/includes? (clojure.string/lower-case (:name %)) 
                                                 (clojure.string/lower-case search-term))
                        (clojure.string/includes? (:key %) search-term)
                        (clojure.string/includes? (:type %) search-term)
                        (clojure.string/includes? (:status %) search-term))
                   patients))
         filtered-by-date
         (if (nil? date-range)
           filtered-by-search
           (filter #(let [date (:date %)]
                      ;; 这里需要对日期进行实际的比较，简化表示
                      true) filtered-by-search))]
     filtered-by-date)))

;; 获取简要病史数据
(rf/reg-sub
 ::brief-medical-history
 (fn [db]
   (get-in db [:anesthesia :assessment :brief-medical-history])))

;; 获取特定的病史项
(rf/reg-sub
 ::medical-history-item
 (fn [db [_ item-key]]
   (get-in db [:anesthesia :assessment :brief-medical-history item-key])))

;; 获取体格检查数据
(rf/reg-sub
 ::physical-examination
 (fn [db]
   (get-in db [:anesthesia :assessment :physical-examination])))

;; 获取实验室检查数据
(rf/reg-sub
 ::lab-tests
 (fn [db]
   (get-in db [:anesthesia :assessment :lab-tests])))