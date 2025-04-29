(ns hc.hospital.core
  (:require
   [reagent.core :as r]
   [hc.hospital.pages.anesthesia-home :refer [anesthesia-home-page]]
   [reagent.dom :as d]
   ["antd" :as antd :refer [Button]])) ; Import Button component from antd

;; Import Ant Design CSS
;;(js/require "antd/dist/reset.css")

;; -------------------------
;; Views

(defn home-page []
  [anesthesia-home-page]
  #_[:div
     [:h2 "Welcome to Reagent!"]
     [:> Button {:type "primary"} "Hello Ant Design!"]]) ; Use the Button component

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  (mount-root))
