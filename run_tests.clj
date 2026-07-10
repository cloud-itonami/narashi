#!/usr/bin/env bb
;; narashi — bb test runner (ADR-2607101800). New actors ship run_tests.clj,
;; not run_tests.sh, per etzhayyim/root CLAUDE.md.
(require '[clojure.test :as test]
         'methods.test-charter-gates)

(let [results (test/run-tests 'methods.test-charter-gates)]
  (System/exit (if (zero? (+ (:fail results) (:error results))) 0 1)))
