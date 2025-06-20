(ns hc.hospital.web.controllers.overview-api
  (:require [hc.hospital.db.daily-stats :as stats]
            [ring.util.http-response :as http-response]
            [clojure.string :as str])
  (:import [java.time LocalDate]))

(defn- today-date []
  (.toString (LocalDate/now)))

(defn get-stats
  [{:keys [query-fn] {{:keys [date]} :query} :parameters}]
  (try
    (let [d   (if (str/blank? date) (today-date) date)
          stat (stats/get-stats-by-date query-fn d)]
      (http-response/ok {:stats stat}))
    (catch Exception _
      (http-response/internal-server-error {:message "查询统计信息失败"}))))

(defn increment-stats!
  [{:keys [query-fn] {:keys [date] :as body} :body-params}]
  (let [d (if (str/blank? date) (today-date) date)
        increments (into {}
                         (for [[k v] (dissoc body :date)]
                           [(keyword (str/replace (name k) #"_inc$" "")) v]))]
    (stats/increment-daily-stats! query-fn d increments)
    (http-response/ok {:message "统计已更新"})))
