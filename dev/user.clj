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

(defn build
  ([& options]
   "Build Cljs src with options"
   (println "Building Cljs and watching for changes ...")
   (let [start (System/nanoTime)]
     (println "setting main as: " 'garden)
     (cljs.build.api/build "src"
              {:main 'garden
               :output-to "target/build/garden.js"
               :output-dir "target/build"
               :optimizations :none
               :source-map true
               :verbose true})
     (println "... done. Elapsed" (/ (- (System/nanoTime) start) 1e9) "seconds"))))

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
