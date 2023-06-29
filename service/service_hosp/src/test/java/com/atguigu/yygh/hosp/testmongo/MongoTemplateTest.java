package com.atguigu.yygh.hosp.testmongo;

import com.atguigu.yygh.hosp.testmongo.entity.User;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

@Slf4j
@SpringBootTest
public class MongoTemplateTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testInsert() {
        User user = new User(null, "test", 20, "test@qq.com", null);
        User insert = mongoTemplate.insert(user);
        log.info("insert: {}", insert);
    }

    @Test
    public void testFindAll() {
        List<User> all = mongoTemplate.findAll(User.class);
        all.forEach(i -> log.info("user: {}", i));
    }

    @Test
    public void testFindById() {
        User byId = mongoTemplate.findById("649d2495e3630962d5a4ef25", User.class);
        log.info("user: {}", byId);
    }

    @Test
    public void testFindByCondition() {
        Query query = new Query(Criteria.where("name").is("test").and("age").is(20));
        List<User> users = mongoTemplate.find(query, User.class);
        users.forEach(i -> log.info("user: {}", i));
    }

    @Test
    public void testFindByPage() {
        int pageNo = 2;
        int pageSize = 10;

        // Query query = new Query(Criteria.where("age").is(18));
        Query query = new Query();
        query.skip((pageNo - 1) * pageSize).limit(pageSize);

        // 总记录数
        long count = mongoTemplate.count(query, User.class);
        // 当前页结果集
        List<User> users = mongoTemplate.find(query, User.class);

        log.info("count: {}", count);
        users.forEach(i -> log.info("user: {}", i));
    }

    @Test
    public void updateUser() {
        // 更新条件
        Query query = new Query(Criteria.where("_id").is("649d2712566600005e00681d"));

        // 更新内容
        Update update = new Update();
        update.set("name", "戴震北");
        update.set("age", "35");
        update.set("email", "daizhenbei@icloud.com");

        // 执行更新
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, User.class);
        log.info("updateResult: {}", updateResult);
    }
}
