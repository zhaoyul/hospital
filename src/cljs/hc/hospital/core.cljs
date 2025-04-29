(ns hc.hospital.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      ["antd" :as antd :refer [Button]])) ; Import Button component from antd

;; Import Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; Views

(defn home-page []
  [:div
   [:h2 "Welcome to Reagent!"]
   [:> Button {:type "primary"} "Hello Ant Design!"]]) ; Use the Button component

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  (mount-root))
