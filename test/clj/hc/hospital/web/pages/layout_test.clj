(ns hc.hospital.web.pages.layout-test
  (:require [clojure.test :refer :all]
            [hc.hospital.web.pages.layout :as layout]))

(deftest render-produces-html-response
  (let [resp (layout/render {} "home.html")]
    (is (= 200 (:status resp)))
    (is (= "text/html; charset=utf-8" (get-in resp [:headers "Content-Type"])))
    (is (.contains (:body resp) "<!DOCTYPE html>"))))

(deftest error-page-produces-html
  (let [resp (layout/error-page {:status 404 :title "Not Found" :message "oops"})]
    (is (= 404 (:status resp)))
    (is (.contains (:body resp) "Not Found"))))
