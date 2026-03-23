package com.flashback;

import com.flashback.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.flashback.mapper")
@EnableConfigurationProperties(JwtProperties.class)
public class FlashbackBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashbackBackendApplication.class, args);
    }
}
