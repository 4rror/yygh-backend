package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Override
    public void update(Schedule schedule) {
        schedule.setUpdateTime(new Date());
        scheduleRepository.save(schedule);
    }

    @Override
    public Schedule getById(String id) {
        Schedule schedule = scheduleRepository.findById(id).get();
        this.packSchedule(schedule);// param属性赋值（医院名称，科室名称，星期）
        return schedule;
    }

    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        ScheduleOrderVo scheduleOrderVo = new ScheduleOrderVo();
        // 排班信息
        Schedule schedule = this.getById(scheduleId);
        if (null == schedule) {
            throw new YyghException();
        }
        // 获取预约规则信息
        Hospital hospital = hospitalService.getByHoscode(schedule.getHoscode());
        if (null == hospital) {
            throw new YyghException();
        }
        BookingRule bookingRule = hospital.getBookingRule();
        if (null == bookingRule) {
            throw new YyghException();
        }

        scheduleOrderVo.setHoscode(schedule.getHoscode());
        // scheduleOrderVo.setHosname(hospitalService.getHospName(schedule.getHoscode()));
        scheduleOrderVo.setHosname(hospitalService.getByHoscode(schedule.getHoscode()).getHosname());
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepartment(schedule.getHoscode(), schedule.getDepcode()).getDepname());
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());
        // 退号截止天数（如：就诊前一天为-1，当天为0）
        int quitDay = bookingRule.getQuitDay();
        DateTime quitTime = this.getDateTime(new DateTime(schedule.getWorkDate()).plusDays(quitDay).toDate(), bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitTime.toDate());
        // 预约开始时间
        DateTime startTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startTime.toDate());
        // 预约截止时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());
        // 当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
        scheduleOrderVo.setStopTime(stopTime.toDate());

        return scheduleOrderVo;
    }

    @Override
    public Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode) {
        Map<String, Object> result = new HashMap<>();

        // 获取预约规则
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        BookingRule bookingRule = hospital.getBookingRule();

        // 获取可预约日期分页数据
        IPage iPage = this.getListDate(page, limit, bookingRule);

        // 当前页可预约日期列表
        List<Date> dateList = iPage.getRecords();
        // 总日期个数
        Long total = iPage.getTotal();

        // 针对某医院某科室下在指定日期范围内的排班 进行聚合
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode) // 注意：是is
                .and("workDate").in(dateList); // 注意：是in

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")// 分组字段
                        .first("workDate").as("workDate")
                        .first("workDate").as("workDateMd")
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")
        );

        // 参数1：agg  参数2：被聚合的实体类  参数3：聚合后的实体类
        AggregationResults<BookingScheduleRuleVo> aggregationResults =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);

        // 获取聚合分组的结果
        List<BookingScheduleRuleVo> scheduleVoList = aggregationResults.getMappedResults();

        // list集合转成map， workDate作为key，ruleVo对象本身作为value
        Map<Date, BookingScheduleRuleVo> scheduleVoMap = scheduleVoList.stream().collect(
                Collectors.toMap(BookingScheduleRuleVo::getWorkDate, ruVo -> ruVo)
        );

        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        // 遍历日期集合，对于没有bookingScheduleRuleVo的日期，创建默认的bookingScheduleRuleVo
        for (int i = 0; i < dateList.size(); i++) {
            Date date = dateList.get(i);
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);

            if (null == bookingScheduleRuleVo) { // 说明当天没有排班医生
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                bookingScheduleRuleVo.setWorkDate(date);
                bookingScheduleRuleVo.setWorkDateMd(date);
                // 科室剩余预约数  -1表示无号
                bookingScheduleRuleVo.setDocCount(-1);
                bookingScheduleRuleVo.setAvailableNumber(-1);
                bookingScheduleRuleVo.setReservedNumber(-1);
            }

            // 星期
            String dayOfWeek = this.getDayOfWeek(date);
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

            // 最后一页最后一条记录为即将预约
            // 状态 0：正常, 1：即将放号, -1：当天已停止挂号
            if (i == dateList.size() - 1 && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }
            // 当天预约如果过了停号时间， 不能预约
            // 第一页第一条
            if (i == 0 && page == 1) {
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopTime.isBeforeNow()) {
                    // 停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }

        // 其他基础数据（hosname+bigname+depname+workDateString+releaseTime+stopTime）
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname", hospitalService.getByHoscode(hoscode).getHosname());
        Department department = departmentService.getDepartment(hoscode, depcode);
        baseMap.put("bigname", department.getBigname());
        baseMap.put("depname", department.getDepname());
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        baseMap.put("stopTime", bookingRule.getStopTime());

        // 可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", total);// 总日期个数
        result.put("baseMap", baseMap);

        return result;
    }

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

    /**
     * 获取可预约日期分页数据
     */
    private IPage<Date> getListDate(int page, int limit, BookingRule bookingRule) {
        // 当天放号时间
        DateTime releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        // 预约周期
        int cycle = bookingRule.getCycle();
        // 如果当天放号时间已过，则预约周期后一天为即将放号时间，周期加1
        if (releaseTime.isBeforeNow()) {
            cycle += 1;
        }

        // 可预约所有日期，最后一天显示即将放号倒计时
        List<Date> dateList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            // 计算当前预约日期
            DateTime curDateTime = new DateTime().plusDays(i);
            String dateString = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(dateString).toDate());
        }

        // 日期分页，由于预约周期不一样，页面一排最多显示7天数据，多了就要分页显示
        List<Date> pageDateList = new ArrayList<>();

        int start = (page - 1) * limit;
        int end = (page - 1) * limit + limit;

        if (end > dateList.size()) {
            end = dateList.size();
        }

        for (int i = start; i < end; i++) {
            pageDateList.add(dateList.get(i));
        }
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page(page, limit, dateList.size());
        iPage.setRecords(pageDateList);

        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " " + timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }
}