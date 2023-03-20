(ns kek.core-test
  (:require

   ;; bench
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter]
   [next.jdbc.result-set :as rs]
   [honey.sql :as honey]

   [next.jdbc :as jdbc]
   [clojure.test :refer [deftest is use-fixtures]]
   [kek.core :as kek]))


(def db-spec
  {:dbtype "postgres"
   :dbname "test"
   :host "localhost"
   :port 25432
   :user "test"
   :password "test"})


(hugsql/def-db-fns "hugsql.sql")

(def ^:dynamic *db* nil)



#_
(time
 (dotimes [_ 500]
   (let [db (jdbc/get-datasource db-spec)]
     (go-get-item-by-sku db {:sku "aaa"}))))


#_
(time
 (let [db (jdbc/get-datasource db-spec)]
   (dotimes [_ 500]
     (hug-get-items-by-sku-list db {:sku-list ["aaa" "bbb" "ccc"]}))))


#_
(defn honey-get-items-by-sku-list [db sku-list]
  (jdbc/execute! db
                 (honey/format
                  {:select [:*]
                   :from [:items]
                   :where [:in :sku sku-list]})
                 {:builder-fn rs/as-unqualified-maps}))


#_
(time
 (let [db (jdbc/get-datasource db-spec)]
   (dotimes [_ 500]
     (honey-get-items-by-sku-list db ["aaa" "bbb" "ccc"]))))




(use-fixtures :once
  (fn [t]
    (binding [*db*
              (jdbc/get-datasource db-spec)]
      (t))))

#_
(def -ds
  p(jdbc/get-datasource db-spec))


(def funcs
  (kek/from-resource "queries.sql" #_{:db -ds}))


(deftest test-func-count
  (is (= 2 (count funcs))))


(deftest test-get-items-by-ids
  (let [users
        (get-items-by-ids *db* {:ids [1 2 3]})]
    (is (= 1 users))))


(deftest test-in-missing
  (is (thrown-with-msg?
          Exception
          #"parameter `ids` is not set in the context"
        (get-items-by-ids *db* {}))))


(deftest test-param-tag

  (let [result
        (test-limit *db* {:limit 42})]
    (is (= result [{:one 1}])))

  (is (thrown-with-msg?
          Exception
          #"parameter `limit` is not set in the context"
        (test-limit *db* {:foo 42}))))


(deftest test-insert-item
  (let [item
        (insert-item *db* {:fields {:sku "aaa"
                                    :price 999
                                    :title "TEST"}})]
    (is (= 1 item))))


(deftest test-insert-quoting
  (let [item
        (insert-item *db* {:fields {:sku "abc3"
                                    :group-id 1
                                    :price 2}})]
    (is (= 1 item))))


(deftest test-upsert-multi
  (let [item
        (upsert-items *db* {:conflict [:sku]
                            :rows [{:price 1
                                    :title "item1"
                                    :sku "foo1a"
                                    :group-id 1}
                                   {:price 2
                                    :sku "foo2a"
                                    :group-id 2
                                    :title "item2"}
                                   {:title "item3"
                                    :price 3
                                    :group-id 3
                                    :sku "foo3a"}]})]
    (is (= 1 item))))


(deftest test-pass-table
  (let [item
        (select-item-pass-table *db*
                                {:table :items
                                 :sku "aaa"})]
    (is (= 1 item))))
