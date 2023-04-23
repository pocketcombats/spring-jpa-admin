INSERT INTO demo_user (id, username, enabled, version)
VALUES (1, 'Demo Admin', true, 1),
       (2, 'Demo Editor', true, 1),
       (3, 'Demo Readonly', true, 1),
       (4, 'Demo Disabled', false, 1);

INSERT INTO demo_post (id, post_time, author_id, text, approved)
VALUES (1, NOW(), 2, 'Very first post', true);
