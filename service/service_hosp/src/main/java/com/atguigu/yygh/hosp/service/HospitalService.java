package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * @author haisky
 */
public interface HospitalService {

    /**
     * 根据医院名称获取医院列表
     */
    List<Hospital> findByHosname(String hosname);

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

    /**
     * 医院预约挂号详情
     */
    Map<String, Object> item(String hoscode);

    Page<Hospital> pageList(HospitalQueryVo hospitalQueryVo, Integer pageNum, Integer pageSize);

    void updateStatus(String id, Integer status);

    Hospital getById(String id);
}
