package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author haisky
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void saveDepartment(Map<String, Object> paramMap) {
        // 1、paramMap变成对象
        String jsonString = JSONObject.toJSONString(paramMap);
        Department department = JSONObject.parseObject(jsonString, Department.class);

        // 2、查询科室是否存在，医院编号 + 科室编号
        Department existDepartment = departmentRepository.findByHoscodeAndDepcode(department.getHoscode(), department.getDepcode());

        if (existDepartment != null) {
            // 修改
            department.setId(existDepartment.getId());
            department.setCreateTime(existDepartment.getCreateTime());
            department.setUpdateTime(new Date());
            departmentRepository.save(department);
        } else {
            // 新增
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            departmentRepository.save(department);
        }
    }

    @Override
    public Page<Department> selectPage(int page, int limit, DepartmentQueryVo queryVo) {
        // 1、创建时间降序排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        // 2、分页对象，0为第一页
        PageRequest pageAble = PageRequest.of(page - 1, limit, sort);
        // 3、查询条件
        Department department = new Department();
        BeanUtils.copyProperties(queryVo, department);
        // 4、模糊查询匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        // 5、创建Example
        Example<Department> example = Example.of(department, matcher);
        return departmentRepository.findAll(example, pageAble);
    }

    @Override
    public void remove(String hoscode, String depcode) {
        Department department = departmentRepository.findByHoscodeAndDepcode(hoscode, depcode);
        if (department != null) {
            departmentRepository.deleteById(department.getId());
        }
    }

    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        List<Department> departmentList = departmentRepository.findByHoscode(hoscode);
        Map<String, List<Department>> map = departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));
        List<DepartmentVo> departmentVoList = new ArrayList<>();
        map.forEach((k, v) -> {
            DepartmentVo departmentVo = new DepartmentVo();
            departmentVo.setDepcode(k);
            departmentVo.setDepname(v.size() > 0 ? v.get(0).getBigname() : "暂无名称");
            departmentVo.setChildren(this.transferVo(v));
            departmentVoList.add(departmentVo);
        });
        return departmentVoList;
    }

    private List<DepartmentVo> transferVo(List<Department> v) {
        return v.stream().map(i -> {
            DepartmentVo departmentVo = new DepartmentVo();
            departmentVo.setDepname(i.getDepname());
            departmentVo.setDepcode(i.getDepcode());
            return departmentVo;
        }).collect(Collectors.toList());
    }
}
