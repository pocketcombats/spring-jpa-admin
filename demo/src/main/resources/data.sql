INSERT INTO demo_user (id, username, enabled, version)
VALUES (1, 'Demo Admin', true, 1),
       (2, 'Demo Editor', true, 1),
       (3, 'Demo Readonly', true, 1),
       (4, 'Demo Disabled', false, 1);

INSERT INTO demo_post (id, post_time, author_id, text, category, approved)
VALUES (1, '2023-06-18T10:10Z', 2, 'The very first post, with rather long text potentially breaking the list view.', null, true),
       (2, '2023-06-18T10:32Z', 4, 'Post by disabled user', 'DRAMA', false),
       (3, '2023-06-19T00:00Z', 1, 'Tagless post', null, false);

INSERT INTO tag (id, text)
VALUES (1, 'First Tag'),
       (2, 'Second Tag'),
       (3, 'Third Tag'),
       (4, 'Fourth Tag');

INSERT INTO post_tags (post_id, tag_id)
VALUES (1, 1),
       (2, 1),
       (2, 2);
