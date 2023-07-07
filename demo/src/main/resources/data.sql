INSERT INTO demo_user (id, username, enabled, version)
VALUES (1, 'Demo Admin', true, 1),
       (2, 'Demo Editor', true, 1),
       (3, 'Demo Readonly', true, 1),
       (4, 'Demo Disabled', false, 1);

INSERT INTO demo_post (id, post_time, author_id, text, category, approved)
VALUES (1, NOW(), 2, 'Very first post', null, true),
       (2, NOW(), 4, 'Post by disabled user', 'DRAMA', false),
       (3, NOW(), 1, 'Tagless post', null, false);

INSERT INTO tag (id, text)
VALUES (1, 'First Tag'),
       (2, 'Second Tag'),
       (3, 'Third Tag'),
       (4, 'Fourth Tag');

INSERT INTO post_tags (post_id, tag_id)
VALUES (1, 1),
       (2, 1),
       (2, 2);
