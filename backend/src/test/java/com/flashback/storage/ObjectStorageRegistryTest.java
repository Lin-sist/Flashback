package com.flashback.storage;

import com.flashback.config.AppStorageProperties;
import com.flashback.domain.StorageProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStorageRegistryTest {

    @Test
    void shouldResolveAliasToConfiguredS3Provider() {
        AppStorageProperties properties = new AppStorageProperties();
        properties.setProvider("aliyun-oss");
        StubProvider qiniu = new StubProvider(StorageProvider.QINIU, true);
        StubProvider s3 = new StubProvider(StorageProvider.S3_COMPATIBLE, true);

        ObjectStorageRegistry registry = new ObjectStorageRegistry(properties, List.of(qiniu, s3));

        assertThat(registry.getActiveProvider()).isSameAs(s3);
        assertThat(registry.getRequired(StorageProvider.QINIU)).isSameAs(qiniu);
    }

    @Test
    void shouldRejectPersistedProviderWhenItsCredentialsAreNotConfigured() {
        AppStorageProperties properties = new AppStorageProperties();
        ObjectStorageRegistry registry = new ObjectStorageRegistry(
                properties,
                List.of(new StubProvider(StorageProvider.QINIU, false)));

        assertThatThrownBy(() -> registry.getRequired(StorageProvider.QINIU))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageContaining("not configured");
    }

    private record StubProvider(StorageProvider provider, boolean configured) implements ObjectStorageProvider {
        @Override public StorageProvider getProvider() { return provider; }
        @Override public boolean isConfigured() { return configured; }
        @Override public String getBucket() { return "bucket"; }
        @Override public String getKeyPrefix() { return "flashback"; }
        @Override public long getUploadAuthorizationTtlSeconds() { return 600; }
        @Override public long getDownloadUrlTtlSeconds() { return 600; }
        @Override public ObjectStorageUploadAuthorization createUploadAuthorization(String key, String mimeType, long sizeBytes, Instant expiresAt) { return null; }
        @Override public ObjectStorageMetadata statObject(String bucket, String key) { return null; }
        @Override public void deleteObject(String bucket, String key) { }
        @Override public String createPrivateAccessUrl(String bucket, String key, Instant expiresAt) { return null; }
    }
}
