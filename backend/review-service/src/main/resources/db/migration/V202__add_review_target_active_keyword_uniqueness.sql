create unique index if not exists ux_review_targets_owner_type_keyword_active
    on review_targets(created_by, type, lower(trim(keyword)))
    where status <> 'DELETED';
