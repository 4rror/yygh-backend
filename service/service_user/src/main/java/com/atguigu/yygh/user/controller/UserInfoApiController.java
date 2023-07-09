package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.jwt.AuthContextHolder;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author haisky
 */

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    // 用户认证接口
    @PostMapping("/auth/userAuth")
    public R userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        userInfoService.userAuth(userId, userAuthVo);
        return R.ok();
    }

    // 获取用户id信息接口
    @GetMapping("/auth/getUserInfo")
    public R getUserInfo(HttpServletRequest request) {

        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);

        // 从枚举类中查询认证状态字符串
        Integer authStatus = userInfo.getAuthStatus();
        String statusNameByStatus = AuthStatusEnum.getStatusNameByStatus(authStatus);

        userInfo.getParam().put("authStatusString", statusNameByStatus);

        return R.ok().data("userInfo", userInfo);
    }

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
