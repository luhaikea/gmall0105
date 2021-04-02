package com.atguigu.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = "com.atguigu.gmall.order.mapper")
@SpringBootApplication
public class GmallOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallOrderServiceApplication.class, args);
    }

}
