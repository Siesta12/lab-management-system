package com.nlt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckinPageController {

    @GetMapping("/checkin")
    public String checkinPage() {
        return "forward:/index.html";
    }
}
