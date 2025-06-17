(ns hc.hospital.web.controllers.report
  (:require
   [hc.hospital.web.pages.layout :as layout]))

(defn sedation-consent-page [request]
  (layout/render request "report/sedation_consent.html"))

(defn pre-anesthesia-consent-page [request]
  (layout/render request "report/pre_anesthesia_consent.html"))

(defn anesthesia-consent-page [request]
  (layout/render request "report/anesthesia_consent.html"))
