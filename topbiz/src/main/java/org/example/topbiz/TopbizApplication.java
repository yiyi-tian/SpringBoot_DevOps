package org.example.topbiz;

import org.example.common.AccessLogConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(AccessLogConfiguration.class)
public class TopbizApplication {
    public static void main(String[] args) {
        SpringApplication.run(TopbizApplication.class, args);
    }
}