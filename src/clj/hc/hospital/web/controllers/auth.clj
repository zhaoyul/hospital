(ns hc.hospital.web.controllers.auth
  (:require
   [taoensso.timbre :as ctl]
   [hc.hospital.db.doctor :as doctor.db]
   [hc.hospital.web.pages.layout :as layout]
   [ring.util.http-response :as http-response]))

(defn login-page
  "Render the login page."
  [request]
  (ctl/info "渲染login.html")
  (layout/render request "login.html"))

(defn handle-login!
  "Handle doctor login attempt."
  [{{:keys [username password]} :params
    {:keys [query-fn]} :integrant-deps
    :as request}]
  (if (or (empty? username) (empty? password))
    (layout/render request "login.html" {:error "用户名和密码不能为空"})
    (if-let [doctor (doctor.db/verify-doctor-credentials query-fn username password)]
      (let [session (:session request)
            updated-session (assoc session :identity {:id (:id doctor) :username (:username doctor)})]
        (-> (http-response/found "/")   ; Redirect to home page after login
            (assoc :session updated-session)))
      (layout/render request "login.html" {:error "无效的用户名或密码"}))))

(defn handle-logout!
  "Handle doctor logout."
  [request]
  (-> (http-response/found "/login") ; Redirect to login page after logout
      (assoc :session nil)))
