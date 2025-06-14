(ns hc.hospital.db.doctor
  (:require [hc.hospital.db.user :as user]))

(def create-doctor! user/create-user!)
(def get-doctor-by-username user/get-user-by-username)
(def verify-doctor-credentials user/verify-credentials)
(def list-doctors user/list-users)
(def get-doctor-by-id user/get-user-by-id)
(def update-doctor-password! user/update-user-password!)
(def update-doctor-name! user/update-user-name!)
(def delete-doctor! user/delete-user!)
