(ns kuni-umi.robotics.kami-parity-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [kuni-umi.robotics.kinematics :as k]))

(def trace
  (json/parse-string
   (slurp "wire/robotics/golden/kami_fk_ik_trace.json") true))

(def arm (k/planar-arm [1.2 1.0]))
(def tolerance 5.0e-4)

(deftest committed-kami-fk-trace-matches-cljc
  (let [samples (get-in trace [:parity_2link :fk])]
    (is (seq samples))
    (doseq [{:keys [q tip]} samples]
      (let [{:keys [x y]} (k/fk arm q)]
        (is (< (Math/abs (- x (nth tip 0))) tolerance))
        (is (< (Math/abs (- y (nth tip 1))) tolerance))
        (is (< (Math/abs (nth tip 2)) 1.0e-6))))))

(deftest analytic-ik-covers-committed-kami-targets
  (doseq [{:keys [target]} (get-in trace [:parity_2link :ik])]
    (let [[x y] target]
      (is (k/reachable arm x y))
      (doseq [elbow-up? [true false]]
        (let [q (k/ik2 arm x y elbow-up?)
              pose (k/fk arm q)]
          (is (< (Math/hypot (- (:x pose) x) (- (:y pose) y)) 1.0e-6)))))))

(deftest giemon-six-dof-trace-remains-valid
  (let [{:keys [ndof joint_limits fk ik max_reach_upper_bound]}
        (:giemon_arm6 trace)]
    (is (= 6 ndof))
    (is (= 6 (count joint_limits)))
    (is (every? (fn [[lo hi]] (< lo 0 hi)) joint_limits))
    (is (every? (fn [{:keys [tip]}]
                  (<= (Math/sqrt (reduce + (map #(* % %) tip)))
                      (+ max_reach_upper_bound 1.0e-3)))
                fk))
    (is (every? #(< (:tip_err %) 1.0e-3) ik))))
