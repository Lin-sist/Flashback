package com.flashback.storage;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObjectStorageUploadAuthorization {

    private String method;
    private String uploadUrl;
    private String fileFieldName = "file";
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> formData = new LinkedHashMap<>();

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
    public String getFileFieldName() { return fileFieldName; }
    public void setFileFieldName(String fileFieldName) { this.fileFieldName = fileFieldName; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public Map<String, String> getFormData() { return formData; }
    public void setFormData(Map<String, String> formData) { this.formData = formData; }
}
