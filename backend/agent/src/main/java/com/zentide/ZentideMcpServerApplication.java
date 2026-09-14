package com.zentide;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@MapperScan("com.zentide.mapper")
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = {"com.zentide"})
public class ZentideMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZentideMcpServerApplication.class, args);
    }

}
