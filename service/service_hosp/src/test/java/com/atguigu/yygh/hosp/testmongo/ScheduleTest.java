package com.atguigu.yygh.hosp.testmongo;

import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class ScheduleTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // 查询某个医院的大科室列表，每个大课室需要 depname + children + depcode （每个小科室也是这些属性）
    @Test
    public void test1() {
        String hoscode = "10000";
        List<Department> departmentList = departmentRepository.findByHoscode(hoscode);

        Map<String, List<Department>> map = departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));

        List<DepartmentVo> departmentVoList = new ArrayList<>();

        map.forEach((k, v) -> {
            // log.info("k = {}, v.size() = {}", k, v.size());
            // 封装一个大科室对象
            DepartmentVo departmentVo = new DepartmentVo();
            departmentVo.setDepcode(k);
            departmentVo.setDepname(v.get(0).getBigname());
            departmentVo.setChildren(this.transferVo(v));
            departmentVoList.add(departmentVo);
        });

        log.info("departmentVoList: {}", departmentVoList);
    }

    @Test
    public void test2() {
        Long page = 1L;
        Long limit = 5L;

        String hoscode = "10000";
        String depcode = "200040878";

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
                Aggregation.sort(Sort.Direction.ASC, "workDate"),
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

        log.info("result: {}", mappedResults);
    }

    private String getDayOfWeek(Date workDate) {
        DateTime dateTime = new DateTime(workDate);
        List<String> list = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        int dayOfWeek = dateTime.getDayOfWeek();
        return list.get(dayOfWeek - 1);
    }

    private List<DepartmentVo> transferVo(List<Department> v) {
        return v.stream().map(i -> {
            DepartmentVo departmentVo = new DepartmentVo();
            // 小科室编号
            departmentVo.setDepcode(i.getDepcode());
            // 小科室名称
            departmentVo.setDepname(i.getDepname());
            return departmentVo;
        }).collect(Collectors.toList());
    }

}
