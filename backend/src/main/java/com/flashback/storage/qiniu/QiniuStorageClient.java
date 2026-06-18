package com.flashback.storage.qiniu;

public interface QiniuStorageClient {

    QiniuObjectMetadata statObject(String bucket, String key);

    void deleteObject(String bucket, String key);
}
