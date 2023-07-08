package com.atguigu.yygh.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.jwt.JwtHelper;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.result.ResultCode;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.config.ConstantPropertiesUtil;
import com.atguigu.yygh.user.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author haisky
 */
@Slf4j
@Controller
@RequestMapping("/api/user/wx")
public class WeixinApiController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 扫码之后点击允许，（微信端）自动调用
     * <p>
     * 需求：
     * 1. 获取微信端的code临时令牌，参数格式：redirect_uri?code=CODE<br>
     * 2. 根据code临时令牌，去调用微信端接口获取openid和accessToken（为了继续获取该微信的昵称）<br>
     * 3. 根据accessToken获取该微信的昵称nickname<br>
     * 4. 先判断该微信用户在user_info表中是否存在，如果不存在 利用openid + nickname 自动注册<br>
     * 5. 判断用户status是否被锁定，如果用户被锁定，抛出自定义异常
     * 6. 准备name和token<br>
     * name=userinfo.name   or   userinfo.nickname   or   userinfo.phone   和之前的规则一样<br>
     * token = name + userinfo.id 创建一个jwt令牌 和之前的规则一样<br>
     * 7. return 重定向到前端的 callback.vue 并且传递参数 url?name=NAME&token=TOKEN&openid=OPENID<br>
     * 如果该微信用户的phone字段为空，OPENID填写实际的openid值<br>
     * 如果该微信用户的phone字段不为空，OPENID填写空字符串即可
     */
    @GetMapping("/callback")
    public String callback(String code) throws UnsupportedEncodingException {
        log.info("code: {}", code);
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token" +
                "?appid=" + ConstantPropertiesUtil.APP_ID +
                "&secret=" + ConstantPropertiesUtil.APP_SECRET +
                "&code=" + code +
                "&grant_type=authorization_code";

        String respJsonStr = restTemplate.getForObject(url, String.class);
        /*
         * {
         *   "access_token":"ACCESS_TOKEN",
         *   "expires_in":7200,
         *   "refresh_token":"REFRESH_TOKEN",
         *   "openid":"OPENID",
         *   "scope":"SCOPE",
         *   "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
         * }
         */
        log.info("stringObj: {}", respJsonStr);

        JSONObject jsonObject = JSONObject.parseObject(respJsonStr);
        String accessToken = jsonObject.getString("access_token");
        String openid = jsonObject.getString("openid");

        // 根据openid查询user_info表中用户是否存在
        UserInfo userInfo = userInfoService.selectByOpenid(openid);
        if (userInfo == null) {
            // 不存在，自动注册
            userInfo = new UserInfo();

            // 调用接口，获取nickname
            String urlForNickname = "https://api.weixin.qq.com/sns/userinfo" +
                    "?access_token=" + accessToken +
                    "&openid=" + openid;

            String forObject = restTemplate.getForObject(urlForNickname, String.class);
            JSONObject object = JSONObject.parseObject(forObject);

            String nickname = object.getString("nickname");

            userInfo.setOpenid(openid);
            userInfo.setNickName(nickname);
            userInfo.setStatus(1);
            userInfo.setAuthStatus(AuthStatusEnum.NO_AUTH.getStatus());

            userInfoService.save(userInfo);
        }

        if (userInfo.getStatus() == 0) {
            throw new YyghException(ResultCode.ERROR, "当前微信用户被锁定");
        }

        String name = userInfo.getName();
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
            if (StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
        }

        String token = JwtHelper.createToken(userInfo.getId(), name);

        String callbackParamOpenid = "";
        if (StringUtils.isEmpty(userInfo.getPhone())) {
            callbackParamOpenid = openid;
        }

        return "redirect:http://localhost:3000/weixin/callback?name=" + URLEncoder.encode(name, "UTF-8") + "&token=" + token + "&openid=" + callbackParamOpenid;
    }

    /**
     * 前端的二维码需要的参数
     */
    @ResponseBody
    @GetMapping("/getLoginParam")
    public R getLoginParam() throws UnsupportedEncodingException {
        Map<String, Object> map = new HashMap<>();
        map.put("self_redirect", true);
        map.put("id", "weixinLogin");
        map.put("appid", ConstantPropertiesUtil.APP_ID);
        map.put("scope", "snsapi_login");
        map.put("redirect_uri", URLEncoder.encode(ConstantPropertiesUtil.REDIRECT_URL, "UTF-8"));
        return R.ok().data(map);
    }

}
