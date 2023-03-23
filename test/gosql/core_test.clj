(ns gosql.core-test
  (:require
   gosql.fake-ns
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [gosql.core :as gosql]))


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
  (gosql/from-resource
   "queries.sql"
   {:builder-fn jdbc.rs/as-unqualified-maps}))


(def funcs-foreign
  (gosql/from-resource "queries.sql"
                       {:ns 'gosql.fake-ns
                        :builder-fn jdbc.rs/as-unqualified-maps}))


(deftest test-load-resutl
  (is (vector? funcs)))


(deftest test-get-all-items
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


(deftest test-update-item-by-sku
  (let [item
        (update-item-by-sku *conn*
                            {:sku "x2"
                             :fields {:price 999
                                      :group-id 99}})]
    (is (= {:sku "x2"
            :title "Item 2"
            :price 999
            :group-id 99}
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



(deftest test-upsert-multi-sqlvec
  (let [result
        (upsert-items *conn* {:sqlvec? true
                              :conflict [:sku]
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
    (is (= ["insert into items (price, title, sku, \"group-id\")
    values (?, ?, ?, ?), (?, ?, ?, ?), (?, ?, ?, ?)
    on conflict (sku) do update
    set price = EXCLUDED.price, title = EXCLUDED.title, sku = EXCLUDED.sku, \"group-id\" = EXCLUDED.\"group-id\"
    returning *"
           1
           "item1"
           "x1"
           1
           2
           "item2"
           "foo2a"
           2
           3
           "item3"
           "foo3a"
           3]
           result))))


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


(deftest test-fn-test-arglists
  (let [item1
        (fn-test-arglists *conn* {:sku "x1"
                                  :table "items"
                                  :cols [:sku :title]})

        item2
        (fn-test-arglists *conn* {:title "Item 3"
                                  :table "items"
                                  :cols [:sku :title]})]

    (is (= {:sku "x1" :title "Item 1"}
           item1))

    (is (= {:sku "x3" :title "Item 3"}
           item2))))


(deftest test-fn-var-meta
  (let [var-meta
        (-> fn-test-arglists var meta)]

    (is (= {:ns (the-ns 'gosql.core-test)
            :name 'fn-test-arglists
            :column 1
            :arglists
            '([]
              [{:as context}]
              [db]
              [db {:as context :keys [print? sqlvec? cols sku table title]}])
            :doc "A docstring for the function."}

           (dissoc var-meta :file :line)))

    (is (int? (:line var-meta)))

    (is (str/ends-with? (:file var-meta)
                        "env/dev/resources/queries.sql"))))


(deftest test-upsert-items-array
  (let [matrix
        [[:sku :title :group-id]
         ["x1" "Item 1" 999]
         ["x5" "Item 5" 123]
         ["x9" "Item 9" 321]]

        items
        (upsert-items-array *conn*
                            {:header (first matrix)
                             :rows (rest matrix)
                             :return [:sku :group-id]})]

    (is (= [{:sku "x1" :group-id 999}
            {:sku "x5" :group-id 123}
            {:sku "x9" :group-id 321}]
           items))))


(deftest test-get-items-by-ids-foreign
  (let [items
        (gosql.fake-ns/get-all-items *conn*)]
    (is (= 3 (count items)))
    (is (= {:sku "x1"
            :title "Item 1"
            :price 10
            :group-id 1}
           (first items)))))


(deftest test-fn-delete-count
  (let [result
        (fn-test-delete-count *conn*
                              {:sku-list ["x1" "x3" "x5"]})]
    (is (= 2 result))))


#_
(deftest test-qualified-maps
  (let [item
        (get-items-qualified-maps *conn*)]
    (is (= {:items/sku "x1"
            :items/title "Item 1"
            :items/price 10
            :items/group-id 1}
           item))))


(deftest test-raw-clause
  (let [items
        (fn-with-raw-tag *conn*
                         {:where "sku in ('x1', 'x3')"})]

    (is (= ["x1" "x3"]
           (mapv :sku items)))))
