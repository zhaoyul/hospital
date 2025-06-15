(ns hc.hospital.web.controllers.report
  (:require
   [hc.hospital.web.pages.layout :as layout]))

(defn sedation-consent-page [request]
  (layout/render request "report/sedation_consent.html"))
