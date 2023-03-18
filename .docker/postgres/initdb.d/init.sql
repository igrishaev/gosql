
create table items (
    id          serial primary key,
    sku         text not null unique,
    price       integer,
    title       text,
    description text
    "group-id"  integer
);

insert into items (sku, price, title, description)
values
('XXX1', 11, 'test1', 'Test Item 1'),
('XXX2', 22, 'test2', 'Test Item 2'),
('XXX3', 33, 'test3', 'Test Item 3');
