package com.vpsfilebridge.model;

public record StorageStats(long totalBytes, long usedBytes, long freeBytes, String rootPath) {}
