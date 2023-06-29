package com.atguigu.yygh.hosp.testmongo.repository;

import com.atguigu.yygh.hosp.testmongo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
}
