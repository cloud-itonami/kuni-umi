(ns kuni-umi.cells.social-post.state-machine-test
  (:require [clojure.test :refer [deftest is]]
            [kuni-umi.cells.social-post.state-machine :as state-machine]
            [kuni-umi.methods.social :as social]))

(deftest shared-social-adapter
  (let [post (social/draft-observation-post "site" "observed" ["survey" "audit"])
        drafted (state-machine/transition-to-drafted
                 {"subject" "site" "sources" ["survey" "audit"]})]
    (is (= ":dry-run" (get post ":post/status")))
    (is (false? (get post ":post/server-held-key")))
    (is (= state-machine/phase-drafted
           (get-in drafted ["cell_state" "phase"])))))
