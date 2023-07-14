package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PaymentService extends IService<PaymentInfo> {
    /**
     * 保存交易记录
     *
     * @param order
     */
    void savePaymentInfo(OrderInfo order);

    // 更新支付状态
    // outTradeNo  交易号
    // paymentType  支付类型 微信 支付宝
    // paramMap 调用微信查询支付状态接口返回map集合
    void paySuccess(Map<String, String> paramMap);

    /**
     * 根据订单id查询支付记录
     *
     * @param orderId
     * @return
     */
    PaymentInfo getPaymentInfo(Long orderId);
}