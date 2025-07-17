package com.bridge.service;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import com.bridge.model.Hl7Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of HL7 v2 message parser using HAPI HL7 library
 */
@Service
public class Hl7MessageParserImpl implements Hl7MessageParser {
    
    private static final Logger logger = LoggerFactory.getLogger(Hl7MessageParserImpl.class);
    
    private final HapiContext hapiContext;
    private final PipeParser parser;
    
    // Supported message types
    private static final Set<String> SUPPORTED_MESSAGE_TYPES = new HashSet<>(
        Arrays.asList("ADT", "ORM", "ORU", "MDM", "SIU")
    );
    
    // Date/time formatters for HL7 timestamps
    private static final DateTimeFormatter HL7_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter HL7_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public Hl7MessageParserImpl() {
        this.hapiContext = new DefaultHapiContext();
        this.parser = new PipeParser();
        
        // Use no validation for parsing - we just want to extract data
        ca.uhn.hl7v2.validation.impl.NoValidation noValidation = new ca.uhn.hl7v2.validation.impl.NoValidation();
        this.hapiContext.setValidationContext(noValidation);
        this.parser.setValidationContext(noValidation);
    }
    
    @Override
    public ParsedHl7Data parseMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw message cannot be null or empty");
        }
        
        try {
            Message message = parser.parse(rawMessage);
            return extractDataFromMessage(message);
        } catch (HL7Exception e) {
            logger.error("Failed to parse HL7 message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse HL7 message", e);
        }
    }
    
    @Override
    public ParsedHl7Data parseMessage(Hl7Message message) {
        if (message == null) {
            throw new IllegalArgumentException("HL7 message cannot be null");
        }
        
        return parseMessage(message.getRawMessage());
    }
    
    @Override
    public boolean supportsMessageType(String messageType) {
        return messageType != null && SUPPORTED_MESSAGE_TYPES.contains(messageType.toUpperCase());
    }
    
    private ParsedHl7Data extractDataFromMessage(Message message) throws HL7Exception {
        ParsedHl7Data parsedData = new ParsedHl7Data();
        Terser terser = new Terser(message);
        
        // Extract MSH (Message Header) data
        extractMessageHeaderData(terser, parsedData);
        
        // Extract patient data from PID segment
        extractPatientData(terser, parsedData);
        
        // Extract visit data from PV1 segment
        extractVisitData(terser, parsedData);
        
        // Extract message-type specific data
        String messageType = parsedData.getMessageType();
        if (messageType != null) {
            String baseMessageType = messageType.split("\\^")[0]; // Get base type (e.g., "ADT" from "ADT^A01")
            
            switch (baseMessageType) {
                case "ORM":
                    extractOrderData(terser, parsedData);
                    break;
                case "ORU":
                    extractObservationData(terser, parsedData);
                    break;
                case "ADT":
                    // ADT messages primarily contain patient and visit data, which we've already extracted
                    break;
                default:
                    logger.debug("No specific extraction logic for message type: {}", baseMessageType);
            }
        }
        
        return parsedData;
    }
    
    private void extractMessageHeaderData(Terser terser, ParsedHl7Data parsedData) throws HL7Exception {
        try {
            String messageType = terser.get("MSH-9-1");
            String triggerEvent = terser.get("MSH-9-2");
            if (messageType != null) {
                parsedData.setMessageType(triggerEvent != null ? messageType + "^" + triggerEvent : messageType);
            }
            
            parsedData.setMessageVersion(terser.get("MSH-12"));
            parsedData.setMessageControlId(terser.get("MSH-10"));
            parsedData.setSendingApplication(terser.get("MSH-3"));
            parsedData.setReceivingApplication(terser.get("MSH-5"));
            
            String timestamp = terser.get("MSH-7");
            if (timestamp != null && !timestamp.isEmpty()) {
                parsedData.setMessageTimestamp(parseHl7DateTime(timestamp));
            }
        } catch (HL7Exception e) {
            logger.warn("Error extracting message header data: {}", e.getMessage());
        }
    }
    
    private void extractPatientData(Terser terser, ParsedHl7Data parsedData) {
        try {
            PatientData patientData = new PatientData();
            
            // Try different ways to access PID segment
            String patientId = null;
            String patientIdFull = null;
            try {
                patientId = terser.get("/.PID-3-1"); // Try with leading slash
                patientIdFull = terser.get("/.PID-3");
            } catch (HL7Exception e) {
                try {
                    patientId = terser.get("PID-3-1"); // Try without leading slash
                    patientIdFull = terser.get("PID-3");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-3 field: {}", e2.getMessage());
                }
            }
            
            if (patientIdFull != null && !patientIdFull.trim().isEmpty()) {
                patientData.setPatientId(patientIdFull);
                patientData.setMedicalRecordNumber(patientId != null ? patientId : patientIdFull);
            } else if (patientId != null && !patientId.trim().isEmpty()) {
                patientData.setPatientId(patientId);
                patientData.setMedicalRecordNumber(patientId);
            }
            
            // Patient name (PID-5) - need to get the full field value, not just first component
            String patientName = null;
            try {
                // Try to get the full field by constructing it from components
                String lastName = null;
                String firstName = null;
                String middleName = null;
                
                try {
                    lastName = terser.get("/.PID-5-1");
                    firstName = terser.get("/.PID-5-2");
                    middleName = terser.get("/.PID-5-3");
                } catch (HL7Exception e) {
                    try {
                        lastName = terser.get("PID-5-1");
                        firstName = terser.get("PID-5-2");
                        middleName = terser.get("PID-5-3");
                    } catch (HL7Exception e2) {
                        logger.debug("Could not access PID-5 components: {}", e2.getMessage());
                    }
                }
                
                // Reconstruct the full name field
                StringBuilder nameBuilder = new StringBuilder();
                if (lastName != null && !lastName.trim().isEmpty()) {
                    nameBuilder.append(lastName.trim());
                }
                if (firstName != null && !firstName.trim().isEmpty()) {
                    if (nameBuilder.length() > 0) nameBuilder.append("^");
                    nameBuilder.append(firstName.trim());
                }
                if (middleName != null && !middleName.trim().isEmpty()) {
                    if (nameBuilder.length() > 0 && !nameBuilder.toString().endsWith("^")) nameBuilder.append("^");
                    if (firstName == null || firstName.trim().isEmpty()) nameBuilder.append("^");
                    nameBuilder.append(middleName.trim());
                }
                
                if (nameBuilder.length() > 0) {
                    patientName = nameBuilder.toString();
                    logger.debug("Reconstructed patient name: {}", patientName);
                }
            } catch (Exception e) {
                logger.info("Could not reconstruct patient name: {}", e.getMessage());
            }
            
            if (patientName != null && !patientName.isEmpty()) {
                parsePatientName(patientName, patientData);
            }
            
            // Date of birth
            String dob = null;
            try {
                dob = terser.get("/.PID-7");
            } catch (HL7Exception e) {
                try {
                    dob = terser.get("PID-7");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-7 field: {}", e2.getMessage());
                }
            }
            
            if (dob != null && !dob.isEmpty()) {
                patientData.setDateOfBirth(parseHl7Date(dob));
            }
            
            // Gender
            String gender = null;
            try {
                gender = terser.get("/.PID-8");
            } catch (HL7Exception e) {
                try {
                    gender = terser.get("PID-8");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-8 field: {}", e2.getMessage());
                }
            }
            patientData.setGender(gender);
            
            // Address (PID-11) - need to get the full field value, not just first component
            String address = null;
            try {
                // Try to get the full field by constructing it from components
                String streetAddress = null;
                String otherDesignation = null;
                String city = null;
                String state = null;
                String zipCode = null;
                
                try {
                    streetAddress = terser.get("/.PID-11-1");
                    otherDesignation = terser.get("/.PID-11-2");
                    city = terser.get("/.PID-11-3");
                    state = terser.get("/.PID-11-4");
                    zipCode = terser.get("/.PID-11-5");
                } catch (HL7Exception e) {
                    try {
                        streetAddress = terser.get("PID-11-1");
                        otherDesignation = terser.get("PID-11-2");
                        city = terser.get("PID-11-3");
                        state = terser.get("PID-11-4");
                        zipCode = terser.get("PID-11-5");
                    } catch (HL7Exception e2) {
                        logger.debug("Could not access PID-11 components: {}", e2.getMessage());
                    }
                }
                
                // Reconstruct the full address field maintaining proper component structure
                StringBuilder addressBuilder = new StringBuilder();
                
                // Component 1: Street address
                if (streetAddress != null && !streetAddress.trim().isEmpty()) {
                    addressBuilder.append(streetAddress.trim());
                }
                
                // Component 2: Other designation (usually empty)
                addressBuilder.append("^");
                if (otherDesignation != null && !otherDesignation.trim().isEmpty()) {
                    addressBuilder.append(otherDesignation.trim());
                }
                
                // Component 3: City
                addressBuilder.append("^");
                if (city != null && !city.trim().isEmpty()) {
                    addressBuilder.append(city.trim());
                }
                
                // Component 4: State
                addressBuilder.append("^");
                if (state != null && !state.trim().isEmpty()) {
                    addressBuilder.append(state.trim());
                }
                
                // Component 5: Zip code
                addressBuilder.append("^");
                if (zipCode != null && !zipCode.trim().isEmpty()) {
                    addressBuilder.append(zipCode.trim());
                }
                
                if (addressBuilder.length() > 0) {
                    address = addressBuilder.toString();
                    logger.debug("Reconstructed address: {}", address);
                }
            } catch (Exception e) {
                logger.debug("Could not reconstruct address: {}", e.getMessage());
            }
            
            if (address != null && !address.isEmpty()) {
                parsePatientAddress(address, patientData);
            }
            
            // Phone number
            String phone = null;
            try {
                phone = terser.get("/.PID-13");
            } catch (HL7Exception e) {
                try {
                    phone = terser.get("PID-13");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-13 field: {}", e2.getMessage());
                }
            }
            patientData.setPhoneNumber(phone);
            
            // Marital status
            String maritalStatus = null;
            try {
                maritalStatus = terser.get("/.PID-16");
            } catch (HL7Exception e) {
                try {
                    maritalStatus = terser.get("PID-16");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-16 field: {}", e2.getMessage());
                }
            }
            patientData.setMaritalStatus(maritalStatus);
            
            // Race
            String race = null;
            try {
                race = terser.get("/.PID-10");
            } catch (HL7Exception e) {
                try {
                    race = terser.get("PID-10");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PID-10 field: {}", e2.getMessage());
                }
            }
            patientData.setRace(race);
            
            // Only set patient data if we have at least some patient information
            if (patientData.getPatientId() != null || patientData.getFirstName() != null || patientData.getLastName() != null) {
                parsedData.setPatientData(patientData);
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting patient data: {}", e.getMessage());
        }
    }
    
    private void extractVisitData(Terser terser, ParsedHl7Data parsedData) {
        try {
            VisitData visitData = new VisitData();
            
            // Visit number
            String visitNumber = null;
            try {
                visitNumber = terser.get("/.PV1-19");
            } catch (HL7Exception e) {
                try {
                    visitNumber = terser.get("PV1-19");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-19 field: {}", e2.getMessage());
                }
            }
            visitData.setVisitNumber(visitNumber);
            
            // Patient class
            String patientClass = null;
            try {
                patientClass = terser.get("/.PV1-2");
            } catch (HL7Exception e) {
                try {
                    patientClass = terser.get("PV1-2");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-2 field: {}", e2.getMessage());
                }
            }
            visitData.setPatientClass(patientClass);
            
            // Assigned patient location
            String location = null;
            try {
                location = terser.get("/.PV1-3");
            } catch (HL7Exception e) {
                try {
                    location = terser.get("PV1-3");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-3 field: {}", e2.getMessage());
                }
            }
            
            if (location != null && !location.isEmpty()) {
                visitData.setAssignedPatientLocation(location);
                parsePatientLocation(location, visitData);
            }
            
            // Admission type
            String admissionType = null;
            try {
                admissionType = terser.get("/.PV1-4");
            } catch (HL7Exception e) {
                try {
                    admissionType = terser.get("PV1-4");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-4 field: {}", e2.getMessage());
                }
            }
            visitData.setAdmissionType(admissionType);
            
            // Hospital service
            String hospitalService = null;
            try {
                hospitalService = terser.get("/.PV1-10");
            } catch (HL7Exception e) {
                try {
                    hospitalService = terser.get("PV1-10");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-10 field: {}", e2.getMessage());
                }
            }
            visitData.setHospitalService(hospitalService);
            
            // Admit date/time
            String admitDateTime = null;
            try {
                admitDateTime = terser.get("/.PV1-44");
            } catch (HL7Exception e) {
                try {
                    admitDateTime = terser.get("PV1-44");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-44 field: {}", e2.getMessage());
                }
            }
            
            if (admitDateTime != null && !admitDateTime.isEmpty()) {
                visitData.setAdmitDateTime(parseHl7DateTime(admitDateTime));
            }
            
            // Discharge date/time
            String dischargeDateTime = null;
            try {
                dischargeDateTime = terser.get("/.PV1-45");
            } catch (HL7Exception e) {
                try {
                    dischargeDateTime = terser.get("PV1-45");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-45 field: {}", e2.getMessage());
                }
            }
            
            if (dischargeDateTime != null && !dischargeDateTime.isEmpty()) {
                visitData.setDischargeDateTime(parseHl7DateTime(dischargeDateTime));
            }
            
            // Attending doctor
            String attendingDoctor = null;
            try {
                attendingDoctor = terser.get("/.PV1-7");
            } catch (HL7Exception e) {
                try {
                    attendingDoctor = terser.get("PV1-7");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-7 field: {}", e2.getMessage());
                }
            }
            visitData.setAttendingDoctor(attendingDoctor);
            
            // Referring doctor
            String referringDoctor = null;
            try {
                referringDoctor = terser.get("/.PV1-8");
            } catch (HL7Exception e) {
                try {
                    referringDoctor = terser.get("PV1-8");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access PV1-8 field: {}", e2.getMessage());
                }
            }
            visitData.setReferringDoctor(referringDoctor);
            
            // Only set visit data if we have at least some visit information
            if (visitData.getVisitNumber() != null || visitData.getPatientClass() != null || visitData.getAssignedPatientLocation() != null) {
                parsedData.setVisitData(visitData);
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting visit data: {}", e.getMessage());
        }
    }
    
    private void extractOrderData(Terser terser, ParsedHl7Data parsedData) {
        try {
            // Extract ORC (Common Order) data
            OrderData orderData = new OrderData();
            
            // Order number
            String orderNumber = null;
            try {
                orderNumber = terser.get("/.ORC-2");
            } catch (HL7Exception e) {
                try {
                    orderNumber = terser.get("ORC-2");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access ORC-2 field: {}", e2.getMessage());
                }
            }
            orderData.setOrderNumber(orderNumber);
            orderData.setPlacerOrderNumber(orderNumber);
            
            // Filler order number
            String fillerOrderNumber = null;
            try {
                fillerOrderNumber = terser.get("/.ORC-3");
            } catch (HL7Exception e) {
                try {
                    fillerOrderNumber = terser.get("ORC-3");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access ORC-3 field: {}", e2.getMessage());
                }
            }
            orderData.setFillerOrderNumber(fillerOrderNumber);
            
            // Order status
            String orderStatus = null;
            try {
                orderStatus = terser.get("/.ORC-1");
            } catch (HL7Exception e) {
                try {
                    orderStatus = terser.get("ORC-1");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access ORC-1 field: {}", e2.getMessage());
                }
            }
            orderData.setOrderStatus(orderStatus);
            
            // Order date/time
            String orderDateTime = null;
            try {
                orderDateTime = terser.get("/.ORC-9");
            } catch (HL7Exception e) {
                try {
                    orderDateTime = terser.get("ORC-9");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access ORC-9 field: {}", e2.getMessage());
                }
            }
            
            if (orderDateTime != null && !orderDateTime.isEmpty()) {
                orderData.setOrderDateTime(parseHl7DateTime(orderDateTime));
            }
            
            // Ordering provider
            String orderingProvider = null;
            try {
                orderingProvider = terser.get("/.ORC-12");
            } catch (HL7Exception e) {
                try {
                    orderingProvider = terser.get("ORC-12");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access ORC-12 field: {}", e2.getMessage());
                }
            }
            orderData.setOrderingProvider(orderingProvider);
            
            // Extract OBR (Observation Request) data
            String observationCode = null;
            try {
                observationCode = terser.get("/.OBR-4");
            } catch (HL7Exception e) {
                try {
                    observationCode = terser.get("OBR-4");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access OBR-4 field: {}", e2.getMessage());
                }
            }
            
            if (observationCode != null && !observationCode.isEmpty()) {
                if (observationCode.contains("^")) {
                    String[] components = observationCode.split("\\^");
                    orderData.setOrderCode(components[0]);
                    if (components.length > 1) {
                        orderData.setOrderName(components[1]);
                    }
                } else {
                    orderData.setOrderCode(observationCode);
                }
            }
            
            // Priority
            String priority = null;
            try {
                priority = terser.get("/.OBR-5");
            } catch (HL7Exception e) {
                try {
                    priority = terser.get("OBR-5");
                } catch (HL7Exception e2) {
                    logger.debug("Could not access OBR-5 field: {}", e2.getMessage());
                }
            }
            orderData.setPriority(priority);
            
            // Only add order data if we have at least some order information
            if (orderData.getOrderNumber() != null || orderData.getOrderCode() != null) {
                parsedData.addOrder(orderData);
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting order data: {}", e.getMessage());
        }
    }
    
    private void extractObservationData(Terser terser, ParsedHl7Data parsedData) {
        try {
            // ORU messages can have multiple OBX segments
            int obxIndex = 0;
            while (true) {
                try {
                    String obxPath = "/.OBX(" + obxIndex + ")";
                    String observationId = null;
                    
                    try {
                        observationId = terser.get(obxPath + "-1");
                    } catch (HL7Exception e) {
                        try {
                            observationId = terser.get("OBX(" + obxIndex + ")-1");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({}) segment: {}", obxIndex, e2.getMessage());
                            break;
                        }
                    }
                    
                    if (observationId == null) {
                        break; // No more OBX segments
                    }
                    
                    ObservationData observation = new ObservationData();
                    observation.setObservationId(observationId);
                    
                    // Observation identifier
                    String observationIdentifier = null;
                    try {
                        observationIdentifier = terser.get(obxPath + "-3");
                    } catch (HL7Exception e) {
                        try {
                            observationIdentifier = terser.get("OBX(" + obxIndex + ")-3");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-3 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    
                    if (observationIdentifier != null && !observationIdentifier.isEmpty()) {
                        if (observationIdentifier.contains("^")) {
                            String[] components = observationIdentifier.split("\\^");
                            observation.setObservationCode(components[0]);
                            if (components.length > 1) {
                                observation.setObservationName(components[1]);
                            }
                        } else {
                            observation.setObservationCode(observationIdentifier);
                        }
                    }
                    
                    // Observation value
                    String value = null;
                    try {
                        value = terser.get(obxPath + "-5");
                    } catch (HL7Exception e) {
                        try {
                            value = terser.get("OBX(" + obxIndex + ")-5");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-5 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    observation.setValue(value);
                    
                    // Units
                    String units = null;
                    try {
                        units = terser.get(obxPath + "-6");
                    } catch (HL7Exception e) {
                        try {
                            units = terser.get("OBX(" + obxIndex + ")-6");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-6 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    observation.setUnits(units);
                    
                    // Reference range
                    String referenceRange = null;
                    try {
                        referenceRange = terser.get(obxPath + "-7");
                    } catch (HL7Exception e) {
                        try {
                            referenceRange = terser.get("OBX(" + obxIndex + ")-7");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-7 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    observation.setReferenceRange(referenceRange);
                    
                    // Abnormal flags
                    String abnormalFlags = null;
                    try {
                        abnormalFlags = terser.get(obxPath + "-8");
                    } catch (HL7Exception e) {
                        try {
                            abnormalFlags = terser.get("OBX(" + obxIndex + ")-8");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-8 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    observation.setAbnormalFlags(abnormalFlags);
                    
                    // Observation result status
                    String resultStatus = null;
                    try {
                        resultStatus = terser.get(obxPath + "-11");
                    } catch (HL7Exception e) {
                        try {
                            resultStatus = terser.get("OBX(" + obxIndex + ")-11");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-11 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    observation.setResultStatus(resultStatus);
                    
                    // Observation date/time
                    String observationDateTime = null;
                    try {
                        observationDateTime = terser.get(obxPath + "-14");
                    } catch (HL7Exception e) {
                        try {
                            observationDateTime = terser.get("OBX(" + obxIndex + ")-14");
                        } catch (HL7Exception e2) {
                            logger.debug("Could not access OBX({})-14 field: {}", obxIndex, e2.getMessage());
                        }
                    }
                    
                    if (observationDateTime != null && !observationDateTime.isEmpty()) {
                        observation.setObservationDateTime(parseHl7DateTime(observationDateTime));
                    }
                    
                    parsedData.addObservation(observation);
                    obxIndex++;
                    
                } catch (Exception e) {
                    logger.debug("No more OBX segments found at index {}: {}", obxIndex, e.getMessage());
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting observation data: {}", e.getMessage());
        }
    }
    
    private void parsePatientName(String patientName, PatientData patientData) {
        if (patientName != null && !patientName.trim().isEmpty()) {
            if (patientName.contains("^")) {
                String[] nameComponents = patientName.split("\\^");
                if (nameComponents.length > 0 && !nameComponents[0].trim().isEmpty()) {
                    patientData.setLastName(nameComponents[0].trim());
                }
                if (nameComponents.length > 1 && !nameComponents[1].trim().isEmpty()) {
                    patientData.setFirstName(nameComponents[1].trim());
                }
                if (nameComponents.length > 2 && !nameComponents[2].trim().isEmpty()) {
                    patientData.setMiddleName(nameComponents[2].trim());
                }
            } else {
                // If no components, assume it's the last name
                patientData.setLastName(patientName.trim());
            }
        }
    }
    
    private void parsePatientAddress(String address, PatientData patientData) {
        if (address != null && !address.trim().isEmpty()) {
            if (address.contains("^")) {
                String[] addressComponents = address.split("\\^");
                if (addressComponents.length > 0 && !addressComponents[0].trim().isEmpty()) {
                    patientData.setAddress(addressComponents[0].trim());
                }
                if (addressComponents.length > 2 && !addressComponents[2].trim().isEmpty()) {
                    patientData.setCity(addressComponents[2].trim());
                }
                if (addressComponents.length > 3 && !addressComponents[3].trim().isEmpty()) {
                    patientData.setState(addressComponents[3].trim());
                }
                if (addressComponents.length > 4 && !addressComponents[4].trim().isEmpty()) {
                    patientData.setZipCode(addressComponents[4].trim());
                }
            } else {
                patientData.setAddress(address.trim());
            }
        }
    }
    
    private void parsePatientLocation(String location, VisitData visitData) {
        if (location.contains("^")) {
            String[] locationComponents = location.split("\\^");
            if (locationComponents.length > 0) {
                visitData.setRoom(locationComponents[0]);
            }
            if (locationComponents.length > 1) {
                visitData.setBed(locationComponents[1]);
            }
            if (locationComponents.length > 2) {
                visitData.setFacility(locationComponents[2]);
            }
        }
    }
    
    private LocalDateTime parseHl7DateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        
        try {
            // Remove any timezone info for now (after + or -)
            String cleanDateTime = dateTimeStr.split("[+-]")[0];
            
            // Pad with zeros if needed
            if (cleanDateTime.length() < 14) {
                cleanDateTime = String.format("%-14s", cleanDateTime).replace(' ', '0');
            }
            
            return LocalDateTime.parse(cleanDateTime, HL7_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse HL7 datetime: {}", dateTimeStr);
            return null;
        }
    }
    

    private LocalDate parseHl7Date(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            // Take only the first 8 characters for date
            String cleanDate = dateStr.length() >= 8 ? dateStr.substring(0, 8) : dateStr;
            return LocalDate.parse(cleanDate, HL7_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse HL7 date: {}", dateStr);
            return null;
        }
    }
}