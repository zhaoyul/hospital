(ns hc.hospital.utils
  "工具函数集，处理日期格式化等通用逻辑。"
  (:require ["dayjs" :as dayjs]))

(defn event-value
  "从表单事件中提取输入值。"
  [event]
  (.. event -target -value))

(defn parse-date
  "将日期字符串转换为dayjs对象，如果无效则返回nil"
  [date-str]
  (when (and date-str (not= date-str ""))
    (let [date (dayjs date-str)]
      (when (.isValid date)
        date))))

(defn format-date
  "将dayjs对象或日期字符串格式化为指定格式"
  [date format]
  (when date
    (let [dayjs-obj (if (string? date) (parse-date date) date)]
      (when dayjs-obj
        (.format dayjs-obj format)))))

(defn now []
  (dayjs))

(defn to-dayjs
  "将日期字符串或普通JS日期对象转换为dayjs对象（用于日期选择器等组件）"
  [date]
  (when date
    (cond
      (string? date) (parse-date date)
      :else (dayjs date))))

(defn date->iso-string
  "将dayjs对象转换为ISO格式字符串，适用于API交互"
  [date]
  (when date
    (if (string? date)
      date
      (if (instance? js/Object date) ; Check if it's a dayjs object
        (.format date "YYYY-MM-DD")
        (.format (dayjs date) "YYYY-MM-DD"))))) ; Fallback for other types, though dayjs objects should be passed

(defn datetime->string
  "Formats a dayjs object to a string using the provided format.
   Returns nil if the input is not a valid dayjs object."
  [d format-string]
  (when (and d (instance? js/Object d) (.isValid (dayjs d))) ; Ensure d is a dayjs object and valid
    (.format (dayjs d) format-string)))

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
          ;; 其他情况：若为字符串则原样返回，否则返回 nil
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
