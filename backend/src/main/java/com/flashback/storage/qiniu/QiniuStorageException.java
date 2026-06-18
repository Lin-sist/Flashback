package com.flashback.storage.qiniu;

public class QiniuStorageException extends RuntimeException {

    private final boolean notFound;

    public QiniuStorageException(String message) {
        this(message, false, null);
    }

    public QiniuStorageException(String message, boolean notFound) {
        this(message, notFound, null);
    }

    public QiniuStorageException(String message, Throwable cause) {
        this(message, false, cause);
    }

    private QiniuStorageException(String message, boolean notFound, Throwable cause) {
        super(message, cause);
        this.notFound = notFound;
    }

    public boolean isNotFound() {
        return notFound;
    }
}
