package com.etread;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户认证服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.etread") // 1. 扩大扫描范围，能抓到 common 里的 MinioUtil
@MapperScan("com.etread.mapper") // 2. 告诉 MyBatis-Plus 去哪里找那些 Mapper 搬运工
@EnableDiscoveryClient
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);

        System.out.println("======================================");
        System.out.println("======================================");
    }
}