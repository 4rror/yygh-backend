package com.atguigu.yygh.order.controller;

import com.atguigu.yygh.common.jwt.JwtHelper;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author haisky
 */
@Api(tags = "订单接口")
@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    @ApiOperation(value = "取消预约")
    @GetMapping("auth/cancelOrder/{orderId}")
    public R cancelOrder(@PathVariable("orderId") Long orderId) {
        Boolean flag = orderInfoService.cancelOrder(orderId);
        return R.ok().data("flag", flag);
    }

    // 根据订单id查询订单详情
    @GetMapping("auth/getOrder/{orderId}")
    public R getOrder(@PathVariable Long orderId) {
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);
        return R.ok().data("orderInfo", orderInfo);
    }

    // 订单列表（条件查询带分页）
    @GetMapping("auth/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  OrderQueryVo orderQueryVo, HttpServletRequest request) {
        // 1、从请求头解析令牌
        Long userId = JwtHelper.getUserId(request.getHeader("token"));
        // 2、设置当前用户id，根据userid查询某一个用户的订单
        orderQueryVo.setUserId(userId);
        // 3、返回page对象
        Page<OrderInfo> pageModel = orderInfoService.selectPage(page, limit, orderQueryVo);
        return R.ok().data("pageModel", pageModel);
    }

    @ApiOperation(value = "获取订单状态列表")
    @GetMapping("auth/getStatusList")
    public R getStatusList() {
        return R.ok().data("statusList", OrderStatusEnum.getStatusList());
    }

    @ApiOperation(value = "创建订单")
    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    public R submitOrder(@PathVariable String scheduleId, @PathVariable Long patientId) {
        Long orderId = orderInfoService.saveOrder(scheduleId, patientId);
        return R.ok().data("orderId", orderId);
    }
}