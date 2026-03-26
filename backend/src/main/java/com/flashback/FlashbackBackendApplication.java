package com.flashback;

import com.flashback.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.flashback.mapper")
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class FlashbackBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashbackBackendApplication.class, args);
    }
}
