package com.atguigu.yygh.order.service;

import java.util.Map;

/**
 * @author haisky
 */
public interface WeixinService {
    String createNative(Long orderId);

    /**
     * 根据订单号去微信第三方查询支付状态
     */
    Map<String, String> queryPayStatus(Long orderId);

    /***
     * 退款流程
     * @param orderId
     * @return 布尔表示是否退款成功
     */
    Boolean refund(Long orderId);
}
