package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * @author haisky
 */
public interface ScheduleService {
    /**
     * 上传排班信息
     *
     * @param paramMap
     */
    void save(Map<String, Object> paramMap);

    /**
     * 分页查询
     *
     * @param page            当前页码
     * @param limit           每页记录数
     * @param scheduleQueryVo 查询条件
     * @return
     */
    Page<Schedule> selectPage(Integer page, Integer limit, ScheduleQueryVo scheduleQueryVo);

    /**
     * 删除排班
     *
     * @param hoscode
     * @param hosScheduleId
     */
    void remove(String hoscode, String hosScheduleId);

    Map<String, Object> getScheduleRuleVoList(Long page, Long limit, String hoscode, String depcode);

    List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate);

    /**
     * 获取排班可预约日期数据
     *
     * @param page
     * @param limit
     * @param hoscode
     * @param depcode
     */
    Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode);

    /**
     * 根据id获取排班
     *
     * @param id
     */
    Schedule getById(String id);

    // 根据排班id获取预约下单数据
    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    /**
     * 修改排班
     */
    void update(Schedule schedule);
}
