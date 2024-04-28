package com.pocketcombats.admin.plugin.auth.conf;

import com.pocketcombats.admin.plugin.auth.service.RequestAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Set;

public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher("/admin/login", "POST");
    private static final Set<String> LOCALHOST_ADDRESSES = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    private final boolean adminLocalhostOnly;

    public CustomUsernamePasswordAuthenticationFilter(boolean adminLocalhostOnly) {
        super();
        this.adminLocalhostOnly = adminLocalhostOnly;
        setRequiresAuthenticationRequestMatcher(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public CustomUsernamePasswordAuthenticationFilter(
            boolean adminLocalhostOnly,
            AuthenticationManager authenticationManager
    ) {
        super(authenticationManager);
        this.adminLocalhostOnly = adminLocalhostOnly;
        setRequiresAuthenticationRequestMatcher(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (adminLocalhostOnly) {
            String username = obtainUsername(request);
            if (username != null && username.equalsIgnoreCase("admin")) {
                String ipAddress = RequestAddress.resolve(request);
                if (!LOCALHOST_ADDRESSES.contains(ipAddress)) {
                    throw new AuthenticationServiceException("Access for admin enabled for localhost only");
                }
            }
        }

        return super.attemptAuthentication(request, response);
    }
}
