(ns garden.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [garden.color-test]))

(doo-tests 'garden.color-test)
