(ns hc.hospital.web.middleware.core
  (:require
   [hc.hospital.env :as env]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.session.cookie :as cookie]
   [hc.hospital.web.middleware.auth :as auth-mw]
   [clojure.string :as str])) ; For str/starts-with? and str/capitalize

;; Helper to get the configured session cookie name
(defn- get-session-cookie-name [site-defaults-config]
  (get-in site-defaults-config [:session :cookie-name] "hc.hospital"))

;; Helper to get base cookie attributes (path, http-only, same-site, secure)
;; from the configuration, excluding max-age and expires.
(defn- get-base-cookie-attrs [site-defaults-config]
  (let [session-opts (get site-defaults-config :session {})
        cookie-attrs (get session-opts :cookie-attrs {})]
    (merge {:path "/" :http-only true :same-site :strict :secure false} ; Provide common defaults, including :secure false
           (dissoc cookie-attrs :max-age :expires)))) ; Remove max-age/expires as they are forced for logout

(defn wrap-force-logout-cookie [handler actual-cookie-name base-attrs-for-expired-cookie]
  (fn [request]
    (let [response (handler request)]
      (if (get response ::force-expire-cookie) ; Check for the marker from controller
        (let [;; 1. Define all attributes for the expired cookie for the :cookies map
              expired-cookie-map-entry {:value "" ; Empty value for expired cookie
                                        :path (:path base-attrs-for-expired-cookie)
                                        :max-age 0
                                        :expires "Thu, 01 Jan 1970 00:00:00 GMT"
                                        :http-only (:http-only base-attrs-for-expired-cookie)
                                        :same-site (:same-site base-attrs-for-expired-cookie)
                                        :secure (:secure base-attrs-for-expired-cookie)}

;; 2. Construct the Set-Cookie header string
              header-value-str (str actual-cookie-name "=") ; No value after '=' for expired cookie
              path-attr (str "; Path=" (:path base-attrs-for-expired-cookie))
              httponly-attr (if (:http-only base-attrs-for-expired-cookie) "; HttpOnly" "")
              samesite-val-name (when-let [ss (:same-site base-attrs-for-expired-cookie)] (name ss))
              samesite-attr (if samesite-val-name (str "; SameSite=" (str/capitalize samesite-val-name)) "")
              secure-attr (if (:secure base-attrs-for-expired-cookie) "; Secure" "")
              expires_attrs_str "; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
              full-expired-cookie-header (str header-value-str path-attr httponly-attr samesite-attr secure-attr expires_attrs_str)

              response-with-marker-removed (dissoc response ::force-expire-cookie)

              ;; Action A: Update :cookies map in the response
              response-updated-cookies-map (assoc-in response-with-marker-removed [:cookies actual-cookie-name] expired-cookie-map-entry)

              ;; Action B: Update :headers "Set-Cookie" in the response
              existing-headers (get response-updated-cookies-map :headers {})
              set-cookie-header-val (get existing-headers "Set-Cookie")

              final-set-cookie-value (cond
                                       (nil? set-cookie-header-val)
                                       full-expired-cookie-header

                                       (vector? set-cookie-header-val)
                                       (vec (cons full-expired-cookie-header
                                                  (remove #(str/starts-with? % (str actual-cookie-name "=")) set-cookie-header-val)))

                                       (string? set-cookie-header-val)
                                       (if (str/starts-with? set-cookie-header-val (str actual-cookie-name "="))
                                         full-expired-cookie-header ; Overwrite if it's the session cookie
                                         [set-cookie-header-val full-expired-cookie-header]) ; Preserve other, make vector

                                       :else full-expired-cookie-header) ; Fallback

              final-headers (assoc existing-headers "Set-Cookie" final-set-cookie-value)]

          (assoc response-updated-cookies-map :headers final-headers))
        response))))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store             (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})
        session-cookie-name      (get-session-cookie-name site-defaults-config)
        ;; Get base attributes (HttpOnly, SameSite, Path, Secure) from config to reuse them for the expired cookie
        base-attrs-for-logout    (get-base-cookie-attrs site-defaults-config)
        ;; Ensure wrap-defaults uses the cookie-store
        config-for-defaults      (assoc-in site-defaults-config [:session :store] cookie-store)
        env-defaults-fn          (:middleware env/defaults)]
    (fn [handler]
      (-> handler
          (env-defaults-fn opts)
          (auth-mw/wrap-auth)
          (defaults/wrap-defaults config-for-defaults) ; Uses static :cookie-attrs from system.edn
          ;; Our custom middleware runs after to force expire cookie on logout
          (wrap-force-logout-cookie session-cookie-name base-attrs-for-logout)))))
