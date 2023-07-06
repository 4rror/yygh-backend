package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * @author haisky
 */
@CrossOrigin
@RestController
@Api(description = "医院接口")
@RequestMapping("/admin/hosp/hospital")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @GetMapping("/show/{id}")
    public R showHospitalDetail(@PathVariable("id") String id) {
        Hospital item = hospitalService.getById(id);
        return R.ok().data("item", item);
    }

    /**
     * @param hospitalQueryVo 查询参数
     * @param pageNum         分页页码
     * @param pageSize        分页大小
     * @return 当前页的结果集 + 总记录数
     */
    @ApiOperation("医院列表")
    @PostMapping("/hospitalList/{pageNum}/{pageSize}")
    public R hostList(@RequestBody HospitalQueryVo hospitalQueryVo, @PathVariable("pageNum") Integer pageNum, @PathVariable("pageSize") Integer pageSize) {
        Page<Hospital> page = hospitalService.pageList(hospitalQueryVo, pageNum, pageSize);
        return R.ok().data("page", page);
    }

    @ApiOperation("更改医院上下线状态")
    @PostMapping("/updateStatus/{id}/{status}")
    public R updateStatus(@PathVariable("id") String id, @PathVariable("status") Integer status) {
        hospitalService.updateStatus(id, status);
        return R.ok();
    }
}
