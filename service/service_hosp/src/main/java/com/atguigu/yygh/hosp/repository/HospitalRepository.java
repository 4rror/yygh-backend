package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author haisk
 */
@Repository
public interface HospitalRepository extends MongoRepository<Hospital, String> {

    Hospital getHospitalByHoscode(String hoscode);

    Hospital findByHoscode(String hoscode);

    List<Hospital> findHospitalByHosnameLike(String hosname);
}
