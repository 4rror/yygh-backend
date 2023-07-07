package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author haisky
 */
@Api(description = "排班接口")
@RestController
@RequestMapping("/admin/hosp/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @ApiOperation("查询排班的日期和其他数据")
    @GetMapping("/getScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public R getScheduleRuleVoList(@PathVariable("page") Long page, @PathVariable("limit") Long limit, @PathVariable("hoscode") String hoscode, @PathVariable("depcode") String depcode) {
        Map<String, Object> map = scheduleService.getScheduleRuleVoList(page, limit, hoscode, depcode);
        return R.ok().data(map);
    }

    @ApiOperation("查询排班列表")
    @GetMapping("/getScheduleDetail/{hoscode}/{depcode}/{workDate}")
    public R getScheduleDetail(@PathVariable("hoscode") String hoscode, @PathVariable("depcode") String depcode, @PathVariable("workDate") String workDate) {
        List<Schedule> list = scheduleService.getScheduleDetail(hoscode, depcode, workDate);
        return R.ok().data("list", list);
    }
}
