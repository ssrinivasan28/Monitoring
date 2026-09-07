package com.islandpacific.sentinel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SpaWebController {

    @GetMapping(value = {
        "/",
        "/login",
        "/activate",
        "/reset-password",
        "/fleet",
        "/incidents",
        "/assistant",
        "/audit-logs",
        "/admin/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
