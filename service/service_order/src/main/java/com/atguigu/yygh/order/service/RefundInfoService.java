package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author haisky
 */
public interface RefundInfoService extends IService<RefundInfo> {
    /**
     * 根据支付记录创建退款记录，并且返回退款记录；每个订单只能有一个退款记录
     *
     * @param paymentInfo
     */
    RefundInfo saveRefundInfo(PaymentInfo paymentInfo);
}