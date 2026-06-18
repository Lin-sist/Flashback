package com.flashback.storage.qiniu;

public interface QiniuStorageClient {

    QiniuObjectMetadata statObject(String bucket, String key);
}
