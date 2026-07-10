(ns methods.test-charter-gates
  "narashi — constitutional-gate conformance tests. Substrate-native Clojure
  (ADR-2607101800), following the kanae/danjo test_charter_gates.cljc pattern.
  Lexicons live in the sibling etzhayyim/root checkout
  (00-contracts/lexicons/com/etzhayyim/narashi/) per the west-managed
  orgs/etzhayyim/{root,com-etzhayyim-narashi} sibling layout."
  (:require [clojure.test :refer [deftest is run-tests]]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def ^:private here (.getParentFile (java.io.File. ^String *file*)))
(def ^:private actor-dir (.getParentFile here))
(def ^:private orgs-etzhayyim-dir (.getParentFile actor-dir))
(def ^:private lexdir
  (java.io.File. orgs-etzhayyim-dir "root/00-contracts/lexicons/com/etzhayyim/narashi"))

(defn- manifest [] (json/parse-string (slurp (java.io.File. actor-dir "manifest.jsonld"))))
(defn- lex [name] (json/parse-string (slurp (java.io.File. lexdir (str name ".json")))))

(defn- required-union [doc]
  (let [acc (atom #{})]
    (letfn [(walk [x] (cond (map? x) (do (when (sequential? (get x "required")) (swap! acc into (get x "required")))
                                         (doseq [v (vals x)] (walk v)))
                            (sequential? x) (doseq [v x] (walk v))))]
      (walk doc)) @acc))

(defn- property-keys [doc]
  (let [acc (atom #{})]
    (letfn [(walk [x] (cond (map? x) (do (when (map? (get x "properties")) (swap! acc into (keys (get x "properties"))))
                                         (doseq [v (vals x)] (walk v)))
                            (sequential? x) (doseq [v x] (walk v))))]
      (walk doc)) @acc))

(defn- const-field [doc field]
  (let [acc (atom :not-found)]
    (letfn [(walk [x] (cond (and (map? x) (map? (get x "properties")) (contains? (get x "properties") field))
                             (reset! acc (get-in x ["properties" field "const"]))
                             (map? x) (doseq [v (vals x)] (walk v))
                             (sequential? x) (doseq [v x] (walk v))))]
      (walk doc)) @acc))

(def ^:private indicator-values
  #{"gini" "poverty-headcount-ratio-international" "poverty-headcount-ratio-national"
    "income-share-bottom40" "income-share-top10" "sdg10-shared-prosperity-premium"})

;; -- full gate set --
(deftest test-all-9-gates-declared
  (let [gates (get-in (manifest) ["constitutionalGates" "gates"])]
    (is (= (set (keys gates)) (set (map #(str "G" %) (range 1 10))))
        "manifest must declare G1-G9")))

(deftest test-all-8-nongoals-declared
  (let [goals (get-in (manifest) ["nonGoals" "goals"])]
    (is (= (set (keys goals)) (set (map #(str "N" %) (range 1 9))))
        "manifest must declare N1-N8")))

;; -- G4 non-adjudication --
(deftest test-g4-non-adjudicating-notice
  (is (= true (const-field (lex "metricNarrative") "nonAdjudicatingNotice"))
      "G4: metricNarrative.nonAdjudicatingNotice must be const true"))

(deftest test-g4-no-verdict-or-ranking-field
  (doseq [name ["metricObservation" "metricNarrative"]]
    (let [keys (set (map str/lower-case (property-keys (lex name))))]
      (doseq [bad ["verdict" "ranking" "score" "grade" "rating"]]
        (is (not (contains? keys bad))
            (str "G4: " name " must not carry a '" bad "' field (narashi ranks no jurisdiction)"))))))

;; -- G8 non-causal cross-reference (narashi-specific) --
(deftest test-g8-causal-claim-const-false
  (is (= false (const-field (lex "crossReferenceNote") "causalClaim"))
      "G8: crossReferenceNote.causalClaim must be const false"))

;; -- G5 source provenance --
(deftest test-g5-source-provenance-required
  (let [req (required-union (lex "metricObservation"))]
    (is (contains? req "sourceRecordCids") "G5: metricObservation must require sourceRecordCids")
    (is (contains? req "methodNoteCid") "G5/G6: metricObservation must require methodNoteCid")))

;; -- G7 Murakumo-only narration --
(deftest test-g7-murakumo-only-narration
  (let [req (required-union (lex "metricNarrative"))]
    (is (contains? req "murakumoInferenceAttestation")
        "G7: metricNarrative must require murakumoInferenceAttestation")))

;; -- G6 open method --
(deftest test-g6-method-note-fields
  (let [req (required-union (lex "methodNote"))]
    (doseq [field ["version" "appliesToCell" "description"]]
      (is (contains? req field) (str "G6: methodNote must require " field)))))

;; -- bounded indicator vocabulary --
(deftest test-bounded-indicator-vocabulary
  (let [doc (lex "metricObservation")
        found (get-in doc ["defs" "main" "record" "properties" "indicator" "knownValues"])]
    (is (= (set found) indicator-values) "indicator vocabulary drifted from manifest cell docs")))

;; -- N1: no redistribution-shaped fields anywhere in narashi's own lexicons --
(deftest test-n1-no-redistribution-fields
  (doseq [name ["metricObservation" "crossReferenceNote" "metricNarrative" "methodNote"]]
    (let [keys (set (map str/lower-case (property-keys (lex name))))]
      (doseq [bad ["amountusdc" "grantamount" "stipend" "payoutamount" "disbursement"]]
        (is (not (contains? keys bad))
            (str "N1: " name " must not carry a '" bad "' field (narashi never moves funds)"))))))
