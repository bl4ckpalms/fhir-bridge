package com.bridge.service;

import com.bridge.model.Hl7Message;

/**
 * Interface for parsing HL7 v2 messages and extracting clinical data
 */
public interface Hl7MessageParser {
    
    /**
     * Parse a raw HL7 v2 message string and extract clinical data
     * 
     * @param rawMessage the raw HL7 v2 message string
     * @return ParsedHl7Data containing extracted patient and clinical data
     * @throws IllegalArgumentException if rawMessage is null or empty
     * @throws RuntimeException if message parsing fails
     */
    ParsedHl7Data parseMessage(String rawMessage);
    
    /**
     * Parse an Hl7Message object and extract clinical data
     * 
     * @param message the Hl7Message object containing the raw message
     * @return ParsedHl7Data containing extracted patient and clinical data
     * @throws IllegalArgumentException if message is null
     * @throws RuntimeException if message parsing fails
     */
    ParsedHl7Data parseMessage(Hl7Message message);
    
    /**
     * Check if the parser supports a specific HL7 message type
     * 
     * @param messageType the HL7 message type (e.g., "ADT", "ORM", "ORU")
     * @return true if the message type is supported, false otherwise
     */
    boolean supportsMessageType(String messageType);
}