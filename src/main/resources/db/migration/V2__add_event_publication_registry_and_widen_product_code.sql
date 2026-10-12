-- Spring Modulith's JPA event publication registry (spring-modulith-events-jpa, DefaultJpaEventPublication).
-- Schema matches the project's documented Postgres DDL for spring-modulith-events-jdbc 2.1.x; Hibernate only
-- validates against it (spring.jpa.hibernate.ddl-auto: validate), it never creates it.
create table event_publication
(
    id                      uuid                     not null,
    listener_id             text                     not null,
    event_type              text                     not null,
    serialized_event        text                     not null,
    publication_date        timestamp with time zone not null,
    completion_date         timestamp with time zone,
    status                  text,
    completion_attempts     integer,
    last_resubmission_date  timestamp with time zone,
    primary key (id)
);

create index event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index event_publication_by_completion_date_idx on event_publication (completion_date);

-- Same shape, used only when spring.modulith.events.completion-mode=archive (our choice; see README).
create table event_publication_archive
(
    id                      uuid                     not null,
    listener_id             text                     not null,
    event_type              text                     not null,
    serialized_event        text                     not null,
    publication_date        timestamp with time zone not null,
    completion_date         timestamp with time zone,
    status                  text,
    completion_attempts     integer,
    last_resubmission_date  timestamp with time zone,
    primary key (id)
);

create index event_publication_archive_serialized_event_hash_idx on event_publication_archive using hash (serialized_event);
create index event_publication_archive_by_completion_date_idx on event_publication_archive (completion_date);

-- inventory_reservations.product_code was left at the Hibernate default (255) in V1; widen it to match
-- orders.product_code (64), which is the actual bound enforced by PlaceOrderCommand's @Size(max = 64).
alter table inventory_reservations
    alter column product_code type varchar(64);
