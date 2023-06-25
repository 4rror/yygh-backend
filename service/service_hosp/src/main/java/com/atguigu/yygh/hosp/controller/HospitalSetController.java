package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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

    @ApiOperation(value = "新增医院设置")
    @PostMapping("/saveHospSet")
    public R save(@ApiParam(name = "hospitalSet", value = "医院设置对象", required = true) @RequestBody HospitalSet hospitalSet) {
        hospitalSet.setStatus(1);
        hospitalSetService.save(hospitalSet);
        return R.ok();
    }

    @ApiOperation(value = "根据ID查询医院设置")
    @GetMapping("/getHospSet/{id}")
    public R getById(@ApiParam(name = "id", value = "医院设置ID", required = true) @PathVariable String id) {
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        return R.ok().data("item", hospitalSet);
    }

    @ApiOperation(value = "根据ID修改医院设置")
    @PostMapping("updateHospSet")
    public R updateById(@ApiParam(name = "hospitalSet", value = "医院设置对象", required = true) @RequestBody HospitalSet hospitalSet) {
        hospitalSetService.updateById(hospitalSet);
        return R.ok();
    }

    @ApiOperation(value = "批量删除医院设置")
    @DeleteMapping("/batchRemove")
    public R batchRemoveHospitalSet(@RequestBody List<Long> ids) {
        hospitalSetService.removeByIds(ids);
        return R.ok();
    }

    @GetMapping("/lockHospitalSet/{id}/{status}")
    public R lockHospitalSet(@PathVariable("id") Long id, @PathVariable("status") Integer status) {
        // status取值范围0-1，如果不在该范围，返回message = "status不合法", code = 20001
        if (status != 0 && status != 1) {
            return R.error().message("status不合法");
        }
        // 如果医院设置不存在，返回message = "该医院暂未开通权限", code = 20001
        // 先查询
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        if (hospitalSet == null) {
            return R.error().message("该医院暂未开通权限");
        }
        // 如果该医院设置的status已经锁定状态，不需要重复锁定，在锁定之前，（status = 0） 判断status是否 = 0
        if (hospitalSet.getStatus().intValue() == status) {
            return R.error().message(status == 0 ? "请勿重复锁定" : "请勿重复解锁");
        }
        // 如果数据发生改变，updateTime需要一同更新
        hospitalSet.setStatus(status);
        hospitalSet.setUpdateTime(new Date());
        boolean b = hospitalSetService.updateById(hospitalSet);
        return b ? R.ok() : R.error();
    }
}
