package com.nlt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaPageController {

    @GetMapping({"/login", "/admin", "/admin/**"})
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
