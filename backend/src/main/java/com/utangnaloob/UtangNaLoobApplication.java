package com.utangnaloob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UtangNaLoobApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtangNaLoobApplication.class, args);
    }
}
