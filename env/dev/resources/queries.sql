

{% query get-all-items :as-unqualified-maps %}
select * from items order by sku
{% endquery %}



{% query get-items-by-ids
    :doc "Query all the items by a list of ids."
    :as-unqualified-maps
%}

select * from items
where id in {% sql/in ids %}

{% endquery %}


{% query test-limit :as-unqualified-maps %}

select 1 as one
limit {% sql/? limit %}

{% endquery %}


{% query insert-item
    :as-unqualified-maps
    :one
%}

insert into items {% sql/columns fields %}
values {% sql/values fields %}
returning *

{% endquery %}


{% query upsert-item :one :as-unqualified-maps %}

insert into items {% sql/columns fields %}
values {% sql/values fields %}
on conflict (sku) do update set {% sql/excluded fields %}
returning *

{% endquery %}


{% query upsert-items :as-unqualified-maps %}

insert into items {% sql/columns* rows %}
values {% sql/values* rows %}
on conflict {% sql/columns conflict %} do update
set {% sql/excluded* rows %}
returning *

{% endquery %}


{% query update-item-by-sku :1 :as-unqualified-maps %}

update items
set {% sql/set fields %}
where sku = {% sql/? sku %}
returning *

{% endquery %}



{% query upsert-items-array
    :as-unqualified-maps %}

insert into items {% sql/columns header %}
values {% sql/values* rows %}
on conflict (sku) do update
set {% sql/excluded header %}
returning *

{% endquery %}



{% query select-item-pass-table :1 :as-unqualified-maps %}

select * from {% sql/quote table mysql %}
where sku = {% sql/? sku %}
limit 1

{% endquery %}


{% query go-get-item-by-sku :1 :as-unqualified-maps %}

select * from items
where sku = {% sql/? sku %}

{% endquery %}


{% query go-get-items-by-sku-list :as-unqualified-maps %}

select * from items
where sku in {% sql/in sku-list %}

{% endquery %}
