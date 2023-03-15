

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


{% query get-foo-by-this %}

insert into messages
  (entry_id, subscription_id, date_published_at)
select e.id, {{ subscription-id|? }}, e.date_published_at
  from entries e

set foo = '{{ data|json }}'

where e.feed_id = {{ feed-id|? }}
where e.feed_id = {% ? feed-id %}
order by created_at desc
limit {{ limit|? }}
on conflict (entry_id, subscription_id)
do nothing
returning *

{% endquery %}
