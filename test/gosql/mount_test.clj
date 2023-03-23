(ns gosql.mount-test
  (:import
   java.sql.Connection)
  (:require
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [gosql.core :as gosql]))


(def db-spec
  {:dbtype "sqlite" :dbname ":memory:"})



(mount/defstate ^{:dynamic true :on-reload :noop}
  ^Connection db
  :start
  (-> db-spec
      jdbc/get-datasource
      jdbc/get-connection)

  :stop
  nil)


(defmacro with-tx [[opt] & body]
  `(jdbc/with-transaction [tx# db ~opt]
     (binding [db tx#]
       ~@body)))



(use-fixtures :each
  (fn [t]
    (mount/start (var db))
    (t)
    (mount/stop (var db)))

  (fn [t]
    (jdbc/execute! db ["create table items (sku text unique, title text, price integer, \"group-id\" integer)"])
    (jdbc/execute! db ["insert into items (sku, title, price, \"group-id\")
                              values ('x1', 'Item 1', 10, 1),
                                     ('x2', 'Item 2', 20, 2),
                                     ('x3', 'Item 3', 30, 3);"])
    (t)))


(def funcs
  (gosql/from-resource "queries.sql"
                       {:db (var db)
                        :builder-fn jdbc.rs/as-unqualified-maps}))


(deftest test-global-db-ok
  (let [item
        (select-item-pass-table {:table :items
                                 :sku "x3"})]

    (is (= {:sku "x3"
            :title "Item 3"
            :price 30
            :group-id 3}
           item))))


(deftest test-global-transaction

  (with-tx [{:rollback-only true}]

    (fn-test-delete-count {:sku-list ["x1" "x3"]})

    (let [items
          (get-all-items)]
      (is (= ["x2"]
             (map :sku items)))))

  (let [items
        (get-all-items)]
    (is (= ["x1" "x2" "x3"]
           (map :sku items)))))



(deftest test-fn-meta
  (let [meta-map
        (-> select-item-pass-table var meta)]
    (is (= {:ns (the-ns 'gosql.mount-test)
            :name 'select-item-pass-table
            :column 1
            :arglists '([]
                        [{:as context :keys [sqlvec? sku table]}])
            :doc nil}

           (dissoc meta-map :file :line)))))
