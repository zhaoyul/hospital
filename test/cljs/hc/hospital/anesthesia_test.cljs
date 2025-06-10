(ns hc.hospital.anesthesia-test
  (:require
   [cljs.test :refer-macros [deftest is testing run-tests]]
   [re-frame.core :as rf]
   [hc.hospital.events :as events]
   [hc.hospital.subs :as subs]
   [hc.hospital.db :as app-db])) ; Ensure app-db is required for default-db access

;; Initialize re-frame app state for tests if not already handled globally
;; This might be needed if tests run in an environment where ::initialize-db isn't automatically called.
;; However, typically, you dispatch events on a known db state.

(deftest signature-event-and-sub-test
  (testing "Test ::update-signature-data event and ::doctor-signature-image subscription"
    ;; Dispatch an event to ensure the db is initialized, including the path for signature.
    ;; Using a minimal db structure for the test.
    (let [initial-db (assoc-in app-db/default-db [:anesthesia :current-assessment-canonical :基本信息] {})]
      (rf/dispatch-sync [::events/initialize-db]) ;; Ensure a base state

      (testing "Update signature data"
        (rf/dispatch-sync [::events/update-signature-data "test-base64-string"])
        (let [signature-from-db (get-in @rf/app-db [:anesthesia :current-assessment-canonical :基本信息 :医生签名图片])
              signature-from-sub @(rf/subscribe [::subs/doctor-signature-image])]
          (is (= "test-base64-string" signature-from-db) "Signature data should be updated in app-db")
          (is (= "test-base64-string" signature-from-sub) "Subscription should return the updated signature data")))

      (testing "Clear signature data"
        (rf/dispatch-sync [::events/update-signature-data nil])
        (let [signature-from-db (get-in @rf/app-db [:anesthesia :current-assessment-canonical :基本信息 :医生签名图片])
              signature-from-sub @(rf/subscribe [::subs/doctor-signature-image])]
          (is (nil? signature-from-db) "Signature data should be cleared in app-db")
          (is (nil? signature-from-sub) "Subscription should return nil after clearing signature data")))

      (testing "Subscription reflects manually set app-db state"
        ;; Directly manipulate app-db for this part of the test, bypassing the event
        (reset! rf/app-db (assoc-in @rf/app-db [:anesthesia :current-assessment-canonical :基本信息 :医生签名图片] "manual-test-string"))
        (let [signature-from-sub @(rf/subscribe [::subs/doctor-signature-image])]
          (is (= "manual-test-string" signature-from-sub) "Subscription should reflect manually set app-db state"))))))

;; To run tests (usually via a test runner like Kaocha or cljs.test.run):
;; (run-tests 'hc.hospital.anesthesia-test)
;; Or, if you have a main test runner namespace:
;; (run-tests)
;; Make sure this namespace is included in your test build.
