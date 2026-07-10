package com.vpsfilebridge.model;

import java.time.Instant;

public record FileItem(
    String name,
    String path,
    boolean directory,
    long size,
    Instant modified,
    String extension
) {}
