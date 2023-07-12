package com.atguigu.yygh.msm.service.impl;

import com.atguigu.yygh.common.msm.HttpUtils;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.vo.msm.MsmVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author haisky
 */
@Slf4j
@Service
public class MsmServiceImpl implements MsmService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean send(MsmVo msmVo) {
        String phone = msmVo.getPhone();
        String message = String.valueOf(msmVo.getParam().get("message"));
        // System.out.println(phone + "【尚医通】" + message);
        log.info("{} 【尚医通】：{}", phone, message);
        return false;
    }

    @Override
    public void send(String phone) {

        // 判断redis中是否有验证码，有就不发送
        String code = stringRedisTemplate.opsForValue().get(phone);
        if (!StringUtils.isEmpty(code)) {
            return;
        }

        code = this.getCode();

        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "67791fe6d9364bd09f671001ba104f16";
        Map<String, String> headers = new HashMap<>();
        // 最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<>();
        querys.put("mobile", phone);
        querys.put("param", "**code**:" + code + ",**minute**:5");

        // smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html
        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            // System.out.println(response.toString());
            // 获取response的body
            // System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 存储到redis
        stringRedisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
    }

    private String getCode() {
        long code = (long) (Math.random() * 1000);
        if (code < 1000) {
            code += 1000;
        }
        return String.valueOf(code);
    }
}
