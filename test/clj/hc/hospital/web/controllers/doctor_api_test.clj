(ns hc.hospital.web.controllers.doctor-api-test
  (:require [clojure.test :refer :all]
            [hc.hospital.test-utils :as tu]
            [ring.mock.request :as mock]
            [cheshire.core :as json]
            [clojure.string :as str]
            [hc.hospital.web.controllers.doctor-api :as doctor-api.ctlr]
            [hc.hospital.db.doctor :as doctor.db]))

(use-fixtures :once (tu/system-fixture))

(defn get-handler-and-query-fn []
  (let [system (tu/system-state)]
    {:handler (:handler/ring system)
     :query-fn (or (get system :db.sql/query-fn) ; Adjust if necessary
                   (get-in system [:db.sql/query-fn :query-fn]))}))

(deftest doctor-api-controller-tests
  (let [{:keys [handler query-fn]} (get-handler-and-query-fn)
        _ (assert handler "Ring handler should not be nil")
        _ (assert query-fn "Query function should not be nil")]

    (testing "医生注册 API"
      (testing "成功注册"
        (let [response (tu/POST handler "/api/doctors"
                                (json/encode {:username "api_doc1" :password "apipass" :name "API医生"})
                                {"content-type" "application/json"})]
          (is (= 200 (:status response)))
          (is (str/includes? (:body response) "医生注册成功"))))
      (testing "用户名已存在"
        (tu/POST handler "/api/doctors" (json/encode {:username "api_doc_dup" :password "pass" :name "Dup"}) {}) ; 先创建
        (let [response (tu/POST handler "/api/doctors"
                                (json/encode {:username "api_doc_dup" :password "pass" :name "Dup Again"})
                                {"content-type" "application/json"})]
          (is (= 409 (:status response)))
          (is (str/includes? (:body response) "用户名已存在"))))
      (testing "缺少参数"
        (let [response (tu/POST handler "/api/doctors"
                                (json/encode {:username "incomplete_doc"})
                                {"content-type" "application/json"})]
          (is (= 400 (:status response)))
          (is (str/includes? (:body response) "用户名和密码不能为空")))))


    (testing "医生登录和登出 API"
      (let [login-username "login_doc"
            login-password "login_pass"
            _ (doctor.db/create-doctor! query-fn {:username login-username :password login-password :name "登录测试"})]
        (testing "成功登录"
          (let [response (tu/POST handler "/api/doctors/login"
                                  (json/encode {:username login-username :password login-password})
                                  {"content-type" "application/json"})
                body (json/parse-string (:body response) true)]
            (is (= 200 (:status response)))
            (is (= "登录成功" (:message body)))
            (is (= login-username (get-in body [:doctor :username])))
            ;; 检查 session cookie 是否被设置 (更复杂的测试可能需要 Peridot 的会话跟踪)
            (is (some? (get-in response [:headers "Set-Cookie"])))))

        (testing "登录失败 - 密码错误"
          (let [response (tu/POST handler "/api/doctors/login"
                                  (json/encode {:username login-username :password "wrongpass"})
                                  {"content-type" "application/json"})]
            (is (= 401 (:status response)))))

        ;; 登出测试较为复杂.因为它依赖于会话.
        ;; 简单测试:调用登出端点.期望它返回成功.并清除会话.
        ;; 真正验证会话是否清除需要后续请求.
        ;; Peridot 的 p/session 可以更好地处理这个问题.
        ;; (testing "成功登出"
        ;;   ;; 需要先登录以建立会话
        ;;   (let [login-resp (tu/POST handler "/api/doctors/login"
        ;;                             (json/encode {:username login-username :password login-password})
        ;;                             {"content-type" "application/json"})
        ;;         cookies (get-in login-resp [:headers "Set-Cookie"])]
        ;;     (let [logout-resp (tu/POST handler "/api/doctors/logout" "" {"Cookie" (first cookies)})]
        ;;       (is (= 200 (:status logout-resp)))
        ;;       (is (str/includes? (:body logout-resp) "登出成功"))
        ;;       ;; 检查 Set-Cookie 是否用于清除会话 (例如.max-age=0 或 expires=past_date)
        ;;       (is (some #(str/includes? % "Max-Age=0") (get-in logout-resp [:headers "Set-Cookie"] "")))
        ;;       )))
        ))

    ;; 注意: 对于需要认证的端点 (如 /api/doctors, /api/doctors/:id),
    ;; 测试它们需要模拟已认证的会话.
    ;; 这通常通过在请求中包含有效的会话 cookie 来完成.
    ;; 使用 Ring Mock Request 时.你可能需要手动管理 session 或模拟 :identity.
    ;; 例如.直接调用控制器函数并提供一个包含 :identity 的模拟请求:
    #_(testing "获取医生列表 (模拟已认证)"
        (let [mock-req (-> (mock/request :get "/api/doctors")
                           (assoc :integrant-deps {:query-fn query-fn}) ; 控制器需要这个
                           (assoc :identity {:id 1}))] ; 模拟已认证用户ID为1, buddy-auth 通常期望 :identity 是一个 map
          (let [response (doctor-api.ctlr/list-doctors-handler mock-req)] ; 确保调用正确的处理函数
            (is (= 200 (:status response)))
            (is (some? (-> response :body (json/parse-string true) :doctors))))))))
