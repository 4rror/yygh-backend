package com.atguigu.yygh.order.service.impl;

import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.order.service.PaymentService;
import com.atguigu.yygh.order.service.RefundInfoService;
import com.atguigu.yygh.order.service.WeixinService;
import com.atguigu.yygh.order.util.ConstantPropertiesUtils;
import com.atguigu.yygh.order.util.HttpClient;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author haisky
 */
@Service
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundInfoService refundInfoService;

    @Override
    public Boolean refund(Long orderId) {
        try {
            // 1、根据订单id查询支付记录
            PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(orderId);

            // 2、利用支付记录创建退款记录
            RefundInfo refundInfo = refundInfoService.saveRefundInfo(paymentInfoQuery);

            // 3、判断退款记录的退款状态如果为已退款则直接return
            if (refundInfo.getRefundStatus() == RefundStatusEnum.REFUND.getStatus()) {
                // 已经退款
                return true;
            }

            // 4、如果未退款，调用微信退款接口
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("transaction_id", paymentInfoQuery.getTradeNo());
            paramMap.put("out_trade_no", paymentInfoQuery.getOutTradeNo());
            paramMap.put("out_refund_no", "tk" + paymentInfoQuery.getOutTradeNo()); // 商户退款单号
            // paramMap.put("total_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            // paramMap.put("refund_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee", "1");
            paramMap.put("refund_fee", "1");

            // map转成xml字符串，字符串中使用PARTNERKEY生成签名
            String paramXml = WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY);

            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/secapi/pay/refund");
            client.setXmlParam(paramXml);
            client.setHttps(true);
            client.setCert(true);// 是否需要证书
            client.setCertPassword(ConstantPropertiesUtils.PARTNER);// 证书密码商户号
            client.post();

            // 5、微信端返回值，xml字符串转成map对象
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);

            // 6、result_code等于SUCCESS说明退款接口调用成功
            // 将微信端返回值更新到退款记录中
            if (WXPayConstants.SUCCESS.equalsIgnoreCase(resultMap.get("result_code"))) {
                // 如果退款成功，将退款记录的4个属性重新赋值
                refundInfo.setTradeNo(resultMap.get("refund_id"));// 微信退款单号
                refundInfo.setCallbackTime(new Date());// 回调时间
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());// 退款状态已退款
                refundInfo.setCallbackContent(resultMap.toString());// 微信接口返回的map.toString

                refundInfoService.updateById(refundInfo);// 更新到数据库中
                return true;// 成功
            }
            return false;// 失败
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;// 失败
    }

    @Override
    public Map<String, String> queryPayStatus(Long orderId) {
        try {
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            // 1、封装参数
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            // 2、设置请求
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();
            // 3、返回第三方的数据，转成Map
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            // 4、返回
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String createNative(Long orderId) {
        try {
            // 1、先尝试从redis中根据订单id获取到map（支付链接），如果存在直接返回
            String payUrl = stringRedisTemplate.opsForValue().get(orderId.toString());
            if (null != payUrl) return payUrl;

            // 2、根据id获取订单信息
            OrderInfo order = orderInfoService.getById(orderId);
            // 3、根据订单对象，创建支付记录对象
            paymentService.savePaymentInfo(order);

            // 4、调用微信端“交易预创建”接口：封装参数
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            Date reserveDate = order.getReserveDate();
            String reserveDateString = new DateTime(reserveDate).toString("yyyy/MM/dd");
            String body = reserveDateString + "就诊" + order.getDepname();
            paramMap.put("body", body);
            paramMap.put("out_trade_no", order.getOutTradeNo());
            // paramMap.put("total_fee", order.getAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee", "1");// 为了测试，支付金额为1分
            paramMap.put("spbill_create_ip", "127.0.0.1");
            paramMap.put("notify_url", "http://guli.shop/api/order/weixinPay/weixinNotify");
            paramMap.put("trade_type", "NATIVE");

            // 5、使用HttpClient发送请求
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");

            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();

            // 6、微信端返回数据
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);

            // 7、封装返回结果集
            // Map map = new HashMap<>();
            // // map.put("orderId", orderId);//订单id
            // // map.put("totalFee", order.getAmount());//订单的金额
            // // map.put("resultCode", resultMap.get("result_code"));//SUCCESS/FAIL
            // map.put("codeUrl", resultMap.get("code_url"));// 支付链接

            // 8、暂存到redis
            stringRedisTemplate.opsForValue().set(orderId.toString(), resultMap.get("code_url"), 5, TimeUnit.SECONDS);

            return resultMap.get("code_url");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
