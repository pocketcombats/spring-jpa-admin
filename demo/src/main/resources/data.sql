INSERT INTO demo_user (id, username, enabled, version)
VALUES (1, 'Demo Admin', true, 1),
       (2, 'Demo Editor', true, 1),
       (3, 'Demo Readonly', true, 1),
       (4, 'Demo Disabled', false, 1);

INSERT INTO demo_post (id, post_time, author_id, text, category, approved, seo_title, seo_description)
VALUES (1, '2023-06-18T10:10Z', 2, 'The very first post, with rather long text potentially breaking the list view.', null, true,
        'The Very First Post', 'An introductory post demonstrating the Spring JPA Admin.'),
       (2, '2023-06-18T10:32Z', 4, 'Post by disabled user', 'DRAMA', false, null, null),
       (3, '2023-06-19T00:00Z', 1, 'Tagless post', null, false, null, null),
       (4, '2023-06-19T00:10Z', 2, 'Second post by Demo Editor', null, false,
        'Second Post by Demo Editor', 'Follow-up thoughts from the Demo Editor on Spring JPA Admin.');

INSERT INTO tag (id, text)
VALUES (1, 'First Tag'),
       (2, 'Second Tag'),
       (3, 'Third Tag'),
       (4, 'Fourth Tag');

INSERT INTO post_tags (post_id, tag_id)
VALUES (1, 1),
       (2, 1),
       (2, 2);

INSERT INTO demo_comment (id, post_id, post_time, author_id, text)
VALUES (1, 1, '2023-06-18T11:00Z', 1, 'Great first post!'),
       (2, 1, '2023-06-18T12:15Z', 2, 'Thanks for reading, glad you liked it.'),
       (3, 4, '2023-06-19T00:45Z', 3, 'Looking forward to the next one.');

-- Restart the Hibernate-managed id sequences.
-- demo_user, tag and demo_post_reaction ids are assigned manually (no @GeneratedValue).
ALTER SEQUENCE demo_post_seq RESTART WITH 100;
ALTER SEQUENCE demo_comment_seq RESTART WITH 100;
