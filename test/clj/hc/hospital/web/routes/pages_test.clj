(ns hc.hospital.web.routes.pages-test
  (:require [clojure.test :refer :all]
            [hc.hospital.web.routes.pages :as pages]
            [hc.hospital.web.middleware.exception :as exception]))

(deftest page-routes-structure
  (let [routes (pages/page-routes {})]
    (is (= 3 (count routes)))
    (is (= "/" (first (first routes))))
    (is (= "/login" (first (second routes))))
    (is (= "/logout" (first (nth routes 2))))))

(deftest route-data-contains-middlewares
  (is (vector? (:middleware pages/route-data)))
  (is (fn? (first (:middleware pages/route-data))))
  (is (some #{exception/wrap-exception} (:middleware pages/route-data))))
