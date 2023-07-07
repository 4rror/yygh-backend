package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author haisky
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void save(Map<String, Object> paramMap) {
        // 1、paramMap转换department对象
        String paramMapString = JSONObject.toJSONString(paramMap);
        Schedule schedule = JSONObject.parseObject(paramMapString, Schedule.class);

        String hoscode = schedule.getHoscode();
        String hosScheduleId = schedule.getHosScheduleId();// 医院端排班id

        // 2、根据医院编号 和 排班编号查询
        Schedule scheduleExist = scheduleRepository
                .getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);

        // 判断
        if (scheduleExist != null) {
            scheduleExist.setUpdateTime(new Date());
            scheduleExist.setIsDeleted(0);
            scheduleExist.setStatus(1);
            scheduleRepository.save(scheduleExist);
        } else {
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
        }
    }

    @Override
    public Page<Schedule> selectPage(Integer page, Integer limit, ScheduleQueryVo scheduleQueryVo) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo, schedule);

        // 创建匹配器，即如何使用查询条件
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);

        Example<Schedule> example = Example.of(schedule, matcher);
        Page<Schedule> pages = scheduleRepository.findAll(example, pageable);
        return pages;
    }

    @Override
    public void remove(String hoscode, String hosScheduleId) {
        Schedule schedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (null != schedule) {
            scheduleRepository.deleteById(schedule.getId());
        }
    }

    @Override
    public Map<String, Object> getScheduleRuleVoList(Long page, Long limit, String hoscode, String depcode) {
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);

        // agg: 聚合对象
        // Schedule.class: 进行聚合统计的数据类型
        // BookingScheduleRuleVo.class: 聚合后，每一组数据提取一些数据封装成一个BookingScheduleRuleVo
        Aggregation agg = Aggregation.newAggregation(
                // 针对哪些排班进行聚合
                Aggregation.match(criteria),
                // 按照排班中的workDate进行分组
                Aggregation.group("workDate")
                        // 每一组workDate相同的排班，提取一些数据
                        .first("workDate").as("workDate")
                        .first("workDate").as("workDateMd")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber")
                        .count().as("docCount"),
                // 排序
                Aggregation.sort(Sort.Direction.DESC, "workDate"),
                // 分页
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
        );

        AggregationResults<BookingScheduleRuleVo> aggregate = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);

        // 当前页的日期列表对应的vo集合
        List<BookingScheduleRuleVo> mappedResults = aggregate.getMappedResults();

        for (BookingScheduleRuleVo ruleVo : mappedResults) {
            Date workDate = ruleVo.getWorkDate();
            String dayOfWeek = getDayOfWeek(workDate);
            ruleVo.setDayOfWeek(dayOfWeek);
        }

        // 封装map
        Map<String, Object> map = new HashMap<>();
        map.put("bookingScheduleRuleVoList", mappedResults);
        // 总日期个数(vo)
        map.put("total", this.calTotal(hoscode, depcode));
        return map;
    }

    @Override
    public List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate) {
        Date date = new DateTime(workDate).toDate();
        List<Schedule> list = scheduleRepository.findByHoscodeAndDepcodeAndWorkDate(hoscode, depcode, date);
        // 每个schedule对象的param属性，添加三个值（hosname + depname + dayOfWeek）这三个数形，在后台系统中没有使用，前端挂号网站使用
        list.forEach(this::packSchedule);
        return list;
    }

    private void packSchedule(Schedule schedule) {
        String hoscode = schedule.getHoscode();
        String depcode = schedule.getDepcode();

        // 当前排班对应的医院名称
        Hospital hospital = hospitalRepository.findByHoscode(hoscode);
        String hosname = hospital.getHosname();

        // 当前排班对应的小科室名称
        Department department = departmentRepository.findByHoscodeAndDepcode(hoscode, depcode);
        String depname = department.getDepname();

        Date workDate = schedule.getWorkDate();
        String dayOfWeek = this.getDayOfWeek(workDate);

        schedule.getParam().put("hosname", hosname);
        schedule.getParam().put("depname", depname);
        schedule.getParam().put("dayOfWeek", dayOfWeek);
    }

    private Integer calTotal(String hoscode, String depcode) {
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);

        // agg: 聚合对象
        // Schedule.class: 进行聚合统计的数据类型
        // BookingScheduleRuleVo.class: 聚合后，每一组数据提取一些数据封装成一个BookingScheduleRuleVo
        Aggregation agg = Aggregation.newAggregation(
                // 针对哪些排班进行聚合
                Aggregation.match(criteria),
                // 按照排班中的workDate进行分组
                Aggregation.group("workDate"),
                // 排序
                Aggregation.sort(Sort.Direction.DESC, "workDate")
        );

        AggregationResults<BookingScheduleRuleVo> aggregate = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);

        // 当前页的日期列表对应的vo集合
        List<BookingScheduleRuleVo> mappedResults = aggregate.getMappedResults();
        return mappedResults.size();
    }

    private String getDayOfWeek(Date workDate) {
        DateTime dateTime = new DateTime(workDate);
        List<String> list = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        int dayOfWeek = dateTime.getDayOfWeek();
        return list.get(dayOfWeek - 1);
    }
}