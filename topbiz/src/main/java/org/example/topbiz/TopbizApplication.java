package org.example.topbiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TopbizApplication {
    public static void main(String[] args) {
        SpringApplication.run(TopbizApplication.class, args);
    }
}