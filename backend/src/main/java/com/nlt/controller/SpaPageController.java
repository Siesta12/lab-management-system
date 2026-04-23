package com.nlt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端单页应用入口转发控制器。
 * 处理 /login、/admin 及其子路由的直达和刷新，避免后端静态资源 404。
 */
@Controller
public class SpaPageController {

    @GetMapping({"/login", "/admin", "/admin/**"})
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
