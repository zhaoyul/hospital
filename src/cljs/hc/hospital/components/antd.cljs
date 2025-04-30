(ns hc.hospital.components.antd
  (:require [reagent.core :as r]
            ["antd" :refer [Layout Menu DatePicker Input InputGroup Button
                            Switch
                            Tabs Row Col Card Form Radio Checkbox InputNumber Typography Tag]]
            ["@ant-design/icons" :as icons]))

;; Layout Components
(def layout (r/adapt-react-class Layout))
(def content (r/adapt-react-class (.-Content Layout)))
(def sider (r/adapt-react-class (.-Sider Layout)))
(def header (r/adapt-react-class (.-Header Layout)))
(def footer (r/adapt-react-class (.-Footer Layout)))

;; Navigation
(def menu (r/adapt-react-class Menu))

;; Data Entry Components
(def date-picker (r/adapt-react-class DatePicker))
(def range-picker (r/adapt-react-class (.-RangePicker DatePicker)))
(def input (r/adapt-react-class Input))
(def input-group (r/adapt-react-class (.-Group Input)))
(def input-search (r/adapt-react-class (.-Search Input)))
(def button (r/adapt-react-class Button))
(def tabs (r/adapt-react-class Tabs))
(def form (r/adapt-react-class Form))
(def form-item (r/adapt-react-class (.-Item Form)))
(def radio (r/adapt-react-class Radio))
(def radio-group (r/adapt-react-class (.-Group Radio)))
(def checkbox (r/adapt-react-class Checkbox))
(def checkbox-group (r/adapt-react-class (.-Group Checkbox)))
(def input-number (r/adapt-react-class InputNumber))

;; Data Display Components
(def card (r/adapt-react-class Card))
(def tag (r/adapt-react-class Tag))

(def switch (r/adapt-react-class Switch))

;; Typography
(def typography (r/adapt-react-class Typography))
(def title (r/adapt-react-class (.-Title Typography)))
(def text (r/adapt-react-class (.-Text Typography)))

;; Grid Components
(def row (r/adapt-react-class Row))
(def col (r/adapt-react-class Col))

;; Icons (Example - add more as needed)
(def user-outlined (r/adapt-react-class icons/UserOutlined))
(def laptop-outlined (r/adapt-react-class icons/LaptopOutlined))
(def notification-outlined (r/adapt-react-class icons/NotificationOutlined))
(def filter-outlined (r/adapt-react-class icons/FilterOutlined))
