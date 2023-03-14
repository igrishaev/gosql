

{% query getusersbyid %}

select {{ fields | join:", " }}
from users u
{% if foobar %}
  join profiles p on p.user_id = u.id
{% endif %}
where id = {{ id|param }}
  and name = {{ name|param }}

{% endquery %}



{% query other_query %}

select * from users
where id = {% param id %}
where id = {{ id|param }}
limit 1

{% endquery %}
