package com.flashback.service;

public record DraftCleanupReport(int scanned, int deleted, int retry, int skipped) {
}
