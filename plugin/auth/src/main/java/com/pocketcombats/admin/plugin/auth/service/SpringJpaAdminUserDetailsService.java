package com.pocketcombats.admin.plugin.auth.service;

import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUser;
import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class SpringJpaAdminUserDetailsService implements UserDetailsService {

    private final SpringJpaAdminUserRepository userRepository;

    public SpringJpaAdminUserDetailsService(SpringJpaAdminUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public SpringJpaAdminUser loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
