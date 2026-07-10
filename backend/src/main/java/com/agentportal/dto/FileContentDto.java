package com.agentportal.dto;

public record FileContentDto(String path, String content, String mediaType, boolean truncated) {
}
