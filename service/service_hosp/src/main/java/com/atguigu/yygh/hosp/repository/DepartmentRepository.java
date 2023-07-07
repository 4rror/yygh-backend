package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author haisky
 */
@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    Department findByHoscodeAndDepcode(String hoscode, String depcode);

    List<Department> findByHoscode(String hoscode);
}
