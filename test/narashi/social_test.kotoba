(ns narashi.social-test
  (:require [clojure.test :refer [deftest is testing]]
            [narashi.cells.social-post.state-machine :as state-machine]
            [narashi.methods.mesh :as mesh]
            [narashi.methods.social :as social]))

(deftest dry-run-post-requires-provenance
  (testing "two sources produce a non-adjudicating dry-run post"
    (let [post (social/draft-observation-post
                "Gini trend"
                "A disclosed aggregate moved between two periods."
                ["bafy-source-a" "bafy-source-b"]
                "did:web:etzhayyim.com:actor:narashi")]
      (is (= ":dry-run" (get post ":post/status")))
      (is (true? (get post ":post/non-adjudicating-notice")))
      (is (false? (get post ":post/server-held-key")))))
  (testing "one source is refused"
    (is (thrown? clojure.lang.ExceptionInfo
                 (social/draft-observation-post "subject" "body" ["only-one"])))))

(deftest live-publication-is-unrepresentable-at-r0
  (is (thrown? clojure.lang.ExceptionInfo (social/build-live {})))
  (is (thrown? clojure.lang.ExceptionInfo (mesh/run {})))
  (is (thrown? clojure.lang.ExceptionInfo (mesh/on-kse "topic" {}))))

(deftest publication-cell-enforces-r0-gates
  (let [drafted (state-machine/transition-to-drafted
                 {"subject" "Aggregate observation"
                  "sources" ["bafy-source-a" "bafy-source-b"]})
        live-request (state-machine/transition-to-drafted
                      {"subject" "Aggregate observation"
                       "sources" ["bafy-source-a" "bafy-source-b"]
                       "requested_status" "published"})
        server-key (state-machine/transition-to-drafted
                    {"subject" "Aggregate observation"
                     "sources" ["bafy-source-a" "bafy-source-b"]
                     "server_held_key" true})]
    (is (= state-machine/phase-drafted (get-in drafted ["cell_state" "phase"])))
    (is (= state-machine/phase-refused (get-in live-request ["cell_state" "phase"])))
    (is (= state-machine/phase-refused (get-in server-key ["cell_state" "phase"])))))
