

[hugsql.core :as hugsql]
[hugsql.adapter.next-jdbc :as next-adapter]
[next.jdbc.result-set :as rs]
[honey.sql :as honey]

(hugsql/def-db-fns "hugsql.sql")


(def db-spec
  {:dbtype "sqlite" :dbname ":memory:"})


(time
 (dotimes [_ 500]
   (let [db (jdbc/get-datasource db-spec)]
     (go-get-item-by-sku db {:sku "aaa"}))))


(time
 (let [db (jdbc/get-datasource db-spec)]
   (dotimes [_ 500]
     (hug-get-items-by-sku-list db {:sku-list ["aaa" "bbb" "ccc"]}))))


(defn honey-get-items-by-sku-list [db sku-list]
  (jdbc/execute! db
                 (honey/format
                  {:select [:*]
                   :from [:items]
                   :where [:in :sku sku-list]})
                 {:builder-fn rs/as-unqualified-maps}))


(time
 (let [db (jdbc/get-datasource db-spec)]
   (dotimes [_ 500]
     (honey-get-items-by-sku-list db ["aaa" "bbb" "ccc"]))))
