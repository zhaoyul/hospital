(ns hc.hospital.web.middleware.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [hc.hospital.web.middleware.core :as core]))

(deftest wrap-force-logout-cookie-test
  (let [attrs {:path "/" :http-only true :same-site :strict :secure false}
        logout-handler (fn [_] {:status 200 :body "bye" ::core/force-expire-cookie true})
        wrapped (core/wrap-force-logout-cookie logout-handler "hc.hospital" attrs)
        resp (wrapped {})]
    (is (= 200 (:status resp)))
    (is (= "" (get-in resp [:cookies "hc.hospital" :value])))
    (is (= 0 (get-in resp [:cookies "hc.hospital" :max-age])))
    (is (= "Thu, 01 Jan 1970 00:00:00 GMT" (get-in resp [:cookies "hc.hospital" :expires])))
    (let [header (get-in resp [:headers "Set-Cookie"])
          header-str (if (vector? header) (str/join ";" header) header)]
      (is (str/includes? header-str "Max-Age=0"))
      (is (str/includes? header-str "Expires=Thu, 01 Jan 1970 00:00:00 GMT")))))
