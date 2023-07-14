package com.pocketcombats.admin.conf;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@ConditionalOnProperty(name = "spring.jpa-admin.configure-security", havingValue = "true", matchIfMissing = true)
@EnableMethodSecurity(securedEnabled = true)
public class JpaAdminSecurityConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public SecurityFilterChain permitWebjars(HttpSecurity http) throws Exception {
        http.securityMatcher("/webjars/**")
                .authorizeHttpRequests(authorize -> authorize.requestMatchers("/webjars/**").permitAll());
        return http.build();
    }
}
