package com.pocketcombats.admin.plugin.auth.conf;

import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUserRepository;
import com.pocketcombats.admin.plugin.auth.service.SpringJpaAdminAuthService;
import com.pocketcombats.admin.plugin.auth.service.SpringJpaAdminUserDetailsService;
import com.pocketcombats.admin.plugin.auth.web.AdminLoginController;
import com.pocketcombats.admin.plugin.auth.web.ChangePasswordController;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableConfigurationProperties(JpaAdminAuthProperties.class)
public class JpaAdminAuthAutoConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private final int passwordStrength;
    private final boolean createDefaultAdmin;

    public JpaAdminAuthAutoConfiguration(
            JpaAdminAuthProperties properties
    ) {
        this.passwordStrength = properties.passWordStrength();
        this.createDefaultAdmin = properties.createDefaultAdmin();
    }

    @Bean
    public AdminLoginController adminLoginController(SpringJpaAdminAuthService service) {
        return new AdminLoginController(service);
    }

    @Bean
    public ChangePasswordController changePasswordController(
            AdminModelFormService adminModelFormService,
            PasswordEncoder passwordEncoder
    ) {
        return new ChangePasswordController(adminModelFormService, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2B, passwordStrength);
    }

    @Bean
    public SpringJpaAdminUserRepository springJpaAdminUserRepository(EntityManager em) {
        return new SpringJpaAdminUserRepository(em);
    }

    @Bean
    public SpringJpaAdminAuthService springJpaAdminAuthService(
            SpringJpaAdminUserRepository userRepository,
            EntityManager em,
            PasswordEncoder passwordEncoder
    ) {
        return new SpringJpaAdminAuthService(userRepository, em, passwordEncoder);
    }

    public UsernamePasswordAuthenticationFilter authenticationFilter() {
        return new CustomUsernamePasswordAuthenticationFilter(createDefaultAdmin);
    }

    @Bean
    public SecurityFilterChain adminSecurityChain(HttpSecurity http, SpringJpaAdminUserRepository repository) throws Exception {
        http.securityMatchers(matchers -> matchers.requestMatchers("/admin/**"))
                .userDetailsService(new SpringJpaAdminUserDetailsService(repository))
                .with(new CustomFormLoginConfigurer<>(authenticationFilter()), formLogin -> formLogin
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .failureForwardUrl("/admin/login/error")
                        .successHandler(new ForwardAuthenticationSuccessHandler("/admin/login/success"))
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                )
                .authorizeHttpRequests(requests -> {
                    requests
                            .requestMatchers("/admin/login/**").permitAll()
                            .requestMatchers("/admin/logout").permitAll()
                            .requestMatchers("/admin/**").authenticated();
                });

        return http.build();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (createDefaultAdmin) {
            event.getApplicationContext().getBean(SpringJpaAdminAuthService.class).createDefaultAdminUser();
        }
    }
}
