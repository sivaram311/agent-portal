package com.agentportal.domain;

public enum SessionStatus {
    IDLE,
    STREAMING,
    WAITING_PERMISSION,
    WAITING_PLAN,
    COMPLETED,
    FAILED,
    CANCELLED,
    ARCHIVED
}
