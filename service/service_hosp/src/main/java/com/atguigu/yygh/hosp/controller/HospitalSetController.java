package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
    @GetMapping("/findAll")
    public R findAll() {
        List<HospitalSet> list = hospitalSetService.list();
        return R.ok().data("list", list);
    }

    @ApiOperation(value = "医院设置删除")
    @DeleteMapping("/{id}")
    public R removeById(@PathVariable Long id) {
        hospitalSetService.removeById(id);
        return R.ok();
    }

    @ApiOperation(value = "分页条件查询")
    @PostMapping("/{page}/{limit}")
    public R pageQuery(@PathVariable("page") Long page, @PathVariable("limit") Long limit, @RequestBody HospitalSetQueryVo hospitalSetQueryVo) {
        // 1. 分页对象
        Page<HospitalSet> hospitalSetPage = new Page<>(page, limit);

        // 2.构造查询条件
        QueryWrapper<HospitalSet> queryWrapper = new QueryWrapper<>();
        String hoscode = hospitalSetQueryVo.getHoscode();
        String hosname = hospitalSetQueryVo.getHosname();
        if (!StringUtils.isEmpty(hoscode)) {
            queryWrapper.eq("hoscode", hoscode);
        }
        if (!StringUtils.isEmpty(hosname)) {
            queryWrapper.like("hosname", hosname);
        }

        hospitalSetService.page(hospitalSetPage, queryWrapper);

        // 3. 返回值
        List<HospitalSet> list = hospitalSetPage.getRecords();
        long total = hospitalSetPage.getTotal();

        return R.ok().data("rows", list).data("total", total);
    }
}
