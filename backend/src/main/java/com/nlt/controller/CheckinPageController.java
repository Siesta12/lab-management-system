package com.nlt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 扫码签到页面转发控制器。
 * 将浏览器直接访问的 /checkin 前端路由转发到静态入口页，
 * 这样 Vue Router 才能接管页面渲染。
 */
@Controller
public class CheckinPageController {

    /**
     * 直接访问签到页时转发到前端入口。
     *
     * @return 前端静态入口页
     */
    @GetMapping("/checkin")
    public String checkinPage() {
        return "forward:/index.html";
    }
}
