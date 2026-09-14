package com.zentide;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.zentide"})
@MapperScan("com.zentide.mapper")
@EnableTransactionManagement
@EnableScheduling
public class ZentideWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZentideWebApplication.class, args);
    }
}
