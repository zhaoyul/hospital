(ns hc.hospital.db.daily-stats-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [hc.hospital.test-utils :refer [system-fixture *sys*]]
            [hc.hospital.db.daily-stats :as stats]))

(use-fixtures :once (system-fixture))

(defn query-fn []
  (let [sys @*sys*]
    (or (get sys :db.sql/query-fn) (get-in sys [:db.sql/query-fn :query-fn]))))

(deftest daily-stats-basic-test
  (let [qf (query-fn)
        date "2099-01-01"]
    (stats/upsert-daily-stats! qf date {:总就诊人数 1
                                        :今日患者人数 1})
    (stats/increment-daily-stats! qf date {:已签字人数 1
                                           :评估人数 1})
    (let [{:keys [data]} (stats/get-stats-by-date qf date)]
      (is (= 1 (:今日患者人数 data)))
      (is (= 1 (:已签字人数 data)))
      (is (= 1 (:评估人数 data))))

    ;; 测试单字段自增
    (stats/increment-stat-key! qf date :已签字人数)
    (let [{:keys [data]} (stats/get-stats-by-date qf date)]
      (is (= 2 (:已签字人数 data))))

    ;; 跨年初始化测试
    (let [last-day "2099-12-31"
          next-day "2100-01-01"]
      (stats/upsert-daily-stats! qf last-day {:年累计就诊人数 10
                                              :月累计就诊人数 3
                                              :周累计就诊人数 2
                                              :今日患者人数 5})
      (stats/inherit-or-init-stats! qf next-day)
      (let [{:keys [data]} (stats/get-stats-by-date qf next-day)]
        (is (= 0 (:年累计就诊人数 data)))
        (is (= 0 (:月累计就诊人数 data)))
        ;; 跨年但未必跨周，周统计应继承
        (is (= 2 (:周累计就诊人数 data)))
        (is (= 0 (:今日患者人数 data)))))

    ;; 跨周初始化测试
    (let [last-day "2100-01-10" ; 周跨越示例
          next-day "2100-01-11"]
      (stats/upsert-daily-stats! qf last-day {:周累计就诊人数 5})
      (stats/inherit-or-init-stats! qf next-day)
      (let [{:keys [data]} (stats/get-stats-by-date qf next-day)]
        (is (= 0 (:周累计就诊人数 data)))))

    ;; 跨月初始化测试
    (let [last-day "2100-05-31"
          next-day "2100-06-01"]
      (stats/upsert-daily-stats! qf last-day {:月累计就诊人数 7
                                              :周累计就诊人数 3})
      (stats/inherit-or-init-stats! qf next-day)
      (let [{:keys [data]} (stats/get-stats-by-date qf next-day)]
        (is (= 0 (:月累计就诊人数 data)))
        (is (= 3 (:周累计就诊人数 data)))))))
