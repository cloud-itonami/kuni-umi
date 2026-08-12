;; Standalone test-suite entrypoint. Run it through the task registry:
;;   nbb scripts/run-task.cljs test
;; which invokes `clojure -M:test run_tests.clj`. It carried a
;; `#!/usr/bin/env bb` shebang until 2026-08-13; bb was retired by
;; ADR-2607173000 and is not installed on the murakumo fleet nodes.
(require '[clojure.test :as t])

(def suites
  '[kuni-umi.cells.audit-witness.test-cell
    kuni-umi.cells.commissioning.cell-test
    kuni-umi.cells.construction-orchestration.test-cell
    kuni-umi.cells.decommission.test-cell
    kuni-umi.cells.deployment-planning.test-cell
    kuni-umi.cells.site-survey.test-cell
    kuni-umi.cells.social-post.state-machine-test
    kuni-umi.kotoba.ingest-mcp-test
    kuni-umi.repository-contract-test
    kuni-umi.robotics.kami-parity-test
    kuni-umi.robotics.test-kinematics
    kuni-umi.robotics.test-robotics])

(apply require suites)
(let [{:keys [fail error] :as result} (apply t/run-tests suites)]
  (println (select-keys result [:test :pass :fail :error]))
  (when (pos? (+ fail error))
    (System/exit 1)))
