(defproject garden "1.3.8"
  :description "Generate CSS from Clojure(Script) data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.8"
                  :exclusions [rhino/js]]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :clean-targets ^{:protect false}  ["target"]
  :profiles {:dev
             {:source-paths ["src" "test" "dev"]
              :dependencies [[criterium "0.4.3"]]
              :plugins  [[codox "0.9.1"]
                         [lein-npm "0.6.1"]
                         [com.jakemccrary/lein-test-refresh "0.12.0"]]}}
  :aliases {"build-cljs" ["run" "-m" "user/build"]
            "test-clj" ["do" ["clean"] ["test"]]
            "test-cljs" ["do"
                         ["build-cljs"]
                         ["run" "-m" "garden.tests/test-all"]]
            "test-cljc" ["do"
                         ["test-clj"]
                         ["test-cljs"]]
            "node-repl" ["run" "-m" "user/node-repl"]})
