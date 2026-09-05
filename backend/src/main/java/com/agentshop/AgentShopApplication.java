package com.agentshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class AgentShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentShopApplication.class, args);
    }
}
