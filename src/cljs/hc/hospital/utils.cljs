(ns hc.hospital.utils)

(defn event-value [event]
  (.. event -target -value))
