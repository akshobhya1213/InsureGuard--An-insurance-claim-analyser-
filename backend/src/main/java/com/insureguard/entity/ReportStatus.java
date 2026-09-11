package com.insureguard.entity;

/** Lifecycle of a claim report as it moves through analysis. */
public enum ReportStatus {
    SUBMITTED,
    PROCESSING,
    COMPLETED,
    FAILED
}
