package com.atguigu.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.util.HttpRequestHelper;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentStatusEnum;
import com.atguigu.yygh.hosp.feign.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.order.mapper.PaymentMapper;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.order.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author haisky
 */
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, PaymentInfo> implements PaymentService {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Override
    public PaymentInfo getPaymentInfo(Long orderId) {
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void paySuccess(Map<String, String> resultMap) {
        // 0、根据out_trade_no查询订单
        String out_trade_no = resultMap.get("out_trade_no");
        QueryWrapper<OrderInfo> wrapperOrder = new QueryWrapper<>();
        wrapperOrder.eq("out_trade_no", out_trade_no);
        OrderInfo orderInfo = orderInfoService.getOne(wrapperOrder);


        // 1、调用医院端的“更新支付状态接口”，将医院端订单的状态改成1--已支付
        String hoscode = orderInfo.getHoscode();
        // 获取医院设置中的apiUrl属性
        String apiUrl = hospitalFeignClient.getApiUrl(hoscode);
        String url = apiUrl + "/order/updatePayStatus";

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode", hoscode);
        paramMap.put("hosRecordId", orderInfo.getHosRecordId());// 医院端订单的id

        JSONObject jsonObject = HttpRequestHelper.sendRequest(paramMap, url);
        Integer code = jsonObject.getInteger("code");
        if (code != 200) {
            // 医院端更新订单支付状态接口调用失败
            throw new YyghException(20001, "医院端更新订单支付状态接口调用失败");
        }


        // 2、并更新平台端订单状态为已支付
        orderInfo.setOrderStatus(OrderStatusEnum.PAID.getStatus());
        orderInfoService.updateById(orderInfo);

        // 3、根据out_trade_no查询支付记录，更新支付记录状态为已支付
        QueryWrapper<PaymentInfo> wrapperPayment = new QueryWrapper<>();
        wrapperPayment.eq("out_trade_no", out_trade_no);// 根据order_id 或者 out_trade_no都能找到一个唯一的订单

        PaymentInfo paymentInfo = baseMapper.selectOne(wrapperPayment);
        // 设置状态和tradeNo+callback字段
        paymentInfo.setPaymentStatus(PaymentStatusEnum.PAID.getStatus());
        paymentInfo.setTradeNo(resultMap.get("transaction_id"));
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(resultMap.toString());

        baseMapper.updateById(paymentInfo);
    }

    @Override
    public void savePaymentInfo(OrderInfo orderInfo) {
        // 1、根据订单id和支付方式查询支付记录，如果存在直接return
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderInfo.getId());// 根据order_id可以唯一查询到该订单对应的支付记录
        // queryWrapper.eq("payment_type", paymentType);

        Integer count = baseMapper.selectCount(queryWrapper);
        if (count > 0) return;

        // 2、保存交易记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        // paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatusEnum.UNPAID.getStatus());
        // 2023-01-07|北京协和医院|科室名称|副主任医师
        String subject = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + "|" + orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle();
        paymentInfo.setSubject(subject);
        paymentInfo.setTotalAmount(orderInfo.getAmount());

        baseMapper.insert(paymentInfo);
    }
}