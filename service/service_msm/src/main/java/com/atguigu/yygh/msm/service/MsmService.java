package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

/**
 * @author haisky
 */
public interface MsmService {
    void send(String phone);

    // 给指定手机号发送短信通知
    boolean send(MsmVo msmVo);
}
