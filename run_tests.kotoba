#!/usr/bin/env bb
;; narashi — standalone bb test runner.
(require '[clojure.test :as test])

(def suites '[narashi.methods.test-charter-gates
              narashi.social-test])

(apply require suites)

(let [results (apply test/run-tests suites)]
  (System/exit (if (zero? (+ (:fail results) (:error results))) 0 1)))
