package com.pocketcombats.admin.plugin.auth.conf;

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class CustomFormLoginConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractAuthenticationFilterConfigurer<H, CustomFormLoginConfigurer<H>, UsernamePasswordAuthenticationFilter> {

    public CustomFormLoginConfigurer(UsernamePasswordAuthenticationFilter authenticationFilter) {
        super(authenticationFilter, "/admin/login");
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return PathPatternRequestMatcher.pathPattern(HttpMethod.POST, loginProcessingUrl);
    }

    @Override
    public CustomFormLoginConfigurer<H> loginPage(String loginPage) {
        return super.loginPage(loginPage);
    }

    public CustomFormLoginConfigurer<H> failureForwardUrl(String forwardUrl) {
        failureHandler(new ForwardAuthenticationFailureHandler(forwardUrl));
        return this;
    }
}
