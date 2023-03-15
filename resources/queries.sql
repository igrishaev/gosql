

{% query get-user-by-id
    :doc "sdfsdfsdf sdfsdfdsf" %}

select * from users
where id = {{ id|? }}

{% endquery %}
