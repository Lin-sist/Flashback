package com.flashback.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppStoragePropertiesTest {

    @Test
    void shouldExposeAcceptedDefaultQiniuStorageConfig() {
        AppStorageProperties properties = new AppStorageProperties();

        assertThat(properties.getProvider()).isEqualTo("qiniu");
        assertThat(properties.getProviderType()).isEqualTo(AppStorageProperties.StorageProvider.QINIU);
        assertThat(properties.getQiniu().getAccessKey()).isEmpty();
        assertThat(properties.getQiniu().getSecretKey()).isEmpty();
        assertThat(properties.getQiniu().getBucket()).isEmpty();
        assertThat(properties.getQiniu().getRegion()).isEmpty();
        assertThat(properties.getQiniu().getPrivateDomain()).isEmpty();
        assertThat(properties.getQiniu().getUploadTokenTtlSeconds()).isEqualTo(600);
        assertThat(properties.getQiniu().getDownloadUrlTtlSeconds()).isEqualTo(600);
        assertThat(properties.getQiniu().getKeyPrefix()).isEqualTo("flashback");
        assertThat(properties.getQiniu().isConfigured()).isFalse();
    }

    @Test
    void shouldResolveAcceptedStorageProviderValues() {
        assertThat(AppStorageProperties.StorageProvider.fromConfigValue("qiniu"))
                .isEqualTo(AppStorageProperties.StorageProvider.QINIU);
        assertThat(AppStorageProperties.StorageProvider.fromConfigValue("QINIU"))
                .isEqualTo(AppStorageProperties.StorageProvider.QINIU);
        assertThat(AppStorageProperties.StorageProvider.fromConfigValue(""))
                .isEqualTo(AppStorageProperties.StorageProvider.QINIU);
    }
}
