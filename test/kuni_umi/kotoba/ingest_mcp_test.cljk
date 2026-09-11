(ns kuni-umi.kotoba.ingest-mcp-test
  (:require [clojure.test :refer [deftest is]]
            [kuni-umi.kotoba.ingest-mcp :as ingest]))

(deftest canonical-seed-is-one-edn-value
  (let [result (ingest/run {:dry-run? true})]
    (is (= :dry-run (:status result)))
    (is (= 4 (:entities result)))
    (is (pos? (:datoms result)))))
