(ns hc.hospital.web.handler
  "组装 Ring 处理器与路由"
  (:require
   [hc.hospital.web.middleware.core :as middleware]
   [hc.hospital.web.routes.user-api]
   [hc.hospital.web.routes.patient-api]
   [hc.hospital.web.routes.patient-pages]
   [integrant.core :as ig]
   [reitit.ring :as ring]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :as http-response]))

(defmethod ig/init-key :handler/ring
  [_ {:keys [router api-path] :as opts}]
  (ring/ring-handler
   (router)
   (ring/routes
    ;; 统一处理尾随斜杠
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
