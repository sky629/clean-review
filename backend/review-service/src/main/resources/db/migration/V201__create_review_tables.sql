create table if not exists categories (
    id uuid primary key,
    name varchar(120) not null,
    target_type varchar(30),
    parent_id uuid references categories(id),
    display_order int not null default 0,
    status varchar(30) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (name, target_type, parent_id)
);

create table if not exists review_targets (
    id uuid primary key,
    type varchar(30) not null,
    keyword varchar(200) not null,
    status varchar(30) not null,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_review_targets_created_by on review_targets(created_by);

create table if not exists collection_settings (
    id uuid primary key,
    target_id uuid not null references review_targets(id),
    source varchar(50) not null,
    keyword varchar(200) not null,
    schedule_cron varchar(120),
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (target_id, source, keyword)
);

create table if not exists collection_runs (
    id uuid primary key,
    target_id uuid not null references review_targets(id),
    source varchar(50) not null,
    keyword varchar(200) not null,
    idempotency_key varchar(300) not null unique,
    run_reason varchar(50) not null,
    window_from timestamptz not null,
    window_to timestamptz not null,
    max_reviews int not null,
    status varchar(30) not null,
    requested_at timestamptz not null,
    requested_by uuid,
    started_at timestamptz,
    completed_at timestamptz,
    failure_code varchar(100),
    failure_message text
);

create unique index if not exists ux_collection_runs_open_target_source
    on collection_runs(target_id, source)
    where status in ('REQUESTED', 'RUNNING');

create table if not exists reviews (
    id uuid primary key,
    target_id uuid not null references review_targets(id),
    collection_run_id uuid references collection_runs(id),
    source varchar(50) not null,
    source_review_id varchar(200),
    canonical_url text not null,
    canonical_url_hash varchar(128) not null,
    normalized_text_hash varchar(128) not null,
    author_hash varchar(128),
    title text,
    raw_text text not null,
    published_at timestamptz,
    collected_at timestamptz not null,
    status varchar(30) not null,
    admin_tags jsonb not null default '[]',
    admin_memo text,
    manual_quality_label varchar(50),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (source, canonical_url_hash)
);

create index if not exists idx_reviews_target_id on reviews(target_id);
create index if not exists idx_reviews_collection_run_id on reviews(collection_run_id);

create table if not exists review_images (
    id uuid primary key,
    review_id uuid not null references reviews(id),
    image_url text not null,
    image_url_hash varchar(128) not null unique,
    perceptual_hash varchar(128),
    width int,
    height int,
    collected_at timestamptz not null
);

create table if not exists review_image_groups (
    id uuid primary key,
    perceptual_hash varchar(128) not null,
    representative_image_id uuid references review_images(id),
    created_at timestamptz not null
);

create table if not exists review_image_group_members (
    group_id uuid not null references review_image_groups(id),
    image_id uuid not null references review_images(id),
    distance int not null,
    primary key (group_id, image_id)
);

create table if not exists review_analysis (
    id uuid primary key,
    review_id uuid not null references reviews(id),
    collection_run_id uuid references collection_runs(id),
    analyzer_version varchar(80) not null,
    model_provider varchar(80) not null,
    model_name varchar(120) not null,
    model_version varchar(120) not null,
    viral_score numeric(5, 2) not null,
    quality_score numeric(5, 2) not null,
    is_suspicious boolean not null,
    useful_for_report boolean not null,
    summary text not null,
    detected_patterns jsonb not null default '[]',
    evidence jsonb not null default '[]',
    status varchar(30) not null,
    analyzed_at timestamptz not null,
    unique (review_id, analyzer_version, model_provider, model_name, model_version)
);

create table if not exists review_reports (
    id uuid primary key,
    target_id uuid not null references review_targets(id),
    collection_run_id uuid not null references collection_runs(id),
    analyzer_version varchar(80) not null,
    model_provider varchar(80) not null,
    model_name varchar(120) not null,
    model_version varchar(120) not null,
    viral_contamination_score double precision not null,
    trust_score double precision not null,
    summary text not null,
    pros jsonb not null default '[]',
    cons jsonb not null default '[]',
    evidence_review_ids jsonb not null default '[]',
    report_hash varchar(128) not null,
    created_at timestamptz not null,
    unique (target_id, collection_run_id, analyzer_version, model_version)
);
