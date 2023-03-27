package com.pocketcombats.admin.web;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    @RequestMapping("/admin/")
    @Secured("ROLE_JPA_ADMIN")
    public ModelAndView index() {
        return new ModelAndView("admin/index");
    }
}
