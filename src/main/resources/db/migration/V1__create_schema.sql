create table orders (
    id          uuid        not null primary key,
    product_code varchar(64) not null,
    quantity    integer     not null check (quantity > 0),
    status      varchar(16) not null check (status in ('PLACED')),
    placed_at   timestamptz not null
);

create table inventory_reservations (
    id            uuid        not null primary key,
    order_id      uuid        not null,
    product_code  varchar(255) not null,
    quantity      integer     not null check (quantity > 0),
    reserved_at   timestamptz not null,
    constraint uk_inventory_reservations_order_id unique (order_id)
);

create table shipments (
    id           uuid        not null primary key,
    order_id     uuid        not null,
    prepared_at  timestamptz not null,
    constraint uk_shipments_order_id unique (order_id)
);
