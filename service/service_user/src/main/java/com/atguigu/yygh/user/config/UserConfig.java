package com.atguigu.yygh.user.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author haisky
 */
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.atguigu")
@EnableFeignClients(basePackages = "com.atguigu")
@Configuration
public class UserConfig {

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}
