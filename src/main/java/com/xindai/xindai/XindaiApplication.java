package com.xindai.xindai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan({"com.xindai.xindai.modules.*.mapper", "com.xindai.xindai.common.mapper"})
public class XindaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XindaiApplication.class, args);
    }

}
