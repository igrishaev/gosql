

{% query get-user-by-id
    :doc "sdfs sdf sdf sdfs dfs fsdfs  sfddsfs
    sdfsfdsdf
    sdfsfdsfsdf
    sdfsfsdfsdfdfsfsdf" %}

select * from users
where id = {{ id|? }}

{% endquery %}
