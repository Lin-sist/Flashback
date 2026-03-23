package com.flashback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 提供统一时间源。
 * 业务层应注入 Clock 获取当前时间，避免直接散落调用 now() 导致测试困难。
 */
@Configuration
public class TimeConfig {

    private final AppTimeProperties appTimeProperties;

    public TimeConfig(AppTimeProperties appTimeProperties) {
        this.appTimeProperties = appTimeProperties;
    }

    @Bean
    public Clock systemClock() {
        return Clock.system(ZoneId.of(appTimeProperties.getZoneId()));
    }
}
