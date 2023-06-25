package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
public class HospitalSetController {

    @Autowired
    private HospitalSetService hospitalSetService;

    // 查询所有医院设置
    @GetMapping("findAll")
    public List<HospitalSet> findAll() {
        List<HospitalSet> list = hospitalSetService.list();
        return list;
    }

    @DeleteMapping("{id}")
    public boolean removeById(@PathVariable Long id) {
        return hospitalSetService.removeById(id);
    }
}
