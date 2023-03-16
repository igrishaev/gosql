
{% query
    get-all-items
    :doc "Query all the items from the database."
    :as-unqualified-maps
%}

select
{% if fields %}
  {{ fields | FIELDS }}
{% else %}
  *
{% endif %}
from items

{% endquery %}



{% query get-item-by-sku
    :doc "Get a sinle item by its SKU name."
    :as-unqualified-maps
    :1 %}

select * from items
where sku = {{ sku|? }}

{% endquery %}



{% query get-item-with-fields
    :1 :as-unqualified-maps %}

select {{ fields | FIELDS }}, 42 as answer from items
where sku = {{ sku|? }}

{% endquery %}



{% query update-item-by-sku
    :1 :as-unqualified-maps %}

update items
set {{ values | SET }}
where sku = {% ? sku %}
returning *

{% endquery %}



{% query upsert-item
    :1 :as-unqualified-maps %}

insert into items ({{ values | FIELDS }})
values ({{ values | VALUES }})
on conflict (sku)
do update set {{ values | EXCLUDED }}
returning *

{% endquery %}


{% query upsert-item-multi
    :as-unqualified-maps %}

insert into items ({{ values | first | FIELDS }})
values {{ values | MVALUES }}
on conflict (sku)
do update set {{ values | first | EXCLUDED }}
returning *

{% endquery %}



{% query delete-all-items :count %}

delete from items

{% endquery %}
