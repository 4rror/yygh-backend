package com.atguigu.yygh.task.task;

import com.atguigu.mq.consts.MqConst;
import com.atguigu.mq.service.RabbitService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class YyghTask {

    @Autowired
    private RabbitService rabbitService;


    // 秒 分 时  日 月 星期          年(spring-task中省略年份)
    // 5  10 20 *  *   ?   每个月下的每一天，每一天中的20:10:05 执行一次
    //*  5  *   *  *  ?  每一小时的第五分钟内下的每一秒执行一次
    // 1  5  *   *  *  ?  每一小时的第五分钟内下的第一秒执行一次
    // 0  0  20  *  * ? 每天晚上的8点
    // 8  *  *  *  * ? 每分钟的第8秒执行一次
    //* - 任意（每一秒，每一分钟...）
    // ? 星期位，表示不管这个时间点是星期几，也就是和星期几无关
    @Scheduled(cron = "8 * * * * ?")
    public void taskPatientTips() {
//        System.out.println(new DateTime().toString("yyyy-MM-dd HH:mm:ss"));

        String date = new DateTime().plusDays(1).toString("yyyy-MM-dd");

        // 向第三个队列发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_ITEM, date);

    }

}
