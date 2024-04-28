package com.pocketcombats.admin.plugin.auth.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SpringJpaAdminUserRepository {

    private final EntityManager em;

    public SpringJpaAdminUserRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<SpringJpaAdminUser> findByUsername(String username) {
        List<SpringJpaAdminUser> users = em.createQuery(
                        "select u from SpringJpaAdminUser u join fetch u.authorities where u.lowerUsername = :lowerUsername",
                        SpringJpaAdminUser.class)
                .setParameter("lowerUsername", username.toLowerCase())
                .setMaxResults(1)
                .getResultList();
        if (users.isEmpty()) {
            return Optional.empty();
        } else {
            SpringJpaAdminUser user = users.get(0);
            em.detach(user);
            return Optional.of(user);
        }
    }
}
