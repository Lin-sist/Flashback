package com.flashback.config;

import com.flashback.domain.StorageProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppStoragePropertiesTest {

    @Test
    void shouldExposeAcceptedDefaultQiniuStorageConfig() {
        AppStorageProperties properties = new AppStorageProperties();

        assertThat(properties.getProvider()).isEqualTo("qiniu");
        assertThat(properties.getProviderType()).isEqualTo(StorageProvider.QINIU);
        assertThat(properties.getQiniu().getAccessKey()).isEmpty();
        assertThat(properties.getQiniu().getSecretKey()).isEmpty();
        assertThat(properties.getQiniu().getBucket()).isEmpty();
        assertThat(properties.getQiniu().getRegion()).isEmpty();
        assertThat(properties.getQiniu().getPrivateDomain()).isEmpty();
        assertThat(properties.getQiniu().getUploadTokenTtlSeconds()).isEqualTo(600);
        assertThat(properties.getQiniu().getDownloadUrlTtlSeconds()).isEqualTo(600);
        assertThat(properties.getQiniu().getKeyPrefix()).isEqualTo("flashback");
        assertThat(properties.getQiniu().isConfigured()).isFalse();
        assertThat(properties.getS3().isConfigured()).isFalse();
        assertThat(properties.getS3().getUploadTokenTtlSeconds()).isEqualTo(600);
        assertThat(properties.getS3().getDownloadUrlTtlSeconds()).isEqualTo(600);
    }

    @Test
    void shouldResolveAcceptedStorageProviderValues() {
        assertThat(StorageProvider.fromConfigValue("qiniu")).isEqualTo(StorageProvider.QINIU);
        assertThat(StorageProvider.fromConfigValue("QINIU")).isEqualTo(StorageProvider.QINIU);
        assertThat(StorageProvider.fromConfigValue("")).isEqualTo(StorageProvider.QINIU);
        assertThat(StorageProvider.fromConfigValue("s3-compatible")).isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(StorageProvider.fromConfigValue("aliyun-oss")).isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(StorageProvider.fromConfigValue("tencent-cos")).isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(StorageProvider.fromConfigValue("minio")).isEqualTo(StorageProvider.S3_COMPATIBLE);
    }
}
