(defproject garden "0.1.0-beta6"
  :description "Generate CSS from Clojure data structures."
  :url "https://github.com/noprompt/garden"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/math.combinatorics "0.0.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[criterium "0.4.1"]]}}
  :plugins [[codox "0.6.4"]])
