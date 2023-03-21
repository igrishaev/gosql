(ns kek.core-test
  (:require
   [next.jdbc :as jdbc]
   [clojure.test :refer [deftest is use-fixtures]]
   [kek.core :as kek]))


(def db-spec
  {:dbtype "sqlite" :dbname ":memory:"})


(def ^:dynamic *conn* nil)


(use-fixtures :each
  (fn [t]
    (let [db (jdbc/get-datasource db-spec)]
      (jdbc/on-connection [conn db]
        (jdbc/execute! conn ["create table items (sku text unique, title text, price integer, \"group-id\" integer)"])
        (jdbc/execute! conn ["insert into items (sku, title, price, \"group-id\")
                              values ('x1', 'Item 1', 10, 1),
                                     ('x2', 'Item 2', 20, 2),
                                     ('x3', 'Item 3', 30, 3);"])
        (binding [*conn* conn]
          (t))))))


(def funcs
  (kek/from-resource "queries.sql"))


(deftest test-load-resutl
  (is (vector? funcs)))


(deftest test-get-items-by-ids
  (let [items
        (get-all-items *conn*)]
    (is (= 3 (count items)))
    (is (= {:sku "x1"
            :title "Item 1"
            :price 10
            :group-id 1}
           (first items)))))


(deftest test-in-missing
  (is (thrown-with-msg?
          Exception
          #"parameter `ids` is not set in the context"
        (get-items-by-ids *conn* {}))))


(deftest test-param-tag

  (let [result
        (test-limit *conn* {:limit 42})]
    (is (= result [{:one 1}])))

  (is (thrown-with-msg?
          Exception
          #"parameter `limit` is not set in the context"
        (test-limit *conn* {:foo 42}))))


(deftest test-insert-item
  (let [item
        (insert-item *conn* {:fields {:sku "aaa"
                                      :price 999
                                      :title "TEST"}})]
    (is (= {:sku "aaa"
            :title "TEST"
            :price 999
            :group-id nil}
           item))))


(deftest test-insert-quoting
  (let [item
        (insert-item *conn* {:fields {:sku "abc3"
                                      :group-id 1
                                      :price 2}})]
    (is (= {:sku "abc3"
            :title nil
            :price 2
            :group-id 1}
           item))))


(deftest test-upsert-multi
  (let [item
        (upsert-items *conn* {:conflict [:sku]
                              :rows [{:price 1
                                      :title "item1"
                                      :sku "x1"
                                      :group-id 1}
                                     {:price 2
                                      :sku "foo2a"
                                      :group-id 2
                                      :title "item2"}
                                     {:title "item3"
                                      :price 3
                                      :group-id 3
                                      :sku "foo3a"}]})]

    (is (= '({:sku "foo2a" :title "item2" :price 2 :group-id 2}
             {:sku "foo3a" :title "item3" :price 3 :group-id 3}
             {:sku "x1" :title "item1" :price 1 :group-id 1})

           (sort-by :sku item)))))


(deftest test-pass-table
  (let [item
        (select-item-pass-table *conn*
                                {:table :items
                                 :sku "x3"})]

    (is (= {:sku "x3"
            :title "Item 3"
            :price 30
            :group-id 3}
           item))))
