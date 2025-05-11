(ns hc.hospital.web.handler
  (:require
   [hc.hospital.web.middleware.core :as middleware]
   [hc.hospital.web.routes.doctor-api]
   [hc.hospital.web.routes.patient-api]
   [hc.hospital.web.routes.patient-pages]
   [integrant.core :as ig]
   [reitit.ring :as ring]
   [reitit.ring.middleware.dev :as dev]
   [reitit.swagger-ui :as swagger-ui] ;; 引入新路由命名空间
   [ring.util.http-response :as http-response]))

(defmethod ig/init-key :handler/ring
  [_ {:keys [router api-path] :as opts}]
  (ring/ring-handler
   (router)
   (ring/routes
    ;; Handle trailing slash in routes - add it + redirect to it
    ;; https://github.com/metosin/reitit/blob/master/doc/ring/slash_handler.md
    (ring/redirect-trailing-slash-handler)
    (ring/create-resource-handler {:path "/"})
    (when (some? api-path)
      (swagger-ui/create-swagger-ui-handler {:path api-path
                                             :url  (str api-path "/swagger.json")}))
    (ring/create-default-handler
     {:not-found
      (constantly (-> {:status 404, :body "Page not found"}
                      (http-response/content-type "text/plain")))
      :method-not-allowed
      (constantly (-> {:status 405, :body "Not allowed"}
                      (http-response/content-type "text/plain")))
      :not-acceptable
      (constantly (-> {:status 406, :body "Not acceptable"}
                      (http-response/content-type "text/plain")))}))
   {:middleware [(middleware/wrap-base opts)]}))

(defmethod ig/init-key :router/routes
  [_ {:keys [routes]}]
  (mapv (fn [route]
          (if (fn? route)
            (route)
            route))
        routes))

(defmethod ig/init-key :router/core
  [_ {:keys [routes env] :as opts}]
  (if (= env :dev)
    #(ring/router ["" opts routes] #_{:reitit.middleware/transform dev/print-request-diffs})
    (constantly (ring/router ["" opts routes]))))
