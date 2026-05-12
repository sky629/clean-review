create table if not exists users (
    id uuid primary key,
    email varchar(320) not null unique,
    name varchar(200) not null,
    profile_image_url text,
    role varchar(30) not null,
    status varchar(30) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists social_accounts (
    id uuid primary key,
    user_id uuid not null references users(id),
    provider varchar(50) not null,
    provider_user_id varchar(200) not null,
    email varchar(320) not null,
    display_name varchar(200),
    profile_image_url text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (provider, provider_user_id)
);

