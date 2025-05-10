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

;; 将日期转换为dayjs对象（与moment兼容的接口）
(defn to-moment
  "将日期字符串或普通JS日期对象转换为dayjs对象（用于日期选择器等组件）"
  [date]
  (when date
    (cond
      (string? date) (parse-date date)
      :else (dayjs date))))

;; 将日期对象转换为ISO格式字符串
(defn date->iso-string
  "将dayjs对象转换为ISO格式字符串，适用于API交互"
  [date]
  (when date
    (if (string? date)
      date
      (.format (dayjs date) "YYYY-MM-DD"))))

;; 将数字转换为罗马数字
(defn to-roman
  "将数字转换为罗马数字表示，支持1-10"
  [num]
  (let [romans {1 "I", 2 "II", 3 "III", 4 "IV", 5 "V", 
               6 "VI", 7 "VII", 8 "VIII", 9 "IX", 10 "X"}]
    (get romans num (str num))))

(defn format-datetime-for-input
  "Formats a date value for HTML date or datetime-local input fields."
  [value input-type]
  (when value
    (let [date-obj (if (string? value) (parse-date value) (dayjs value))]
      (when (.isValid date-obj)
        (condp = input-type
          "date" (.format date-obj "YYYY-MM-DD")
          "datetime-local" (.format date-obj "YYYY-MM-DDTHH:mm")
          ;; else, return original value if it's already a string, or nil
          (if (string? value) value nil))))))

(defn display-value 
  "将值格式化为可读字符串，处理nil或空字符串的情况"
  [v]
  (cond
    (nil? v) "-"
    (and (string? v) (empty? v)) "-"
    :else (str v)))

(defn display-list 
  "将列表格式化为可读字符串，处理nil或空列表的情况"
  [v]
  (cond
    (nil? v) "-"
    (and (coll? v) (empty? v)) "-"
    (coll? v) (->> v
                (filter #(and % (not= % "")))
                (map #(str "• " %))
                (clojure.string/join "\n"))
    :else (display-value v)))
