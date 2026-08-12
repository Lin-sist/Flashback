package com.flashback.service.data;

import com.flashback.config.AppDataOwnershipProperties;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataOwnershipArtifactStore {
    private final AppDataOwnershipProperties properties;
    private final Clock clock;
    public DataOwnershipArtifactStore(AppDataOwnershipProperties properties, Clock clock) { this.properties = properties; this.clock = clock; }

    public StoredArtifact save(byte[] content) throws IOException {
        Path directory = Path.of(properties.getArtifactDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String token = UUID.randomUUID().toString();
        Path partial = directory.resolve(token + ".partial");
        Path complete = directory.resolve(token + ".zip");
        try {
            Files.write(partial, content);
            try { Files.move(partial, complete, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(partial, complete, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) {
            Files.deleteIfExists(partial);
            throw ex;
        }
        return new StoredArtifact(token, LocalDateTime.now(clock).plusHours(properties.getArtifactTtlHours()));
    }
    public byte[] read(String token) throws IOException { return Files.readAllBytes(resolve(token)); }
    public void delete(String token) { try { Files.deleteIfExists(resolve(token)); } catch (IOException ignored) { }
    }
    private Path resolve(String token) {
        if (token == null || !token.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("invalid artifact token");
        Path base = Path.of(properties.getArtifactDirectory()).toAbsolutePath().normalize();
        Path file = base.resolve(token + ".zip").normalize();
        if (!file.startsWith(base)) throw new IllegalArgumentException("invalid artifact path");
        return file;
    }
    public record StoredArtifact(String token, LocalDateTime expiresAt) {}
}
