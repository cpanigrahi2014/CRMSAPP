package com.crm.opportunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.crm.opportunity", "com.crm.common"})
public class OpportunityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpportunityServiceApplication.class, args);
    }
}
