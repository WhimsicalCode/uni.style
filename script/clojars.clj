;; The Clojars artifact com.whimsical/uni.style: the library's sources and
;; resources (including its clj-kondo exports), with a pom of deps.edn's :deps.
;; Its version is in VERSION. Run by script/clojars:
;;
;;   jar             writes target/clojars/uni.style-<version>.jar and its pom
;;   install <repo>  installs the jar into the local Maven repository <repo>
;;   deploy          deploys the jar to Clojars, as CLOJARS_USERNAME with the
;;                   deploy token CLOJARS_PASSWORD

(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.tools.build.api :as b]
         '[deps-deploy.deps-deploy :as dd])

(def lib 'com.whimsical/uni.style)

(def version (str/trim (slurp "VERSION")))

(def paths ["src" "resources"])

(def deps (:deps (edn/read-string (slurp "deps.edn"))))

(def class-dir "target/clojars/classes")

(def jar-file (format "target/clojars/uni.style-%s.jar" version))

(defn basis [extra]
  (b/create-basis {:user nil :project (merge {:paths paths :deps deps} extra)}))

(defn jar []
  (b/delete {:path "target/clojars"})
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis (basis nil)
                :src-dirs ["src"]
                :resource-dirs ["resources"]
                :scm {:url "https://github.com/WhimsicalCode/uni.style"
                      :connection "scm:git:https://github.com/WhimsicalCode/uni.style.git"
                      :tag (str "v" version)}
                :pom-data [[:description "Compile-time atomic CSS for ClojureScript and Clojure"]
                           [:url "https://github.com/WhimsicalCode/uni.style"]
                           [:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/license/mit/"]]]]})
  (b/copy-dir {:src-dirs paths :target-dir class-dir})
  (b/copy-file {:src "LICENSE" :target (str class-dir "/META-INF/LICENSE")})
  (b/jar {:class-dir class-dir :jar-file jar-file})
  (println "Wrote" jar-file))

(defn install [repo]
  (b/install {:basis (basis {:mvn/local-repo repo})
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir})
  (println "Installed" lib version "into" repo))

(defn deploy []
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(let [[command repo] *command-line-args*]
  (case command
    "jar"     (jar)
    "install" (do (jar) (install repo))
    "deploy"  (do (jar) (deploy))
    (do (println "Usage: script/clojars jar | install <local-repo> | deploy")
        (System/exit 2)))
  (shutdown-agents))
