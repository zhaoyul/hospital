(ns hc.hospital.web.middleware.exception-test
  (:require [clojure.test :refer :all]
            [hc.hospital.web.middleware.exception :as exc]))

(defn apply-mw [mw handler]
  ((:wrap mw) handler))

(deftest wrap-exception-handles-known-types
  (let [handler (fn [_] (throw (ex-info "boom" {:type :system.exception/not-found})))
        wrapped (apply-mw exc/wrap-exception handler)
        resp (wrapped {:uri "/missing"})]
    (is (= 404 (:status resp)))
    (is (= "not found" (get-in resp [:body :message])))))

(deftest wrap-exception-handles-default
  (let [handler (fn [_] (throw (ex-info "boom" {})))
        wrapped (apply-mw exc/wrap-exception handler)
        resp (wrapped {:uri "/"})]
    (is (= 500 (:status resp)))
    (is (= "default" (get-in resp [:body :message])))))
