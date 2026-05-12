create table if not exists notification_channels (
    id uuid primary key,
    user_id uuid,
    channel varchar(50) not null,
    recipient varchar(300) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (channel, recipient)
);

create table if not exists notification_deliveries (
    id uuid primary key,
    notification_type varchar(100) not null,
    target_id uuid,
    source_event_id varchar(120) not null,
    channel varchar(50) not null,
    recipient varchar(300) not null,
    status varchar(30) not null,
    message text,
    sent_at timestamptz,
    failure_code varchar(100),
    failure_message text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (source_event_id, channel, recipient)
);

