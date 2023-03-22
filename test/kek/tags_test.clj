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


(deftest test-quote

  (is (= "\"foo-bar\""
         (kek/quote-handler ["col"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (kek/quote-handler ["col" "ansi"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (kek/quote-handler ["col" "pg"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (kek/quote-handler ["col" "sqlite"] {:col :foo-bar})))

  (is (= "`foo-bar`"
         (kek/quote-handler ["col" "mysql"] {:col :foo-bar})))

  (is (= "[foo-bar]"
         (kek/quote-handler ["col" "mssql"] {:col :foo-bar})))

  (is (= "foo-bar"
         (kek/quote-handler ["col" "dunno"] {:col :foo-bar})))





  )
