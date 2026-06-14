package com.example.jwtapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JwtApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JwtApiApplication.class, args);
    }
}
