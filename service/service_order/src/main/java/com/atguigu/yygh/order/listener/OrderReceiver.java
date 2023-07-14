package com.atguigu.yygh.order.listener;

import com.atguigu.mq.consts.MqConst;
import com.atguigu.mq.service.RabbitService;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author haisky
 */
@Component
public class OrderReceiver {

    @Autowired
    OrderInfoService orderInfoService;

    @Autowired
    RabbitService rabbitService;

    @RabbitListener(
            bindings = {
                    @QueueBinding(
                            // 队列和交换机不存在时，会自动创建
                            value = @Queue(name = MqConst.QUEUE_TASK_ITEM, durable = "true"), // 当前监听程序负责监听的队列的名称
                            exchange = @Exchange(name = MqConst.EXCHANGE_DIRECT_TASK, durable = "true"),// 当前队列绑定到的交换机，默认是direct类型
                            key = {MqConst.ROUTING_TASK_ITEM} // 队列和交换机绑定时指定的路由键
                    )
            }
    )
    public void receive(String date) {

        // 订单服务接收到消息，就是一个日期
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reserve_date", date);
        queryWrapper.eq("order_status", OrderStatusEnum.PAID.getStatus());


        List<OrderInfo> list = orderInfoService.list(queryWrapper);

        list.forEach(orderInfo -> {
            String patientPhone = orderInfo.getPatientPhone();
            String message = "【尚医通】明天" + date + "就诊日，请及时就诊！";

            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(patientPhone);
            msmVo.getParam().put("message", message);

            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);
        });

    }
}