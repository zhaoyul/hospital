(ns hc.hospital.web.middleware.core
  (:require
   [hc.hospital.env :as env]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.session.cookie :as cookie]
   [hc.hospital.web.middleware.auth :as auth-mw]))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})
        env-defaults-fn (:middleware env/defaults)]
    (fn [handler]
      (-> handler
          (env-defaults-fn opts)
          (auth-mw/wrap-auth)
          (defaults/wrap-defaults (assoc-in site-defaults-config [:session :store] cookie-store))))))
