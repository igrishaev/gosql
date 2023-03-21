(ns kek.tags-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [kek.core :as kek]))


(deftest test-?

  (kek/with-params
    (kek/?-handler ["foo"] {:foo 1})
    (is (= [1] (vec kek/*params*))))

  (kek/with-params
    (is (thrown-with-msg?
            Exception
            #"parameter `abc` is not set in the context"
          (kek/?-handler ["abc"] {:foo 1})))))
