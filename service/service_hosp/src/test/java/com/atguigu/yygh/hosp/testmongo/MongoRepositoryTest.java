package com.atguigu.yygh.hosp.testmongo;

import com.atguigu.yygh.hosp.testmongo.entity.User;
import com.atguigu.yygh.hosp.testmongo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@SpringBootTest
public class MongoRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void createUser() {
        User user = new User(null, "张三", 20, "zs@qq.com", null);
        // save方法既可以做新增也可以做修改
        User save = userRepository.save(user);
        log.info("user: {}", save);
    }

    @Test
    public void testFindAll() {
        List<User> all = userRepository.findAll();
        all.forEach(i -> log.info("user: {}", i));
    }

    @Test
    public void testFindById() {
        User user = userRepository.findById("649d2ec1912e9c298ae17d3e").get();
        log.info("user: {}", user);
    }

    @Test
    public void testFindByCondition() {
        User user = new User();
        user.setName("张三");
        Example<User> example = Example.of(user);
        List<User> all = userRepository.findAll(example);
        all.forEach(i -> log.info("user: {}", i));
    }

    // 模糊查询
    @Test
    public void testFindByLike() {
        // 1、模糊查询的条件匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);

        // 2、封装条件  name like ?
        User user = new User();
        user.setName("南");

        Example<User> userExample = Example.of(user, matcher);

        List<User> users = userRepository.findAll(userExample);
        users.forEach(i -> log.info("user: {}", i));
    }

    // 分页查询
    @Test
    public void findUsersPage() {
        // 1、排序对象，age字段降序
        Sort sort = Sort.by(Sort.Direction.DESC, "age");
        // 2、分页对象，0为第一页
        Pageable pageable = PageRequest.of(0, 10, sort);

        // 3、模糊查询匹配器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        // 4、查询条件
        User user = new User();
        user.setName("三");
        Example<User> example = Example.of(user, matcher);

        // import org.springframework.data.domain.Page;
        Page<User> pages = userRepository.findAll(example, pageable);
        // 5、从page对象中解析数据
        List<User> userList = pages.getContent();
        long total = pages.getTotalElements();

        log.info("pages: {}", pages);
        log.info("total: {}", total);
        log.info("userList: {}", userList);
    }

    // 修改
    @Test
    public void updateUser() {
        User user = userRepository.findById("649d2ec1912e9c298ae17d3e").get();
        user.setName("张三_1");
        user.setAge(25);
        user.setEmail("111111111@qq.com");
        User save = userRepository.save(user);// user中的主键在数据库中存在就是修改操作
        log.info("user: {}", save);
    }

    // 删除
    @Test
    public void delete() {
        userRepository.deleteById("649d2ec1912e9c298ae17d3e");
    }

}
