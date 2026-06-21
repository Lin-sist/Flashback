package com.flashback.storage.s3;

import com.flashback.config.AppStorageProperties;
import com.flashback.domain.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class S3CompatibleObjectStorageProviderTest {

    private S3CompatibleObjectStorageProvider provider;

    @BeforeEach
    void setUp() {
        AppStorageProperties properties = new AppStorageProperties();
        properties.getS3().setEndpoint("https://s3.example.com");
        properties.getS3().setRegion("ap-east-1");
        properties.getS3().setAccessKey("test-access-key");
        properties.getS3().setSecretKey("test-secret-key");
        properties.getS3().setBucket("flashback-private");
        provider = new S3CompatibleObjectStorageProvider(properties);
    }

    @Test
    void shouldCreateSigV4PutAuthorizationWithoutExposingSecret() {
        var result = provider.createUploadAuthorization(
                "flashback/users/1/records/2/image/test.jpg",
                "image/jpeg",
                1024,
                Instant.now().plusSeconds(600));

        assertThat(provider.getProvider()).isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(provider.isConfigured()).isTrue();
        assertThat(result.getMethod()).isEqualTo("PUT");
        assertThat(result.getUploadUrl())
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-Signature=")
                .doesNotContain("test-secret-key");
        assertThat(result.getHeaders()).containsEntry("Content-Type", "image/jpeg");
        assertThat(result.getFormData()).isEmpty();
    }

    @Test
    void shouldCreatePrivatePresignedGetUrl() {
        String url = provider.createPrivateAccessUrl(
                "flashback-private",
                "flashback/users/1/records/2/voice/test.mp3",
                Instant.now().plusSeconds(600));

        assertThat(url)
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-Signature=")
                .doesNotContain("test-secret-key");
    }
}
