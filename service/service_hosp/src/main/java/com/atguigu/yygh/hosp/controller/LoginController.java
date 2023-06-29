package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * @author haisky
 */
@CrossOrigin
@RestController
@RequestMapping("/admin/hosp")
public class LoginController {

    /**
     * 管理员登录
     *
     * @return
     */
    @PostMapping("/user/login")
    public R login() {
        return R.ok().data("token", "admin-token");
    }

    /**
     * 登录成功后获取用户信息接口
     *
     * @return
     */
    @GetMapping("/user/info")
    public R info() {
        // {"code":20000,
        //  "data":{"roles":["admin"],
        //  "introduction":"I am a super administrator",
        //  "avatar":"https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
        //  "name":"Super Admin"}
        // }
        return R.ok()
                .data("roles", Arrays.asList("admin"))
                .data("introduction", "我是尚医通后台管理员")
                .data("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif")
                .data("name", "尚医通管理员");
    }

}