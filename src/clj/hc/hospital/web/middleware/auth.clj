(ns hc.hospital.web.middleware.auth
  (:require
   [buddy.auth :as auth]
   [buddy.auth.accessrules :as accessrules]
   [buddy.auth.backends.session :as session]
   [buddy.auth.middleware :as auth-middleware]
   [clojure.tools.logging :as log]
   [ring.util.http-response :as http-response])) ; Added for redirect

(defn on-error [request _response]
  (-> (http-response/found "/login")
      (assoc :flash {:error (str "Access to " (:uri request) " is not authorized. Please log in.")})))

(defn wrap-restricted [handler]
  (accessrules/restrict handler {:handler auth/authenticated?
                                 :on-error on-error}))

(defn wrap-auth [handler]
  (let [backend (session/session-backend)]
    (-> handler
        (auth-middleware/wrap-authorization backend)
        (auth-middleware/wrap-authentication backend))))
