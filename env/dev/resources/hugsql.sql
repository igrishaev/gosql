
-- :name hug-get-item-by-sku :? :1
select * from items
where sku = :sku

-- :name hug-get-items-by-sku-list :?
select * from items
where sku in (:v*:sku-list)
