package com.enterprise.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClearingSettlementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClearingSettlementApplication.class, args);
    }
}
