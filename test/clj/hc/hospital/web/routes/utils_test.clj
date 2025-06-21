(ns hc.hospital.web.routes.utils-test
  (:require [clojure.test :refer :all]
            [hc.hospital.web.routes.utils :as utils]))

(deftest route-data-utils
  (let [req {:reitit.core/match {:data {:role :doctor :page "index"}}}]
    (is (= {:role :doctor :page "index"} (utils/route-data req)))
    (is (= :doctor (utils/route-data-key req :role)))
    (is (nil? (utils/route-data-key req :missing)))))
