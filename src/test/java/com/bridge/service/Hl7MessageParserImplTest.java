package com.bridge.service;

import com.bridge.model.Hl7Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Hl7MessageParserImpl
 */
class Hl7MessageParserImplTest {
    
    private Hl7MessageParserImpl parser;
    
    // Sample valid HL7 ADT message
    private static final String VALID_ADT_MESSAGE = 
        "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
        "PID|1||123456789^^^MRN||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345||(555)123-4567|||S||987654321||||||||||||\r" +
        "PV1|1|I|ICU^101^1|||DOCTOR123^SMITH^JANE|||SUR||||A|||DOCTOR123^SMITH^JANE|INP|CAT|||||||||||||||||||||||||20240715120000";
    
    // Sample valid HL7 ORM message
    private static final String VALID_ORM_MESSAGE = 
        "MSH|^~\\&|LAB_SYSTEM|LAB_FACILITY|EMR_SYSTEM|HOSPITAL|20240715120000||ORM^O01|67890|P|2.4\r" +
        "PID|1||987654321^^^MRN||PATIENT^TEST^MIDDLE||19750615|F|||456 OAK AVE^^TESTCITY^CA^90210||(555)987-6543|||M||123456789||||||||||||\r" +
        "ORC|NW|ORDER123|ORDER123|GROUP123||E||20240715120000|||DOCTOR456^JONES^ROBERT\r" +
        "OBR|1|ORDER123|ORDER123|CBC^COMPLETE BLOOD COUNT^L|||20240715120000||||||||DOCTOR456^JONES^ROBERT";
    
    // Sample valid HL7 ORU message
    private static final String VALID_ORU_MESSAGE = 
        "MSH|^~\\&|LAB_SYSTEM|LAB_FACILITY|EMR_SYSTEM|HOSPITAL|20240715120000||ORU^R01|78901|P|2.4\r" +
        "PID|1||111222333^^^MRN||SMITH^JANE^ANN||19850320|F|||789 ELM ST^^HOMETOWN^NY^10001||(555)111-2222|||M||456789123||||||||||||\r" +
        "OBR|1|LAB001|LAB001|CBC^COMPLETE BLOOD COUNT^L|||20240715120000||||||||LAB_TECH^DOE^JOHN\r" +
        "OBX|1|NM|WBC^WHITE BLOOD COUNT^L||7.5|10*3/uL|4.0-11.0||||F|||20240715120000\r" +
        "OBX|2|NM|RBC^RED BLOOD COUNT^L||4.2|10*6/uL|3.8-5.2||||F|||20240715120000\r" +
        "OBX|3|NM|HGB^HEMOGLOBIN^L||13.5|g/dL|12.0-16.0||||F|||20240715120000";
    
    @BeforeEach
    void setUp() {
        parser = new Hl7MessageParserImpl();
    }
    
    @Test
    @DisplayName("Should parse ADT message successfully")
    void testParseAdtMessage() {
        ParsedHl7Data result = parser.parseMessage(VALID_ADT_MESSAGE);
        
        assertNotNull(result);
        assertEquals("ADT^A01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
        assertEquals("12345", result.getMessageControlId());
        assertEquals("SENDING_APP", result.getSendingApplication());
        assertEquals("RECEIVING_APP", result.getReceivingApplication());
        assertNotNull(result.getMessageTimestamp());
        
        // Verify patient data
        PatientData patientData = result.getPatientData();
        assertNotNull(patientData);
        assertEquals("123456789", patientData.getPatientId());
        assertEquals("123456789", patientData.getMedicalRecordNumber());
        assertEquals("DOE", patientData.getLastName());
        assertEquals("JOHN", patientData.getFirstName());
        assertEquals("MIDDLE", patientData.getMiddleName());
        assertEquals("JOHN MIDDLE DOE", patientData.getFullName());
        assertEquals(LocalDate.of(1980, 1, 1), patientData.getDateOfBirth());
        assertEquals("M", patientData.getGender());
        assertEquals("123 MAIN ST", patientData.getAddress());
        assertEquals("ANYTOWN", patientData.getCity());
        assertEquals("ST", patientData.getState());
        assertEquals("12345", patientData.getZipCode());
        assertEquals("(555)123-4567", patientData.getPhoneNumber());
        assertEquals("S", patientData.getMaritalStatus());
        
        // Verify visit data
        VisitData visitData = result.getVisitData();
        assertNotNull(visitData);
        assertEquals("I", visitData.getPatientClass());
        assertEquals("ICU^101^1", visitData.getAssignedPatientLocation());
        assertEquals("ICU", visitData.getRoom());
        assertEquals("101", visitData.getBed());
        assertEquals("1", visitData.getFacility());
        assertEquals("DOCTOR123^SMITH^JANE", visitData.getAttendingDoctor());
        assertNotNull(visitData.getAdmitDateTime());
    }
    
    @Test
    @DisplayName("Should parse ORM message successfully")
    void testParseOrmMessage() {
        ParsedHl7Data result = parser.parseMessage(VALID_ORM_MESSAGE);
        
        assertNotNull(result);
        assertEquals("ORM^O01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
        assertEquals("67890", result.getMessageControlId());
        
        // Verify patient data
        PatientData patientData = result.getPatientData();
        assertNotNull(patientData);
        assertEquals("987654321", patientData.getPatientId());
        assertEquals("PATIENT", patientData.getLastName());
        assertEquals("TEST", patientData.getFirstName());
        assertEquals("MIDDLE", patientData.getMiddleName());
        assertEquals(LocalDate.of(1975, 6, 15), patientData.getDateOfBirth());
        assertEquals("F", patientData.getGender());
        
        // Verify order data
        assertEquals(1, result.getOrders().size());
        OrderData orderData = result.getOrders().get(0);
        assertEquals("ORDER123", orderData.getOrderNumber());
        assertEquals("ORDER123", orderData.getPlacerOrderNumber());
        assertEquals("ORDER123", orderData.getFillerOrderNumber());
        assertEquals("NW", orderData.getOrderStatus());
        assertEquals("CBC", orderData.getOrderCode());
        assertEquals("COMPLETE BLOOD COUNT", orderData.getOrderName());
        assertEquals("DOCTOR456^JONES^ROBERT", orderData.getOrderingProvider());
        assertNotNull(orderData.getOrderDateTime());
    }
    
    @Test
    @DisplayName("Should parse ORU message successfully")
    void testParseOruMessage() {
        ParsedHl7Data result = parser.parseMessage(VALID_ORU_MESSAGE);
        
        assertNotNull(result);
        assertEquals("ORU^R01", result.getMessageType());
        assertEquals("2.4", result.getMessageVersion());
        assertEquals("78901", result.getMessageControlId());
        
        // Verify patient data
        PatientData patientData = result.getPatientData();
        assertNotNull(patientData);
        assertEquals("111222333", patientData.getPatientId());
        assertEquals("SMITH", patientData.getLastName());
        assertEquals("JANE", patientData.getFirstName());
        assertEquals("ANN", patientData.getMiddleName());
        assertEquals(LocalDate.of(1985, 3, 20), patientData.getDateOfBirth());
        assertEquals("F", patientData.getGender());
        
        // Verify observation data
        assertEquals(3, result.getObservations().size());
        
        ObservationData wbc = result.getObservations().get(0);
        assertEquals("1", wbc.getObservationId());
        assertEquals("WBC", wbc.getObservationCode());
        assertEquals("WHITE BLOOD COUNT", wbc.getObservationName());
        assertEquals("7.5", wbc.getValue());
        assertEquals("10*3/uL", wbc.getUnits());
        assertEquals("4.0-11.0", wbc.getReferenceRange());
        assertEquals("F", wbc.getResultStatus());
        
        ObservationData rbc = result.getObservations().get(1);
        assertEquals("2", rbc.getObservationId());
        assertEquals("RBC", rbc.getObservationCode());
        assertEquals("RED BLOOD COUNT", rbc.getObservationName());
        assertEquals("4.2", rbc.getValue());
        assertEquals("10*6/uL", rbc.getUnits());
        
        ObservationData hgb = result.getObservations().get(2);
        assertEquals("3", hgb.getObservationId());
        assertEquals("HGB", hgb.getObservationCode());
        assertEquals("HEMOGLOBIN", hgb.getObservationName());
        assertEquals("13.5", hgb.getValue());
        assertEquals("g/dL", hgb.getUnits());
    }
    
    @Test
    @DisplayName("Should parse Hl7Message object successfully")
    void testParseHl7MessageObject() {
        Hl7Message message = new Hl7Message();
        message.setRawMessage(VALID_ADT_MESSAGE);
        
        ParsedHl7Data result = parser.parseMessage(message);
        
        assertNotNull(result);
        assertEquals("ADT^A01", result.getMessageType());
        assertNotNull(result.getPatientData());
    }
    
    @Test
    @DisplayName("Should check supported message types correctly")
    void testSupportsMessageType() {
        assertTrue(parser.supportsMessageType("ADT"));
        assertTrue(parser.supportsMessageType("ORM"));
        assertTrue(parser.supportsMessageType("ORU"));
        assertTrue(parser.supportsMessageType("MDM"));
        assertTrue(parser.supportsMessageType("SIU"));
        assertTrue(parser.supportsMessageType("adt")); // Case insensitive
        
        assertFalse(parser.supportsMessageType("ACK"));
        assertFalse(parser.supportsMessageType("QRY"));
        assertFalse(parser.supportsMessageType(null));
        assertFalse(parser.supportsMessageType(""));
    }
    
    @Test
    @DisplayName("Should throw exception for null raw message")
    void testParseNullRawMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseMessage((String) null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for empty raw message")
    void testParseEmptyRawMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseMessage("");
        });
    }
    
    @Test
    @DisplayName("Should throw exception for null Hl7Message object")
    void testParseNullHl7MessageObject() {
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseMessage((Hl7Message) null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for malformed HL7 message")
    void testParseMalformedMessage() {
        String malformedMessage = "INVALID|MESSAGE|FORMAT";
        
        assertThrows(RuntimeException.class, () -> {
            parser.parseMessage(malformedMessage);
        });
    }
    
    @Test
    @DisplayName("Should handle message with minimal data")
    void testParseMinimalMessage() {
        String minimalMessage = "MSH|^~\\&|SEND|FACILITY|RECV|FACILITY|20240715120000||ADT^A01|12345|P|2.4";
        
        ParsedHl7Data result = parser.parseMessage(minimalMessage);
        
        assertNotNull(result);
        assertEquals("ADT^A01", result.getMessageType());
        assertEquals("12345", result.getMessageControlId());
        // Patient data should be null or empty since no PID segment
        PatientData patientData = result.getPatientData();
        if (patientData != null) {
            assertNull(patientData.getPatientId());
        }
    }
    
    @Test
    @DisplayName("Should handle patient name without components")
    void testParsePatientNameWithoutComponents() {
        String messageWithSimpleName = 
            "MSH|^~\\&|SEND|FACILITY|RECV|FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789|||SIMPLETON||||||||||||||||||||||||";
        
        ParsedHl7Data result = parser.parseMessage(messageWithSimpleName);
        
        assertNotNull(result);
        PatientData patientData = result.getPatientData();
        assertNotNull(patientData);
        assertEquals("SIMPLETON", patientData.getLastName());
        assertNull(patientData.getFirstName());
    }
    
    @Test
    @DisplayName("Should handle invalid date formats gracefully")
    void testParseInvalidDateFormats() {
        String messageWithInvalidDate = 
            "MSH|^~\\&|SEND|FACILITY|RECV|FACILITY|20240715120000||ADT^A01|12345|P|2.4\r" +
            "PID|1||123456789|||DOE^JOHN||INVALID_DATE|M||||||||||||||||||||||||";
        
        ParsedHl7Data result = parser.parseMessage(messageWithInvalidDate);
        
        assertNotNull(result);
        PatientData patientData = result.getPatientData();
        assertNotNull(patientData);
        assertNull(patientData.getDateOfBirth()); // Should be null due to invalid format
    }
    
    @Test
    @DisplayName("Should parse message timestamp correctly")
    void testParseMessageTimestamp() {
        ParsedHl7Data result = parser.parseMessage(VALID_ADT_MESSAGE);
        
        assertNotNull(result.getMessageTimestamp());
        assertEquals(LocalDateTime.of(2024, 7, 15, 12, 0, 0), result.getMessageTimestamp());
    }
}