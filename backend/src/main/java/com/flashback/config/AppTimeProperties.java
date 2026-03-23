package com.flashback.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 业务时间配置。
 *
 * 关键约定：所有业务当前时间都应基于此处配置的时区生成 Clock，避免依赖部署机默认时区。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.time")
public class AppTimeProperties {

    @NotBlank
    private String zoneId = "Asia/Shanghai";

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
