package com.datacollect;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.datacollect.mapper")
public class DataCollectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCollectServiceApplication.class, args);
    }
}
