(ns hc.hospital.logging-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [hc.hospital.logging :as log])
  (:import [java.time Instant ZoneId]
           [java.time.format DateTimeFormatter]
           [java.nio.file Files]))

(deftest daily-file-appender-writes-log
  (let [tmp-dir (.toString (Files/createTempDirectory "log-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        inst (Instant/parse "2025-01-01T12:00:00Z")
        app-fn (:fn (log/daily-file-appender {:dir tmp-dir :prefix "test" :async? false}))
        msg "hello\n"
        date (.format (.atZone inst (ZoneId/systemDefault)) (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
        expected-path (str tmp-dir "/test-" date ".log")]
    (try
      (app-fn {:timestamp_ (delay inst) :output_ (delay msg)})
      (is (.exists (io/file expected-path)))
      (is (= msg (slurp expected-path)))
      (finally
        (io/delete-file expected-path true)
        (io/delete-file tmp-dir true))))
)
