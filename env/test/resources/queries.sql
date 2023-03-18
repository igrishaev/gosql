
{% query get-items-by-ids
    :doc "Query all the items by a list of ids."
    :as-unqualified-maps
%}

select * from items
where id in {% IN ids %}

{% endquery %}
