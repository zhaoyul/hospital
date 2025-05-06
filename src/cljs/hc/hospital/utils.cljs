(ns hc.hospital.utils
  (:require ["dayjs" :as dayjs]))

(defn event-value [event]
  (.. event -target -value))

;; 将字符串转换为JS日期对象
(defn parse-date
  "将日期字符串转换为dayjs对象，如果无效则返回nil"
  [date-str]
  (when (and date-str (not= date-str ""))
    (let [date (dayjs date-str)]
      (when (.isValid date)
        date))))

;; 将JS日期对象格式化为字符串
(defn format-date
  "将dayjs对象或日期字符串格式化为指定格式"
  [date format]
  (when date
    (let [dayjs-obj (if (string? date) (parse-date date) date)]
      (when dayjs-obj
        (.format dayjs-obj format)))))

;; 创建当前日期的dayjs对象
(defn now []
  (dayjs))
