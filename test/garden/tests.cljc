(ns garden.tests
  (:require
    #?(:cljs [cljs.test :as t :refer-macros [is are deftest testing]]
       :clj  [clojure.test :as t :refer [is are deftest testing]])
    garden.color-test
    garden.units-test
    garden.util-test))

#?(:cljs
   (enable-console-print!))

#?(:cljs (def test-summary (atom nil)))

;; Added special case for printing ex-data of ExceptionInfo
#?(:cljs
  (defmethod t/report [::t/default :error] [m]
    (t/inc-report-counter! :error)
    (println "\nERROR in" (t/testing-vars-str m))
    (when (seq (:testing-contexts (t/get-current-env)))
      (println (t/testing-contexts-str)))
    (when-let [message (:message m)] (println message))
    (println "expected:" (pr-str (:expected m)))
    (print "  actual: ")
    (let [actual (:actual m)]
      (cond
        (instance? ExceptionInfo actual)
        (println (.-stack actual) "\n" (pr-str (ex-data actual)))
        (instance? js/Error actual)
        (println (.-stack actual))
        :else
        (prn actual)))))

(defn wrap-res [f]
  #?(:cljs (do (f) (clj->js @test-summary))
     :clj  (let [res (f)]
             (when (pos? (+ (:fail res) (:error res)))
               (System/exit 1)))))

(defn ^:export test-all []
  (wrap-res #(t/run-all-tests)))
