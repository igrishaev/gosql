

{% query get-user-by-id
    :doc "
sdfs sdf sdf sdfs dfs fsdfs  sfddsfs
sdfsfdsdf
sdfsfdsfsdf
sdfsfsdfsdfdfsfsdf
"
    :as-unqualified-maps
    :1


 %}

select * from users
where id = {{ id|? }}
limit {{  limit}}

{% endquery %}
