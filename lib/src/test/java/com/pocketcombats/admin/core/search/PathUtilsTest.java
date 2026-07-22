package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestComment;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUtilsTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private <T> Root<T> root(Class<T> entityClass) {
        return jpa.em().getCriteriaBuilder().createQuery(entityClass).from(entityClass);
    }

    @Test
    void simplePathDoesNotCreateJoins() {
        Root<TestCategory> root = root(TestCategory.class);

        Path<?> path = PathUtils.resolve(root, "name");

        assertEquals(String.class, path.getJavaType());
        assertTrue(root.getJoins().isEmpty());
    }

    @Test
    void associationSegmentIsNavigatedWithLeftJoin() {
        Root<TestPost> root = root(TestPost.class);

        Path<?> path = PathUtils.resolve(root, "category.name");

        assertEquals(String.class, path.getJavaType());
        assertEquals(1, root.getJoins().size());
        Join<?, ?> join = root.getJoins().iterator().next();
        assertEquals("category", join.getAttribute().getName());
        assertEquals(JoinType.LEFT, join.getJoinType());
    }

    @Test
    void pathsOverTheSameAssociationReuseOneJoin() {
        // Several search factories resolve against the same root within one query;
        // each resolution must not stack another copy of the join
        Root<TestPost> root = root(TestPost.class);

        PathUtils.resolve(root, "category.name");
        PathUtils.resolve(root, "category.id");
        PathUtils.resolve(root, "category.name");

        assertEquals(1, root.getJoins().size());
    }

    @Test
    void multiHopPathChainsLeftJoins() {
        Root<TestComment> root = root(TestComment.class);

        Path<?> path = PathUtils.resolve(root, "post.category.name");

        assertEquals(String.class, path.getJavaType());
        assertEquals(1, root.getJoins().size());
        Join<?, ?> postJoin = root.getJoins().iterator().next();
        assertEquals("post", postJoin.getAttribute().getName());
        assertEquals(JoinType.LEFT, postJoin.getJoinType());
        assertEquals(1, postJoin.getJoins().size());
        Join<?, ?> categoryJoin = postJoin.getJoins().iterator().next();
        assertEquals("category", categoryJoin.getAttribute().getName());
        assertEquals(JoinType.LEFT, categoryJoin.getJoinType());
    }
}
