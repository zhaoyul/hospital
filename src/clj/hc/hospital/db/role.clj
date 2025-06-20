(ns hc.hospital.db.role)

(defn list-roles [query-fn]
  (query-fn :list-roles {}))

(defn create-role! [query-fn name]
  (query-fn :create-role! {:name name}))

(defn update-role-name! [query-fn id name]
  (query-fn :update-role-name! {:id id :name name}))

(defn delete-role! [query-fn id]
  (query-fn :delete-role! {:id id}))

(defn list-permissions [query-fn]
  (query-fn :list-permissions {}))

(defn get-permissions-by-role [query-fn role-id]
  (query-fn :get-permissions-by-role {:role_id role-id}))

(defn set-role-permissions! [query-fn role-id permission-ids]
  (query-fn :delete-role-permissions! {:role_id role-id})
  (doseq [pid permission-ids]
    (query-fn :add-role-permission! {:role_id role-id :permission_id pid})))
