package com.flashback.storage.qiniu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Minimal Qiniu Kodo HTTP client for M4 object stat verification.
 */
@Component
public class QiniuHttpStorageClient implements QiniuStorageClient {

    private static final String RS_HOST = "https://rs.qiniu.com";

    private final AppStorageProperties appStorageProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public QiniuHttpStorageClient(AppStorageProperties appStorageProperties) {
        this(appStorageProperties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    QiniuHttpStorageClient(
            AppStorageProperties appStorageProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.appStorageProperties = appStorageProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public QiniuObjectMetadata statObject(String bucket, String key) {
        AppStorageProperties.Qiniu qiniu = appStorageProperties.getQiniu();
        String path = "/stat/" + encodedEntryUri(bucket, key);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RS_HOST + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "QBox " + managementToken(qiniu, path))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 612) {
                throw new QiniuStorageException("qiniu object not found", true);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new QiniuStorageException("qiniu stat failed: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            QiniuObjectMetadata metadata = new QiniuObjectMetadata();
            JsonNode sizeNode = root.path("fsize");
            if (sizeNode.canConvertToLong()) {
                metadata.setSizeBytes(sizeNode.asLong());
            }
            JsonNode mimeNode = root.path("mimeType");
            if (mimeNode.isTextual()) {
                metadata.setMimeType(mimeNode.asText());
            }
            return metadata;
        } catch (QiniuStorageException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new QiniuStorageException("qiniu stat response invalid", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new QiniuStorageException("qiniu stat interrupted", ex);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        AppStorageProperties.Qiniu qiniu = appStorageProperties.getQiniu();
        String path = "/delete/" + encodedEntryUri(bucket, key);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RS_HOST + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "QBox " + managementToken(qiniu, path))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 612) {
                throw new QiniuStorageException("qiniu object not found", true);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new QiniuStorageException("qiniu delete failed: HTTP " + response.statusCode());
            }
        } catch (QiniuStorageException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new QiniuStorageException("qiniu delete interrupted", ex);
        } catch (IOException ex) {
            throw new QiniuStorageException("qiniu delete failed", ex);
        }
    }

    private String encodedEntryUri(String bucket, String key) {
        return urlSafeBase64((bucket + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private String managementToken(AppStorageProperties.Qiniu qiniu, String path) {
        try {
            String signingStr = path + "\n";
            String encodedSign = hmacSha1(qiniu.getSecretKey().trim(), signingStr);
            return qiniu.getAccessKey().trim() + ":" + encodedSign;
        } catch (Exception ex) {
            throw new QiniuStorageException("qiniu management token failed", ex);
        }
    }

    private String hmacSha1(String secretKey, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return urlSafeBase64(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String urlSafeBase64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
