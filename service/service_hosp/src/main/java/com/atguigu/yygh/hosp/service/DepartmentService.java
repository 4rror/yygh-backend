package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * @author haisky
 */
public interface DepartmentService {

    void saveDepartment(Map<String, Object> paramMap);

    Page<Department> selectPage(int page, int limit, DepartmentQueryVo queryVo);

    void remove(String hoscode, String depcode);

    List<DepartmentVo> findDeptTree(String hoscode);

    /**
     * 根据医院编号 和 科室编号获取科室数据
     */
    Department getDepartment(String hoscode, String depcode);

}
