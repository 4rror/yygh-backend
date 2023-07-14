package com.atguigu.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.mq.consts.MqConst;
import com.atguigu.mq.service.RabbitService;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.util.HttpRequestHelper;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.feign.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.order.mapper.OrderInfoMapper;
import com.atguigu.yygh.order.service.OrderInfoService;
import com.atguigu.yygh.order.service.WeixinService;
import com.atguigu.yygh.user.feign.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author haisky
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private WeixinService weixinService;

    @Override
    public Boolean cancelOrder(Long orderId) {
        // 1、查询平台端订单
        OrderInfo orderInfo = this.getById(orderId);

        // 2、如果退号时间已过，不能取消预约
        DateTime quitTime = new DateTime(orderInfo.getQuitTime());
        // if (quitTime.isBeforeNow()) {
        //     throw new YyghException(20001, "如果退号时间已过，不能取消预约");
        // }

        // 3、调用医院端取消预约接口
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode", orderInfo.getHoscode());
        reqMap.put("hosRecordId", orderInfo.getHosRecordId());

        String apiUrl = hospitalFeignClient.getApiUrl(orderInfo.getHoscode());
        String url = apiUrl + "/order/updateCancelStatus";

        JSONObject result = HttpRequestHelper.sendRequest(reqMap, url);

        if (result.getInteger("code") != 200) {
            throw new YyghException(20001, "医院端取消预约接口调用失败");
        }

        // 4、判断订单状态是否已支付，执行退款流程；退款流程写在weixinService中
        if (orderInfo.getOrderStatus() == OrderStatusEnum.PAID.getStatus()) {
            boolean isRefund = weixinService.refund(orderId);
            if (!isRefund) {
                throw new YyghException(20001, "退款失败");
            }
        }

        // 5、更改平台端订单状态为已取消
        orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
        this.updateById(orderInfo);

        // 6、异步更新mongodb排班号源数量+给就诊人发送短信通知
        // 医院服务接收到消息后，判断消息中如果存在两个num，说明是创建订单；如果没有两个num说明是取消订单。
        // 创建订单时将mg中排班的号源数量更新成医院端返回的两个num
        // 取消订单时将mg中排班的号源数量+1即可
        OrderMqVo mqVo = new OrderMqVo();
        mqVo.setScheduleId(orderInfo.getScheduleId());

        MsmVo msmVo = new MsmVo();
        msmVo.setPhone(orderInfo.getPatientPhone());
        msmVo.getParam().put("message", "取消成功");
        mqVo.setMsmVo(msmVo);
        // 第一个队列中发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, mqVo);

        return true;
    }

    @Override
    public OrderInfo getOrderInfo(Long id) {
        OrderInfo orderInfo = baseMapper.selectById(id);
        this.packOrderInfo(orderInfo);
        return orderInfo;
    }

    @Override
    public Page<OrderInfo> selectPage(Long page, Long limit, OrderQueryVo orderQueryVo) {

        // 1、封装分页对象,mp中1表示第一页
        Page<OrderInfo> pageParam = new Page<>(page, limit);

        // 2、封装查询条件，注意：每个条件值都需要判空校验
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        //(1)条件1，根据用户id查询当前用户自己的订单
        Long userId = orderQueryVo.getUserId();
        if (!StringUtils.isEmpty(userId)) {
            queryWrapper.eq("user_id", userId);
        }
        //(2)条件2，根据选择的就诊人查询订单
        Long patientId = orderQueryVo.getPatientId();
        if (!StringUtils.isEmpty(patientId)) {
            queryWrapper.eq("patient_id", patientId);
        }
        //(3)条件3，根据订单状态的状态进行过滤
        String orderStatus = orderQueryVo.getOrderStatus();
        if (!StringUtils.isEmpty(orderStatus)) {
            queryWrapper.eq("order_status", orderStatus);
        }
        // 3、调用mapper中的分页查询方法
        Page<OrderInfo> pages = baseMapper.selectPage(pageParam, queryWrapper);

        // 4、每一个订单，为param.orderStatusString赋值，订单状态的字符串
        pages.getRecords().forEach(item -> {
            this.packOrderInfo(item);
        });
        return pages;
    }

    // 私有方法，封装订单状态名称
    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        String str = OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus());
        orderInfo.getParam().put("orderStatusString", str);
        return orderInfo;
    }

    // 创建订单
    @Override
    public Long saveOrder(String scheduleId, Long patientId) {
        // 1 根据scheduleId获取排班数据
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);
        // 2 根据patientId获取就诊人信息
        Patient patient = patientFeignClient.getPatient(patientId);

        // 3 平台里面 ==> 调用医院订单确认接口，
        // 3.1 如果医院返回失败，挂号失败
        // 使用map集合封装需要传过医院数据
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode", scheduleOrderVo.getHoscode());
        paramMap.put("depcode", scheduleOrderVo.getDepcode());
        paramMap.put("hosScheduleId", scheduleOrderVo.getHosScheduleId());
        paramMap.put("reserveDate", new DateTime(scheduleOrderVo.getReserveDate()).toString("yyyy-MM-dd"));
        paramMap.put("reserveTime", scheduleOrderVo.getReserveTime());
        paramMap.put("amount", scheduleOrderVo.getAmount()); // 挂号费用
        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType", patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex", patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone", patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode", patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode", patient.getDistrictCode());
        paramMap.put("address", patient.getAddress());
        // 联系人
        paramMap.put("contactsName", patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo", patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone", patient.getContactsPhone());

        // 使用httpclient发送请求，请求医院接口
        // localhost:9998  医院端接口的ip和端口，正常应该是查询该医院的医院设置，获取apiUrl
        JSONObject result =
                HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");

        // 根据医院接口返回状态码判断  200 成功
        if (result.getInteger("code") == 200) { // 挂号成功
            // 3.2 如果返回成功，得到返回其他数据
            JSONObject jsonObject = result.getJSONObject("data");

            // 预约记录唯一标识（医院预约记录主键）
            String hosRecordId = jsonObject.getString("hosRecordId");
            // 预约序号
            Integer number = jsonObject.getInteger("number");
            // 取号时间
            String fetchTime = jsonObject.getString("fetchTime");
            // 取号地址
            String fetchAddress = jsonObject.getString("fetchAddress");

            // 4 如果医院接口返回成功，添加上面三部分数据到数据库
            OrderInfo orderInfo = new OrderInfo();
            // 设置添加数据--排班数据
            BeanUtils.copyProperties(scheduleOrderVo, orderInfo);

            // 设置添加数据--就诊人数据
            // 订单号
            String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(100);
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setScheduleId(scheduleId);
            orderInfo.setUserId(patient.getUserId());
            orderInfo.setPatientId(patientId);
            orderInfo.setPatientName(patient.getName());
            orderInfo.setPatientPhone(patient.getPhone());
            orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
            // 设置添加数据--医院接口返回数据
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            // 调用方法添加
            baseMapper.insert(orderInfo);

            // 排班可预约数
            Integer reservedNumber = jsonObject.getInteger("reservedNumber");
            // 排班剩余预约数
            Integer availableNumber = jsonObject.getInteger("availableNumber");

            // 发送mq信息更新号源和短信通知
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);
            // 短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            msmVo.getParam().put("message", "订单提交成功");

            orderMqVo.setMsmVo(msmVo);

            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);

            return orderInfo.getId();
        } else { // 挂号失败
            throw new YyghException(20001, "挂号失败");
        }
    }
}
