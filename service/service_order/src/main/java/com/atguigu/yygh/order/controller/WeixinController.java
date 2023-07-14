package com.atguigu.yygh.order.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.order.service.PaymentService;
import com.atguigu.yygh.order.service.WeixinService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author haisky
 */
@RestController
@RequestMapping("/api/order/weixin")
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentService paymentService;

    @ApiOperation(value = "查询支付状态")
    @GetMapping("/queryPayStatus/{orderId}")
    public R queryPayStatus(@PathVariable("orderId") Long orderId) {
        // 调用微信端查询接口，直接返回map
        Map<String, String> map = weixinService.queryPayStatus(orderId);
        if (map == null) {// 出错
            return R.error().message("支付出错");
        }
        if ("SUCCESS".equals(map.get("trade_state"))) {// 如果成功
            // 更改订单状态，处理支付结果
            paymentService.paySuccess(map);
            return R.ok().message("支付成功");
        }
        return R.ok().message("支付中");
    }

    @GetMapping("/createNative/{orderId}")
    public R createNative(@PathVariable("orderId") Long orderId) {
        String codeUrl = weixinService.createNative(orderId);
        return R.ok().data("codeUrl", codeUrl);
    }
}
