

{% query get-all-items :as-unqualified-maps %}
select * from items order by sku
{% endquery %}



{% query get-items-by-ids
    :doc "Query all the items by a list of ids."
    :as-unqualified-maps %}

select * from items
where id in {% sql/vals ids %}

{% endquery %}


{% query test-limit :as-unqualified-maps %}

select 1 as one
limit {% sql/? limit %}

{% endquery %}


{% query insert-item
    :as-unqualified-maps
    :one %}

insert into items ({% sql/cols fields %})
values ({% sql/vals fields %})
returning *

{% endquery %}


{% query upsert-item :one :as-unqualified-maps %}

insert into items ({% sql/cols fields %})
values ({% sql/vals fields %})
on conflict (sku) do update set {% sql/excluded fields %}
returning *

{% endquery %}


{% query upsert-items :as-unqualified-maps %}

insert into items ({% sql/cols* rows %})
values {% sql/vals* rows %}
on conflict ({% sql/cols conflict %}) do update
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

insert into items ({% sql/cols header %})
values {% sql/vals* rows %}
on conflict (sku) do update
set {% sql/excluded header %}
returning
{% if return %}
  {% sql/cols return %}
{% else %}
  *
{% endif %}
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



{% query fn-test-arglists :1 :as-unqualified-maps
   :doc " A docstring for the function.  "
 %}

select {% sql/cols cols %} from {% sql/quote table %}
{% if sku %}
where sku = {% sql/? sku %}
{% elif title %}
where title = {% sql/? title %}
{% endif %}

{% endquery %}


{% query fn-test-delete-count :count %}

delete from items where sku in ({% sql/vals sku-list %})

{% endquery %}


{% query get-items-qualified-maps :1 %}

select * from items
where sku = 'x1' limit 1

{% endquery %}
