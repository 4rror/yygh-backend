package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Hospital;

import java.util.Map;

/**
 * @author haisky
 */
public interface HospitalService {

    /**
     * 上传医院信息
     *
     * @param paramMap
     */
    void save(Map<String, Object> paramMap);

    /**
     * 查询医院
     *
     * @param hoscode
     * @return
     */
    Hospital getByHoscode(String hoscode);
}
