(defproject garden "1.3.1-SNAPSHOT"
  :description "Generate CSS from Clojure/Cljs data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :global-vars {*warn-on-reflection* true}

  :dependencies
  [[org.clojure/clojure "1.7.0" :scope "provided"]
   [org.clojure/clojurescript "1.7.28" :scope "provided"]
   [com.yahoo.platform.yui/yuicompressor "2.4.7"]]

  :npm
  {:dependencies
   [[source-map-support "0.3.1"]]}

  :clean-targets
  ^{:protect false}
  ["target"]

  :profiles
  {:dev {:source-paths ["src" "test" "dev"]
         :dependencies [[criterium "0.4.1"]]
         :plugins [[codox "0.8.13"]
                   [lein-npm "0.6.1"]
                   [com.jakemccrary/lein-test-refresh "0.10.0"]]}}

  :aliases
  {"build-cljs" ["run" "-m" "user/build"]
   "node-repl" ["run" "-m" "user/node-repl"]
   "test-clj" ["do" "clean," "run" "-m" "user/build," "test-refresh"]
   "test-cljs" ["run" "-m" "garden.tests/test-all"]})
