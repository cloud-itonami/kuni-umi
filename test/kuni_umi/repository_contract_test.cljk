(ns kuni-umi.repository-contract-test
  (:require [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(def contract (edn/read-string (slurp "repository-contracts.edn")))

(deftest canonical-edn-and-wire-boundary
  (is (= :edn (get-in contract [:canonical :format])))
  (doseq [path ["manifest.edn" "identity.edn" "dependencies.edn"
                "wire/manifest.jsonld"
                "wire/bpmn/kuni-umi-deployment-workflow.bpmn"
                "wire/robotics/golden/device_loop_trace.json"
                "wire/robotics/golden/kami_fk_ik_trace.json"]]
    (is (.isFile (io/file path)) path))
  (doseq [path (:forbidden-root-paths contract)]
    (is (not (.exists (io/file path))) path)))

(deftest central-action-lexicons-are-owned-here
  (let [names ["commissionDeployment" "defineDeploymentSite"
               "proposeDeploymentPlan" "recordConstructionProgress"
               "recordPhysicalAuditEvent" "submitSiteSurvey"]]
    (doseq [name names]
      (is (.isFile (io/file "lex/actions" (str name ".edn"))))
      (is (.isFile (io/file "wire/lexicons/kuniUmi" (str name ".json")))))))

(deftest legacy-apps-namespace-is-wire-only
  (let [names ["commissionDeployment" "defineDeploymentSite"
               "proposeDeploymentPlan" "recordConstructionProgress"
               "recordPhysicalAuditEvent" "submitSiteSurvey"]
        legacy (first (get-in contract [:wire :legacy-compatibility]))]
    (is (= "com.etzhayyim.apps.etzhayyim.kuniUmi" (:namespace legacy)))
    (is (false? (:canonical legacy)))
    (doseq [name names]
      (let [path (io/file (:path legacy) (str name ".json"))
            document (json/parse-string (slurp path))]
        (is (.isFile path))
        (is (= (str (:namespace legacy) "." name) (get document "id")))
        (is (not (.exists (io/file "lex/apps-etzhayyim-kuniUmi"
                                   (str name ".edn")))))))))

(deftest exact-flat-west-dependencies
  (let [deps (edn/read-string (slurp "dependencies.edn"))]
    (is (= "orgs/etzhayyim/com-etzhayyim-kuni-umi" (:west/project deps)))
    (doseq [{:keys [path revision]} (:dependencies deps)]
      (is (re-matches #"orgs/[^/]+/[^/]+" path))
      (is (re-matches #"[0-9a-f]{40}" revision)))))

(deftest duplicate-root-aliases-are-one-actor
  (let [migration (edn/read-string (slurp "migration.edn"))
        manifest (edn/read-string (slurp "manifest.edn"))]
    (is (= "kuni-umi" (:canonical-actor-id migration) (:actor/id manifest)))
    (is (= "kuni-umi.etzhayyim.com" (get-in migration [:identity-evidence :actor-domain])
           (:actor/domain manifest)))
    (is (= #{"20-actors/kuni-umi" "20-actors/kuni_umi"}
           (set (map :path (:root-aliases migration)))))))
