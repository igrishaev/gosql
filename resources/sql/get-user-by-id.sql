

{% query getusersbyid %}

select {{ fields | join:", " }}
from users u
{% if foobar %}
  join profiles p on p.user_id = u.id
{% endif %}
where id = {{ id|param }}
  and name = {{ name|param }}

{% endquery %}



{% query get-user-by-id :one %}

select * from users
where id = {% param id %}
where id = {{ id|param }}
limit 1

{% endquery %}
