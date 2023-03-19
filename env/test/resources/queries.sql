
{% query get-items-by-ids
    :doc "Query all the items by a list of ids."
    :as-unqualified-maps
%}

select * from items
where id in {% IN ids %}

{% endquery %}


{% query test-limit :as-unqualified-maps %}

select 1 as one
limit {% ? limit %}

{% endquery %}


{% query insert-item
    :as-unqualified-maps
    :one
%}

insert into items {% COLUMNS fields %}
values {% VALUES fields %}
returning *

{% endquery %}


{% query upsert-item :one :as-unqualified-maps %}

insert into items {% COLUMNS fields %}
values {% VALUES fields %}
on conflict (sku) do update set {% EXCLUDED fields %}
returning *

{% endquery %}


{% query upsert-items :as-unqualified-maps %}

insert into items {% COLUMNS* rows %}
values {% VALUES* rows %}
on conflict {% COLUMNS conflict %} do update
set {% EXCLUDED* rows %}
returning *

{% endquery %}



{% query upsert-items-array
    :as-unqualified-maps
%}

insert into items {% COLUMNS header %}
values {% VALUES* rows %}
on conflict (sku) do update
set {% EXCLUDED header %}
returning *

{% endquery %}
