package com.bridge.model;

/**
 * Enumeration for HL7 message processing status
 */
public enum MessageStatus {
    RECEIVED,
    VALIDATING,
    VALID,
    INVALID,
    TRANSFORMING,
    TRANSFORMED,
    ERROR
}