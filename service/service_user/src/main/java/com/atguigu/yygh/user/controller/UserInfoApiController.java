package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author haisky
 */

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 登陆
     *
     * @param loginVo 手机号 + 短信验证码
     * @return 带token
     */
    @PostMapping("/login")
    public R login(@RequestBody LoginVo loginVo) {
        Map<String, Object> map;
        if (StringUtils.isEmpty(loginVo.getOpenid())) {
            // 直接通过手机+验证码登陆
            map = userInfoService.login(loginVo);
        } else {
            // 为微信用户绑定手机号
            map = userInfoService.bundle(loginVo);
        }
        // return R.ok().data("token", null).data("name", null);
        return R.ok().data(map);
    }
}
