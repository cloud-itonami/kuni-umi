;; `kotoba/site_survey/jurisdiction.kotoba` against
;; `kuni-umi.cells.site-survey.cell` (ADR-2608261100).
;;
;; The guest cannot require the cell and the cell does not require the guest,
;; so nothing but this file notices the two drifting apart. The `.cljc` stays
;; the oracle: every expected value below is computed by calling it, never
;; typed in by hand.
;;
;; Two negative controls are here on purpose, and each names the reason it is
;; supposed to refuse:
;;
;;   * `governor-refuses-a-permissive-guest` mutates ONE branch — the
;;     constitutional floor's "commons" becomes "military" — and asserts that
;;     the mutated guest actually admits a military site (so the mutation
;;     landed) and that the oracle still refuses it. Source that no longer
;;     reads is a different failure and is not this test.
;;   * `next-node-refuses-a-guest-that-skips-the-jurisdiction-gate` rewires the
;;     graph so collect_sensor_blob jumps straight to witness_attest, and
;;     asserts the mutated guest really does skip the gate.
;;
;; TEST-ONLY dependency: the compiler is in the `:test` alias, nothing in
;; `:paths` reaches it. Compiling in-process (KIR) is what lets the expected
;; values come from the oracle rather than from literals in the guest.

(ns kuni-umi.cells.site-survey.jurisdiction-kotoba-parity-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [kotoba.lang.text :as str]
            [clojure.test :refer [deftest is testing]]
            [kotoba.compiler.core :as compiler]
            [kotoba.kir :as ir]
            [kuni-umi.cells.site-survey.cell :as cell]))

(def ^:private kotoba-file
  (io/file (System/getProperty "user.dir") "kotoba" "site_survey" "jurisdiction.kotoba"))

(defn- source-available? []
  (let [present? (.exists kotoba-file)]
    (is present? (str "kotoba object not found at " kotoba-file))
    present?))

(defn- compile-source [src]
  (:kir (compiler/compile-source src :wasm32-kotoba-v1 {})))

(def ^:private kir (delay (compile-source (slurp kotoba-file))))

(defn- call [compiled f args] (ir/execute compiled f args))

;; ── Fixtures ──────────────────────────────────────────────────────
;; Each case is the oracle's (state, advisor-proposal) pair plus the guest's
;; flattened view of the same thing. `nil` becomes "" on the way in: a Kotoba
;; string has no nil, and the oracle spells absence `str/blank?` so the two
;; agree about a blank field as well as an unset one.

(def ^:private eligible
  {:intendedUse "community"
   :jurisdictionDid "did:web:example.gov"
   :localLawAttestationCid "bafyattestation"})

(defn- guest-args
  "Lower (state, advisor-proposal) to the guest's parameters."
  [state advisor-proposal]
  [(or (:intendedUse state) "")
   (or (:jurisdictionDid state) "")
   (or (:localLawAttestationCid state) "")
   (cell/advisor-verdict advisor-proposal)
   (double (get advisor-proposal :confidence 1.0))])

(def ^:private cases
  [["fresh empty state"        cell/site-survey-state nil]
   ["eligible, no advisor"     eligible               nil]
   ["eligible, mock advisor"   eligible               (cell/mock-advise eligible)]
   ["military use"             (assoc eligible :intendedUse "military")
    {:accepted true :rationale "advisor says fine" :confidence 1.0}]
   ["proprietary-closed use"   (assoc eligible :intendedUse "proprietary-closed-design") nil]
   ["missing jurisdictionDid"  (dissoc eligible :jurisdictionDid) nil]
   ["blank jurisdictionDid"    (assoc eligible :jurisdictionDid "") nil]
   ["missing attestation"      (dissoc eligible :localLawAttestationCid) nil]
   ["advisor rejects"          eligible {:accepted false :rationale "looks off" :confidence 0.9}]
   ["advisor unsure"           eligible {:accepted true :rationale "unsure" :confidence 0.2}]
   ["advisor exactly at floor" eligible {:accepted true :confidence 0.5}]
   ["advisor just under floor" eligible {:accepted true :confidence 0.4999}]
   ["advisor omits :accepted"  eligible {:rationale "no verdict" :confidence 0.9}]
   ["advisor omits confidence" eligible {:accepted true :rationale "silent"}]])

(deftest kotoba-guest-is-present-and-compiles
  (when (source-available?)
    (is (some? @kir))
    (is (= #{"" "intended-use-not-civilian" "jurisdiction-did-missing"
             "local-law-attestation-missing" "advisor-rejected"
             "advisor-confidence-below-floor"}
           (set (map (fn [[_ state proposal]]
                       (cell/jurisdiction-rejection-code state proposal))
                     cases)))
        "the fixtures must reach every branch of the decision, or the parity
         run below is only checking the branches nobody wrote a case for")))

(deftest rejection-codes-agree-with-the-cljc-oracle
  (when (source-available?)
    (doseq [[label state proposal] cases]
      (is (= (cell/jurisdiction-rejection-code state proposal)
             (call @kir 'rejection-code (guest-args state proposal)))
          label))))

(deftest accepted-agrees-with-the-cljc-oracle
  (when (source-available?)
    (doseq [[label state proposal] cases]
      (is (= (:accepted (cell/jurisdiction-governor state proposal))
             (call @kir 'accepted? (guest-args state proposal)))
          label))))

(deftest civilian-floor-agrees-with-the-cljc-oracle
  (when (source-available?)
    (doseq [use ["civilian" "community" "commons" "military"
                 "proprietary-closed-design" "" "Civilian"]]
      (is (= (contains? cell/constitutional-intended-uses use)
             (call @kir 'civilian-use? [use]))
          use))))

(deftest confidence-floor-agrees-with-the-cljc-oracle
  (when (source-available?)
    (is (= (double cell/advisor-confidence-floor)
           (call @kir 'advisor-confidence-floor [])))))

(deftest graph-traversal-agrees-with-the-cljc-oracle
  (when (source-available?)
    (doseq [node ["START" "allocate_scout_fleet" "collect_sensor_blob"
                  "jurisdiction_eligibility" "witness_attest" "emit_survey"
                  "no_such_node"]
            accepted [true false]]
      (is (= (cell/next-node node accepted)
             (call @kir 'next-node [node accepted]))
          (str node " accepted=" accepted)))))

(deftest route-agrees-with-the-cljc-oracle
  (when (source-available?)
    (doseq [accepted [true false]]
      (is (= (cell/router {:accepted accepted})
             (call @kir 'route [accepted]))
          (str "accepted=" accepted)))))

;; ── Negative controls ─────────────────────────────────────────────

(deftest governor-refuses-a-permissive-guest
  (testing "a guest whose constitutional floor admits military use must not
            pass as the oracle's floor — and the mutation has to be what
            changed the answer, not a source that stopped reading"
    (when (source-available?)
      (let [mutated (compile-source (str/replace (slurp kotoba-file)
                                                 "\"commons\"" "\"military\""))
            state (assoc eligible :intendedUse "military")
            args (guest-args state nil)]
        (is (true? (call mutated 'civilian-use? ["military"]))
            "the mutation has to actually admit military use")
        (is (= "" (call mutated 'rejection-code args))
            "the mutated guest has to actually accept the military site")
        (is (not= (cell/jurisdiction-rejection-code state nil)
                  (call mutated 'rejection-code args))
            "a permissive guest must not agree with the oracle")))))

(deftest next-node-refuses-a-guest-that-skips-the-jurisdiction-gate
  (when (source-available?)
    (let [mutated (compile-source
                   (str/replace (slurp kotoba-file)
                                "(string=? node \"collect_sensor_blob\")      \"jurisdiction_eligibility\""
                                "(string=? node \"collect_sensor_blob\")      \"witness_attest\""))]
      (is (= "witness_attest" (call mutated 'next-node ["collect_sensor_blob" true]))
          "the mutation has to actually reroute the edge")
      (is (not= (cell/next-node "collect_sensor_blob" true)
                (call mutated 'next-node ["collect_sensor_blob" true]))
          "skipping the jurisdiction gate must not pass as the oracle's graph"))))

;; ── The public compile path ───────────────────────────────────────

(defn- kotoba-bin [] (or (System/getenv "KOTOBA") "kotoba"))

(defn- kotoba-compiler-cli?
  "Whether the name resolves to THE COMPILER, not merely to something that
  runs. Two separate host facts have to hold and each has bitten:

    * it must EXECUTE — a shim can resolve on PATH and then exit 126, which
      reports as a compile failure rather than as the absent toolchain it is
      (measured 2026-08-26 in org-ietf-smtp: sixteen red assertions).
    * it must be THIS `kotoba` — measured 2026-08-30 on this workstation,
      `/opt/homebrew/bin/kotoba` is the knowledge-graph node CLI, an unrelated
      Rust binary of the same name whose `--help` exits 0. Reading only the
      exit code says the compiler is present and then reports `unexpected
      argument '-M'` as a compile failure. Ask what answered, not just whether
      something did. Set KOTOBA to `orgs/kotoba-lang/amu/bin/kotoba` to run
      this leg."
  []
  (try (let [{:keys [exit out err]} (shell/sh (kotoba-bin) "--help")]
         (and (zero? exit)
              (str/includes? (str out err) "-M compile")))
       (catch Exception _ false)))

(deftest kotoba-cli-compiles-the-guest
  "`-M` is the execution boundary, the wasm target is `wasm32-browser`, the
  flag is `--output`, and the source path must be absolute — a relative one
  comes back :decode / \"input could not be read\". Skip rather than fail when
  the compiler CLI is not on PATH: that is a host fact, not a disagreement
  between the two implementations. The in-process KIR runs above are the
  parity check and they do not skip."
  (when (source-available?)
    (if (kotoba-compiler-cli?)
      (let [dir (io/file (System/getProperty "java.io.tmpdir")
                         (str "kuni-umi-jurisdiction-" (System/nanoTime)))]
        (.mkdirs dir)
        (doseq [[target ext] [["wasm32-browser" ".wasm"] ["js-browser" ".mjs"]]
                :let [out (io/file dir (str "jurisdiction-" target ext))
                      result (shell/sh (kotoba-bin) "-M" "compile"
                                       (.getAbsolutePath kotoba-file)
                                       "--target" target
                                       "--output" (.getAbsolutePath out))]]
          (is (zero? (:exit result))
              (str "-M compile --target " target "\n" (:err result) (:out result)))
          (is (.isFile out) (str target " emitted nothing"))))
      (println "SKIP kotoba-cli-compiles-the-guest:" (kotoba-bin)
               "is not the Kotoba compiler CLI — set KOTOBA to"
               "orgs/kotoba-lang/amu/bin/kotoba"))))
