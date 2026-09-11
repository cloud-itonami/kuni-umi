(ns kuni-umi.cells.commissioning.cell-test
  (:require [clojure.test :refer [deftest is testing]]
            [kuni-umi.cells.commissioning.cell :as cell]))

(deftest offline-open-ot-registration
  (is (= ["did:loop:one"]
         (get (cell/register-with-open-ot {"openOtLoopDids" ["did:loop:one"]} {})
              "openOtLoopDids")))
  (is (thrown? clojure.lang.ExceptionInfo
               (cell/register-with-open-ot {} {:sdk :live}))))

(deftest commissioning-remains-r0-gated
  (is (thrown? clojure.lang.ExceptionInfo (cell/solve {})))
  (is (= "CommissioningCell:did:plan:1"
         (cell/thread-id-from-event {"value" {"planDid" "did:plan:1"}} nil)))
  (is (= "CommissioningCell:fallback"
         (cell/thread-id-from-event {"rkey" "fallback"} nil)))
  (testing "health metadata remains explicit"
    (is (= "4-commissioning" (get (cell/healthz-extra {}) "phase")))))
