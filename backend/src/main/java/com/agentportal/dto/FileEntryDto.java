package com.agentportal.dto;

public record FileEntryDto(String name, String path, boolean directory, long size) {
}
