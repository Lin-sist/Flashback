package com.flashback.storage.s3;

import com.flashback.config.AppStorageProperties;
import com.flashback.domain.StorageProvider;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageMetadata;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageUploadAuthorization;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class S3CompatibleObjectStorageProvider implements ObjectStorageProvider {

    private final AppStorageProperties properties;

    public S3CompatibleObjectStorageProvider(AppStorageProperties properties) {
        this.properties = properties;
    }

    @Override public StorageProvider getProvider() { return StorageProvider.S3_COMPATIBLE; }
    @Override public boolean isConfigured() { return properties.getS3().isConfigured(); }
    @Override public String getBucket() { return properties.getS3().getBucket().trim(); }
    @Override public String getKeyPrefix() { return properties.getS3().getKeyPrefix(); }
    @Override public long getUploadAuthorizationTtlSeconds() { return properties.getS3().getUploadTokenTtlSeconds(); }
    @Override public long getDownloadUrlTtlSeconds() { return properties.getS3().getDownloadUrlTtlSeconds(); }

    @Override
    public ObjectStorageUploadAuthorization createUploadAuthorization(
            String key, String mimeType, long sizeBytes, Instant expiresAt) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(getBucket())
                .key(key)
                .contentType(mimeType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(signatureDuration(expiresAt))
                .putObjectRequest(putRequest)
                .build();
        try (S3Presigner presigner = createPresigner()) {
            PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
            ObjectStorageUploadAuthorization authorization = new ObjectStorageUploadAuthorization();
            authorization.setMethod("PUT");
            authorization.setUploadUrl(presigned.url().toString());
            authorization.setHeaders(flattenUploadHeaders(presigned.signedHeaders()));
            authorization.setFormData(Map.of());
            return authorization;
        } catch (RuntimeException ex) {
            throw new ObjectStorageException("s3 upload authorization failed", ex);
        }
    }

    @Override
    public ObjectStorageMetadata statObject(String bucket, String key) {
        try (S3Client client = createClient()) {
            HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            ObjectStorageMetadata metadata = new ObjectStorageMetadata();
            metadata.setSizeBytes(response.contentLength());
            metadata.setMimeType(response.contentType());
            return metadata;
        } catch (S3Exception ex) {
            throw mapException("s3 stat failed", ex);
        } catch (RuntimeException ex) {
            throw new ObjectStorageException("s3 stat failed", ex);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try (S3Client client = createClient()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            throw mapException("s3 delete failed", ex);
        } catch (RuntimeException ex) {
            throw new ObjectStorageException("s3 delete failed", ex);
        }
    }

    @Override
    public String createPrivateAccessUrl(String bucket, String key, Instant expiresAt) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration(expiresAt))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        try (S3Presigner presigner = createPresigner()) {
            PresignedGetObjectRequest presigned = presigner.presignGetObject(request);
            return presigned.url().toString();
        } catch (RuntimeException ex) {
            throw new ObjectStorageException("s3 access url failed", ex);
        }
    }

    private S3Client createClient() {
        AppStorageProperties.S3 s3 = properties.getS3();
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials(s3)))
                .region(Region.of(s3.getRegion().trim()))
                .endpointOverride(endpoint(s3))
                .serviceConfiguration(serviceConfiguration(s3))
                .build();
    }

    private S3Presigner createPresigner() {
        AppStorageProperties.S3 s3 = properties.getS3();
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials(s3)))
                .region(Region.of(s3.getRegion().trim()))
                .endpointOverride(endpoint(s3))
                .serviceConfiguration(serviceConfiguration(s3))
                .build();
    }

    private AwsCredentials credentials(AppStorageProperties.S3 s3) {
        if (s3.getSessionToken() != null && !s3.getSessionToken().trim().isEmpty()) {
            return AwsSessionCredentials.create(
                    s3.getAccessKey().trim(),
                    s3.getSecretKey().trim(),
                    s3.getSessionToken().trim());
        }
        return AwsBasicCredentials.create(s3.getAccessKey().trim(), s3.getSecretKey().trim());
    }

    private URI endpoint(AppStorageProperties.S3 s3) {
        try {
            return URI.create(s3.getEndpoint().trim());
        } catch (IllegalArgumentException ex) {
            throw new ObjectStorageException("s3 endpoint invalid", ex);
        }
    }

    private S3Configuration serviceConfiguration(AppStorageProperties.S3 s3) {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(s3.isPathStyleAccess())
                .chunkedEncodingEnabled(false)
                .checksumValidationEnabled(false)
                .build();
    }

    private Duration signatureDuration(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        return duration.isNegative() || duration.isZero() ? Duration.ofSeconds(1) : duration;
    }

    private Map<String, String> flattenUploadHeaders(Map<String, List<String>> signedHeaders) {
        Map<String, String> result = new LinkedHashMap<>();
        signedHeaders.forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name) && values != null && !values.isEmpty()) {
                String headerName = "content-type".equalsIgnoreCase(name) ? "Content-Type" : name;
                result.put(headerName, String.join(",", values));
            }
        });
        return result;
    }

    private ObjectStorageException mapException(String message, S3Exception ex) {
        return new ObjectStorageException(message, ex, ex.statusCode() == 404);
    }
}
