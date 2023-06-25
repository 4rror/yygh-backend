package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@Api(description = "医院设置接口")
@RequestMapping("/admin/hosp/hospitalSet")
public class HospitalSetController {

    @Autowired
    private HospitalSetService hospitalSetService;

    // 查询所有医院设置
    @ApiOperation(value = "医院设置列表")
    @GetMapping("findAll")
    public R findAll() {
        List<HospitalSet> list = hospitalSetService.list();
        return R.ok().data("list", list);
    }

    @ApiOperation(value = "医院设置删除")
    @DeleteMapping("{id}")
    public R removeById(@PathVariable Long id) {
        hospitalSetService.removeById(id);
        return R.ok();
    }
}
