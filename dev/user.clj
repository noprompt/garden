(ns user
  (:require
    cljs.repl
    cljs.build.api
    cljs.repl.node
    cljs.repl.browser))

(defrecord Dirs [dirs]
  cljs.closure/Inputs
  (-paths [_]
    (mapv clojure.java.io/file dirs))

  cljs.closure/Compilable
  (-compile [_ opts]
    (let [out-dir (cljs.util/output-directory opts)]
      (vec
       (for [src-dir dirs
             root    (cljs.compiler/compile-root src-dir out-dir opts)]
          (cljs.closure/compiled-file root))))))

(defn repl [main env & dirs]
  (cljs.build.api/build (Dirs. (concat ["src" "test"] dirs))
    {:main       main
     :output-to  "target/garden.js"
     :output-dir "target/dev"
     :warnings   {:single-segment-namespace false}
     :verbose    true})

  (cljs.repl/repl env
    :watch      (Dirs. (concat ["src" "test"] dirs))
    :output-dir "target/dev"))

(defn node-repl []
  (repl 'garden (cljs.repl.node/repl-env)))
