package com.flashback.service.data;

import com.flashback.domain.RecordAttachment;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;

@Component
public class PrivateObjectContentReader {
    private final ObjectStorageRegistry registry;
    private final Clock clock;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public PrivateObjectContentReader(ObjectStorageRegistry registry, Clock clock) { this.registry = registry; this.clock = clock; }
    public byte[] read(RecordAttachment attachment) {
        try {
            ObjectStorageProvider provider = registry.getRequired(attachment.getStorageProvider());
            String url = provider.createPrivateAccessUrl(attachment.getBucket(), attachment.getStorageKey(), clock.instant().plusSeconds(60));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new ObjectStorageException("private object read failed");
            return response.body();
        } catch (ObjectStorageException ex) { throw ex; }
        catch (Exception ex) { throw new ObjectStorageException("private object read failed", ex); }
    }
}
