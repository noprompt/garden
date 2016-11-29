(defproject garden "2.0.0-alpha1"
  :description "Generate CSS from Clojure/Cljs data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"

  :global-vars {*warn-on-reflection* true}

  :dependencies
  [[org.clojure/clojure "1.9.0-alpha13" :scope "provided"]
   [org.clojure/clojurescript "1.9.293" :scope "provided"]
   [garden/garden-color "1.0.0"]
   [garden/garden-units "1.0.0"]]

  :npm
  {:dependencies [[source-map-support "0.4.0"]]}

  :clean-targets
  ^{:protect false}  ["target"]

  :profiles
  {:dev
   {:dependencies [[criterium "0.4.3"]
                   [hiccup "1.0.5"]]
    :jvm-opts ["-Dclojure.spec.compile-asserts=true"
               "-Dclojure.spec.check-asserts=true"]
    :plugins  [[codox "0.9.1"]
               [lein-npm "0.6.1"]
               [com.jakemccrary/lein-test-refresh "0.17.0"]]
    :source-paths ["src" "test" "dev"]}}

  :aliases
  {"build-cljs"
   ["run" "-m" "user/build"]

   "test-clj"
   ["do"
    ["clean"]
    ["test"]]

   "test-cljs"
   ["do"
    ["build-cljs"]
    ["run" "-m" "garden.tests/test-all"]]

   "test-cljc"
   ["do"
    ["test-clj"]
    ["test-cljs"]]

   "node-repl"
   ["run" "-m" "user/node-repl"]})
