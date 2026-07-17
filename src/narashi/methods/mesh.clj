(ns narashi.methods.mesh
  "R0 mesh boundary. Live execution remains unavailable until the Council gate
  declared by manifest.edn is satisfied.")

(defn- refuse [entrypoint]
  (throw (ex-info "narashi R0: mesh activation requires Council ratification"
                  {:actor "narashi"
                   :entrypoint entrypoint
                   :phase :r0
                   :status :refused})))

(defn run [_context]
  (refuse :run))

(defn on-kse [_topic _payload]
  (refuse :on-kse))
