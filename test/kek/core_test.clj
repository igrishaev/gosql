(ns kek.core-test
  (:require
   [next.jdbc :as jdbc]
   [clojure.test :refer [deftest is]]
   [kek.core :as kek]))


(def db-spec
  {:dbtype "postgres"
   :dbname "test"
   :host "localhost"
   :port 25432
   :user "test"
   :password "test"})


(def funcs
  (kek/from-resource "queries.sql"))


(deftest test-func-count
  (is (= 2 (count funcs))))


(deftest test-get-by-sku

  (let [vars
        (kek/from-resource "queries.sql")

        db
        (jdbc/get-datasource db-spec)

        item1
        (get-item-by-sku db {:sku "XXX1"})

        item2
        (get-item-by-sku db {:sku "dunno"})

        item3
        (get-item-by-sku db {:sku nil})]

    (is (= {:id 1
            :sku "XXX1"
            :price 11
            :title "test1"
            :description "Test Item 1"}
           item1))

    (is (nil? item2))
    (is (nil? item3))))


(deftest test-fields-strings

  (let [db
        (jdbc/get-datasource db-spec)

        item
        (get-item-with-fields db {:sku "XXX2"
                                  :fields ["sku" "title"]})]

    (is (= {:sku "XXX2"
            :title "test2"
            :answer 42}
           item))))


(deftest test-fields-keywords

  (let [db
        (jdbc/get-datasource db-spec)

        item
        (get-item-with-fields db {:sku "XXX2"
                                  :fields [:sku :title]})]

    (is (= {:sku "XXX2"
            :title "test2"
            :answer 42}
           item))))


(deftest test-fields-map

  (let [db
        (jdbc/get-datasource db-spec)

        item
        (get-item-with-fields db {:sku "XXX2"
                                  :fields {:sku 1 :title 2}})]

    (is (= {:sku "XXX2"
            :title "test2"
            :answer 42}
           item))))


(deftest test-update-item-by-sku

  (let [db
        (jdbc/get-datasource db-spec)

        item
        (update-item-by-sku db {:debug? true
                                :sku "XXX2"
                                :values {:title "aaa"
                                         :price 199}})]

    (is (= {:sku "XXX2"
            :price 199
            :title "aaa"
            :description "Test Item 2"}

           (dissoc item :id)))))


(deftest test-upsert-item

  (let [db
        (jdbc/get-datasource db-spec)

        item
        (upsert-item db {:values {:sku "XXX2"
                                  :title "NEW"
                                  :price 999}})]

    (is (= {:sku "XXX2"
            :price 999
            :title "NEW"
            :description "Test Item 2"}
           (dissoc item :id)))))


(deftest test-get-all-items

  (let [db
        (jdbc/get-datasource db-spec)

        items
        (get-all-items db {:fields [:sku :price]})]

    (is (= [{:sku "XXX1" :price 11}
            {:sku "XXX3" :price 33}
            {:sku "XXX2" :price 999}]
           items))))


(deftest test-delete-all-items

  (let [db
        (jdbc/get-datasource db-spec)

        result
        (delete-all-items db)]

    (is (= 0 result))))


(deftest test-items-by-ids

  (let [db
        (jdbc/get-datasource db-spec)

        result
        (get-items-by-ids db {:table :items
                              :ids [1 2 3 4 5]})]

    (is (= [] result))





    )

  )
