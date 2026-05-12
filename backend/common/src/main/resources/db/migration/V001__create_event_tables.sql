create table if not exists idempotency_records (
    id uuid primary key,
    scope varchar(100) not null,
    idempotency_key varchar(300) not null,
    request_hash varchar(128) not null,
    status varchar(30) not null,
    locked_by varchar(100),
    locked_until timestamptz,
    response_reference varchar(300),
    failure_code varchar(100),
    failure_message text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    failed_at timestamptz,
    unique (scope, idempotency_key)
);

create table if not exists processed_events (
    id uuid primary key,
    event_id varchar(120) not null,
    consumer_name varchar(120) not null,
    event_type varchar(160) not null,
    processed_at timestamptz not null,
    result_status varchar(30) not null,
    unique (event_id, consumer_name)
);

create table if not exists retry_jobs (
    id uuid primary key,
    topic varchar(200) not null,
    event_type varchar(160) not null,
    original_event_id varchar(120) not null,
    idempotency_key varchar(300) not null,
    correlation_id varchar(120) not null,
    payload jsonb not null,
    attempt int not null,
    max_attempts int not null,
    next_attempt_at timestamptz not null,
    last_error_code varchar(100),
    last_error_message text,
    status varchar(30) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_retry_jobs_due on retry_jobs(status, next_attempt_at);

create table if not exists dead_letter_events (
    id uuid primary key,
    source_topic varchar(200) not null,
    event_id varchar(120) not null,
    event_type varchar(160) not null,
    consumer_name varchar(120) not null,
    payload jsonb not null,
    error_code varchar(100),
    error_message text,
    failed_at timestamptz not null
);

