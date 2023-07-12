package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author haisky
 */
public interface OrderInfoService extends IService<OrderInfo> {

    // 保存订单
    Long saveOrder(String scheduleId, Long patientId);

    /**
     * 订单列表
     */
    Page<OrderInfo> selectPage(Long page, Long limit, OrderQueryVo orderQueryVo);

    /**
     * 获取订单详情
     */
    OrderInfo getOrderInfo(Long id);
}
