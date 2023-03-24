(ns gosql.tags-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [gosql.core :as gosql]))


(deftest test-?

  (gosql/with-params
    (gosql/?-handler ["foo"] {:foo 1})
    (is (= [1] (vec gosql/*params*))))

  (gosql/with-params
    (is (thrown-with-msg?
          Exception
          #"parameter `abc` is not set, query: `null`"
          (gosql/?-handler ["abc"] {:foo 1})))))


(deftest test-quote

  (is (= "\"foo-bar\""
         (gosql/quote-handler ["col"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (gosql/quote-handler ["col" "ansi"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (gosql/quote-handler ["col" "pg"] {:col :foo-bar})))

  (is (= "\"foo-bar\""
         (gosql/quote-handler ["col" "sqlite"] {:col :foo-bar})))

  (is (= "`foo-bar`"
         (gosql/quote-handler ["col" "mysql"] {:col :foo-bar})))

  (is (= "[foo-bar]"
         (gosql/quote-handler ["col" "mssql"] {:col :foo-bar})))

  (is (= "foo-bar"
         (gosql/quote-handler ["col" "dunno"] {:col :foo-bar})))





  )
