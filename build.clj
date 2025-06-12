(ns build
  (:require [clojure.string :as string]
            [clojure.tools.build.api :as b]
            [babashka.fs :as fs :refer [copy-tree]]
            [babashka.process :refer [shell]]
            [clojure.java.io :as io])
  (:import [java.util Date Properties]
           [java.text SimpleDateFormat]
           [java.io FileOutputStream]))

(defn build-cljs [] (println "npx shadow-cljs release app patient-app...") (let [{:keys [exit], :as s} (shell "npx shadow-cljs release app patient-app")] (when-not (zero? exit) (throw (ex-info "could not compile cljs" s))) (copy-tree "target/classes/cljsbuild/public" "target/classes/public")))

(def lib 'hc/hospital)
(def main-cls (string/join "." (filter some? [(namespace lib) (name lib) "core"])))
(def version (format "0.0.1-SNAPSHOT"))
(def target-dir "target")
(def class-dir (str target-dir "/" "classes"))
(def uber-file (format "%s/%s-standalone.jar" target-dir (name lib)))
(def basis (b/create-basis {:project "deps.edn"}))

(defn write-version-file []
  (println "Writing version.properties file...")
  (let [props (Properties.)
        commit-hash (try
                      (-> (shell {:out :string} "git rev-parse --short HEAD")
                          :out
                          string/trim)
                      (catch Exception e
                        (println "Error getting git commit hash:" (.getMessage e))
                        "UNKNOWN"))
        timestamp (-> (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z")
                      (.format (Date.)))
        version-file-dir "resources/hc/hospital"
        version-file-path (str version-file-dir "/version.properties")]
    (fs/create-dirs version-file-dir)
    (.setProperty props "git.commit.hash" commit-hash)
    (.setProperty props "build.timestamp" timestamp)
    (with-open [fos (FileOutputStream. version-file-path)]
      (.store props fos "Build version information"))
    (println (str "Version properties written to " version-file-path))))

(defn clean
  "Delete the build target directory"
  [_]
  (println (str "Cleaning " target-dir))
  (b/delete {:path target-dir}))

(defn prep [_]
  (write-version-file)
  (println "Writing Pom...")
  (println "Before b/write-pom")
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src/clj"]})
  (println "After b/write-pom, before b/copy-dir")
  ;; Copy only resource files to the class directory so that
  ;; the resulting jar contains compiled bytecode without the
  ;; original Clojure source.
  (b/copy-dir {:src-dirs ["resources" "env/prod/resources"]
               :target-dir class-dir})
  (println "After b/copy-dir"))

(defn uber [_]
  (println "Compiling Clojure...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src/clj" "resources" "env/prod/resources" "env/prod/clj"]
                  :class-dir class-dir})
  (build-cljs)
  (println "Making uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :main main-cls
           :basis basis}))

(defn compile-clj-only [_]
  (println "Compiling Clojure (only)...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src/clj" "resources" "env/prod/resources" "env/prod/clj"]
                  :class-dir class-dir})
  (println "Clojure compilation (only) finished."))

(defn all [_]
  (do (clean nil) (prep nil) (uber nil)))
