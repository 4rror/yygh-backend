package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.msm.service.MsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author haisky
 */
@Controller
@RequestMapping("/api/msm")
public class MsmController {

    @Autowired
    private MsmService msmService;

    @ResponseBody
    @GetMapping("/sendCode/{phone}")
    public R sendCode(@PathVariable("phone") String phone) {
        msmService.send(phone);
        return R.ok();
    }
}
