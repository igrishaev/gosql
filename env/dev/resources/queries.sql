

{% sql/query get-all-items %}

    select * from items order by sku

{% sql/endquery %}


{% sql/query get-items-by-ids
    :doc "Query all the items by a list of ids." %}

    select * from items
    where id in {% sql/vals ids %}

{% sql/endquery %}


{% sql/query test-limit %}

    select 1 as one
    limit {% sql/? limit %}

{% sql/endquery %}


{% sql/query insert-item :one %}

    insert into items ({% sql/cols fields %})
    values ({% sql/vals fields %})
    returning *

{% sql/endquery %}


{% sql/query upsert-item :one %}

    insert into items ({% sql/cols fields %})
    values ({% sql/vals fields %})
    on conflict (sku) do update set {% sql/excluded fields %}
    returning *

{% sql/endquery %}


{% sql/query upsert-items %}

    insert into items ({% sql/cols* rows %})
    values {% sql/vals* rows %}
    on conflict ({% sql/cols conflict %}) do update
    set {% sql/excluded* rows %}
    returning *

{% sql/endquery %}


{% sql/query update-item-by-sku :1 %}

update items
set {% sql/set fields %}
where sku = {% sql/? sku %}
returning *

{% sql/endquery %}



{% sql/query upsert-items-array %}

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

{% sql/endquery %}



{% sql/query select-item-pass-table :1 %}

    select * from {% sql/quote table mysql %}
    where sku = {% sql/? sku %}
    limit 1

{% sql/endquery %}


{% sql/query go-get-item-by-sku :1 %}

    select * from items
    where sku = {% sql/? sku %}

{% sql/endquery %}



{% sql/query fn-test-arglists :1
   :doc " A docstring for the function.  " %}

    select {% sql/cols cols %} from {% sql/quote table %}
    {% if sku %}
    where sku = {% sql/? sku %}
    {% elif title %}
    where title = {% sql/? title %}
    {% endif %}

{% sql/endquery %}


{% sql/query fn-test-delete-count :count %}

    delete from items where sku in ({% sql/vals sku-list %})

{% sql/endquery %}


{% sql/query get-items-qualified-maps :1 %}

    select * from items
    where sku = 'x1' limit 1

{% sql/endquery %}


{% sql/query fn-with-raw-tag %}

    select * from items
    where {% sql/raw where %}

{% sql/endquery %}
