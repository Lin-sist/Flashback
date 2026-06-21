package com.flashback.storage;

import com.flashback.config.AppStorageProperties;
import com.flashback.domain.StorageProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ObjectStorageRegistry {

    private final AppStorageProperties properties;
    private final Map<StorageProvider, ObjectStorageProvider> providers = new EnumMap<>(StorageProvider.class);

    public ObjectStorageRegistry(AppStorageProperties properties, List<ObjectStorageProvider> providerList) {
        this.properties = properties;
        for (ObjectStorageProvider provider : providerList) {
            providers.put(provider.getProvider(), provider);
        }
    }

    public ObjectStorageProvider getActiveProvider() {
        try {
            return getRequired(properties.getProviderType());
        } catch (IllegalArgumentException ex) {
            throw new ObjectStorageException("storage provider unsupported", ex);
        }
    }

    public ObjectStorageProvider getRequired(StorageProvider provider) {
        ObjectStorageProvider implementation = providers.get(provider);
        if (implementation == null || !implementation.isConfigured()) {
            throw new ObjectStorageException("storage provider not configured: " + provider);
        }
        return implementation;
    }
}
