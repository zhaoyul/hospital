(ns hc.hospital.web.middleware.auth-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [hc.hospital.web.middleware.auth :as auth]))

(deftest on-error-redirects-to-login
  (let [req (mock/request :get "/secret")
        resp (auth/on-error req nil)]
    (is (= 302 (:status resp)))
    (is (= "/login" (get-in resp [:headers "Location"])))
    (is (= "Access to /secret is not authorized. Please log in." (-> resp :flash :error)))))

(deftest wrap-restricted-requires-authentication
  (let [handler (constantly {:status 200 :body "ok"})
        restricted (auth/wrap-restricted handler)]
    (testing "unauthenticated request gets redirected"
      (let [resp (restricted (mock/request :get "/"))]
        (is (= 302 (:status resp)))))
    (testing "authenticated request passes through"
      (let [resp (restricted (-> (mock/request :get "/")
                                 (assoc :identity {:user "bob"})))]
        (is (= 200 (:status resp)))
        (is (= "ok" (:body resp)))))))
