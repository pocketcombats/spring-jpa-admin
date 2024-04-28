package com.pocketcombats.admin.plugin.auth.service;

import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminAuthority;
import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUser;
import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUserAuthLog;
import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class SpringJpaAdminAuthService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringJpaAdminAuthService.class);

    private final SpringJpaAdminUserRepository userRepository;
    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;

    public SpringJpaAdminAuthService(
            SpringJpaAdminUserRepository userRepository,
            EntityManager em,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.em = em;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDefaultAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            SpringJpaAdminUser admin = new SpringJpaAdminUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setAuthorities(getDefaultAdminAuthorities());
            admin.setEnabled(true);
            em.persist(admin);
            LOG.warn("Created default admin user");
        }
    }

    protected Set<SpringJpaAdminAuthority> getDefaultAdminAuthorities() {
        return new HashSet<>(
                em.createQuery("select a from SpringJpaAdminAuthority a", SpringJpaAdminAuthority.class)
                        .getResultList()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void logAuthentication(Integer userId, HttpServletRequest request) {
        SpringJpaAdminUserAuthLog log = new SpringJpaAdminUserAuthLog();
        log.setUser(em.getReference(SpringJpaAdminUser.class, userId));
        log.setTimestamp(Instant.now());
        log.setAddress(StringUtils.abbreviate(RequestAddress.resolve(request), 50));
        log.setAgent(StringUtils.abbreviate(request.getHeader("User-Agent"), 125));
        em.persist(log);
    }
}
