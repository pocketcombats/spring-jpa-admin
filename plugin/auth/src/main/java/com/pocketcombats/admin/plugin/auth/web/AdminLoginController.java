package com.pocketcombats.admin.plugin.auth.web;

import com.pocketcombats.admin.plugin.auth.persistence.SpringJpaAdminUser;
import com.pocketcombats.admin.plugin.auth.service.SpringJpaAdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
public class AdminLoginController {

    private final SpringJpaAdminAuthService service;
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private RequestCache requestCache = new HttpSessionRequestCache();

    public AdminLoginController(SpringJpaAdminAuthService service) {
        this.service = service;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/admin/login/error")
    public ModelAndView error(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        ModelAndView mav = new ModelAndView("admin/login");
        mav.addObject("errorMessage", getLoginErrorMessage(session));
        return mav;
    }

    @PostMapping("/admin/login/success")
    public void success(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SpringJpaAdminUser adminUser) {
            service.logAuthentication(adminUser.getId(), request);
        }

        SavedRequest savedRequest = this.requestCache.getRequest(request, response);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        if (savedRequest == null) {
            redirectStrategy.sendRedirect(request, response, "/admin/");
        } else {
            String targetUrl = savedRequest.getRedirectUrl();
            redirectStrategy.sendRedirect(request, response, targetUrl);
        }
    }

    private String getLoginErrorMessage(@Nullable HttpSession session) {
        if (session == null) {
            return "Invalid credentials";
        }
        if (!(session
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof AuthenticationException exception)) {
            return "Invalid credentials";
        }
        if (!StringUtils.hasText(exception.getMessage())) {
            return "Invalid credentials";
        }
        return exception.getMessage();
    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }
}
