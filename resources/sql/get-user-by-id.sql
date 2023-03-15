

{% query get-user-by-id %}

{% doc %}
sdfsdf sdf sdf sdf
sf sdf sdf sf  sdf sdf sdf
sd fsdf sf sdf sdf
sdf sdf sdfsdf
{% enddoc %}

select {{ fields | join:", " }}
from users u
{% if foobar %}
  join profiles p on p.user_id = u.id
{% endif %}
where id = {{ id|? }}
  and name = {{ name|? }}

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
where e.feed_id = {{ feed-id|? }}
where e.feed_id = {% ? feed-id %}
order by created_at desc
limit {{ limit|? }}
on conflict (entry_id, subscription_id)
do nothing
returning *

{% endquery %}


{% query update-unread-for-subscription %}

update subscriptions
set unread_count = sub.unread
from (
    select count(m.id) as unread
    from messages m
    where
        m.subscription_id = {{ subscription-id }}
        and not is_read
) as sub
where id = {{ subscription-id }}
returning *

{% endquery %}
