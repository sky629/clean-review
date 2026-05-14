alter table retry_jobs
    add column if not exists consumer_name varchar(120) not null default 'review-analysis-worker';

create unique index if not exists ux_retry_jobs_original_event_consumer
    on retry_jobs(original_event_id, consumer_name);

create unique index if not exists ux_dead_letter_events_event_consumer
    on dead_letter_events(event_id, consumer_name);
