(defproject garden "1.2.7-SNAPSHOT"
  :description "Generate CSS from Clojure/Cljs data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :global-vars {*warn-on-reflection* true}

  :dependencies                  [[org.clojure/clojure "1.7.0" :scope "provided"]
                                  [org.clojure/clojurescript "1.7.28" :scope "provided"]]

  :plugins                       [[cider/cider-nrepl "0.9.1"] ;;required for cider-0.9.1
                                  [codox "0.8.13"]]

  :clean-targets
  ^{:protect false}              ["target"]

  :profiles
  {:dev {:source-paths           ["src" "test" "dev"]
         :dependencies           [[com.yahoo.platform.yui/yuicompressor "2.4.7"]
                                  [criterium "0.4.1"]]}}

  :aliases
  {"node-repl"                   ["run" "-m" "user/node-repl"]
   "test-all"                    ["run" "-m" "garden.tests/test-all"]})
