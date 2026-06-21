package com.flashback.storage;

public class ObjectStorageException extends RuntimeException {

    private final boolean notFound;

    public ObjectStorageException(String message) {
        this(message, null, false);
    }

    public ObjectStorageException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public ObjectStorageException(String message, boolean notFound) {
        this(message, null, notFound);
    }

    public ObjectStorageException(String message, Throwable cause, boolean notFound) {
        super(message, cause);
        this.notFound = notFound;
    }

    public boolean isNotFound() { return notFound; }
}
