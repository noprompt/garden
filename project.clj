(defproject garden "1.0.0-SNAPSHOT"
  :description "Generate CSS from Clojure data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj" "target/generated-src/clj" "target/generated-src/cljs"]
  :test-paths ["test" "target/generated-test"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.7"]
                 [com.cemerick/clojurescript.test "0.0.4"]]
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated-src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated-src/cljs"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "target/generated-test"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "target/generated-test"
                   :rules :cljs}]}
  :cljsbuild {:builds [{:source-paths ["target/generated-src/cljs" "target/generated-test"]
                        :compiler {:output-to "target/cljs/testable.js"}
                        :optimizations :whitespace
                        :pretty-print true}]
              :test-commands {"unit-tests" ["phantomjs"  "runners/phantomjs.js" "target/cljs/testable.js"]}}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[criterium "0.4.1"]]}}
  :plugins [[codox "0.6.4"]
            [lein-cljsbuild "0.3.2"]
            [com.keminglabs/cljx "0.3.0"]])
