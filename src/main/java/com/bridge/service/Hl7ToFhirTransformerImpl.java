package com.bridge.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.bridge.model.FhirResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;

/**
 * Implementation of HL7 to FHIR transformation service using HAPI FHIR library
 */
@Service
public class Hl7ToFhirTransformerImpl implements Hl7ToFhirTransformer {

    private final FhirContext fhirContext;
    private final IParser jsonParser;
    private final TransformationRuleEngine ruleEngine;

    @Autowired
    public Hl7ToFhirTransformerImpl(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        this.jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
        this.ruleEngine = new TransformationRuleEngine();
    }

    @Override
    @Cacheable(value = "fhir-transformations", key = "#parsedData.messageControlId + ':' + #parsedData.patientData?.patientId")
    public List<FhirResource> transformToFhir(ParsedHl7Data parsedData) {
        List<FhirResource> resources = new ArrayList<>();
        
        // Transform patient data if available
        if (parsedData.getPatientData() != null) {
            FhirResource patientResource = transformPatientData(parsedData);
            if (patientResource != null) {
                resources.add(patientResource);
            }
        }
        
        // Transform visit/encounter data if available
        if (parsedData.getVisitData() != null) {
            FhirResource encounterResource = transformVisitData(parsedData);
            if (encounterResource != null) {
                resources.add(encounterResource);
            }
        }
        
        // Transform observations
        for (ObservationData obsData : parsedData.getObservations()) {
            FhirResource observationResource = transformObservationData(obsData, parsedData);
            if (observationResource != null) {
                resources.add(observationResource);
            }
        }
        
        // Transform orders to ServiceRequest resources
        for (OrderData orderData : parsedData.getOrders()) {
            FhirResource serviceRequestResource = transformOrderData(orderData, parsedData);
            if (serviceRequestResource != null) {
                resources.add(serviceRequestResource);
            }
        }
        
        return resources;
    }

    @Override
    public FhirResource transformToFhirResource(ParsedHl7Data parsedData, String resourceType) {
        switch (resourceType.toLowerCase()) {
            case "patient":
                return transformPatientData(parsedData);
            case "encounter":
                return transformVisitData(parsedData);
            case "observation":
                // Return first observation if available
                if (!parsedData.getObservations().isEmpty()) {
                    return transformObservationData(parsedData.getObservations().get(0), parsedData);
                }
                break;
            case "servicerequest":
                // Return first order if available
                if (!parsedData.getOrders().isEmpty()) {
                    return transformOrderData(parsedData.getOrders().get(0), parsedData);
                }
                break;
        }
        return null;
    }

    private FhirResource transformPatientData(ParsedHl7Data parsedData) {
        PatientData patientData = parsedData.getPatientData();
        if (patientData == null) {
            return null;
        }

        Patient patient = new Patient();
        
        // Set patient ID
        if (patientData.getPatientId() != null) {
            patient.setId(patientData.getPatientId());
            patient.addIdentifier()
                .setSystem("http://hospital.example.org/patient-id")
                .setValue(patientData.getPatientId());
        }
        
        // Add medical record number if available
        if (patientData.getMedicalRecordNumber() != null) {
            patient.addIdentifier()
                .setSystem("http://hospital.example.org/mrn")
                .setValue(patientData.getMedicalRecordNumber());
        }
        
        // Set name
        HumanName name = patient.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        if (patientData.getFirstName() != null) {
            name.addGiven(patientData.getFirstName());
        }
        if (patientData.getMiddleName() != null) {
            name.addGiven(patientData.getMiddleName());
        }
        if (patientData.getLastName() != null) {
            name.setFamily(patientData.getLastName());
        }
        
        // Set gender
        if (patientData.getGender() != null) {
            patient.setGender(mapGender(patientData.getGender()));
        }
        
        // Set birth date
        if (patientData.getDateOfBirth() != null) {
            patient.setBirthDate(java.sql.Date.valueOf(patientData.getDateOfBirth()));
        }
        
        // Set address
        if (patientData.getAddress() != null || patientData.getCity() != null || 
            patientData.getState() != null || patientData.getZipCode() != null) {
            Address address = patient.addAddress();
            address.setUse(Address.AddressUse.HOME);
            if (patientData.getAddress() != null) {
                address.addLine(patientData.getAddress());
            }
            if (patientData.getCity() != null) {
                address.setCity(patientData.getCity());
            }
            if (patientData.getState() != null) {
                address.setState(patientData.getState());
            }
            if (patientData.getZipCode() != null) {
                address.setPostalCode(patientData.getZipCode());
            }
        }
        
        // Set phone number
        if (patientData.getPhoneNumber() != null) {
            patient.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setUse(ContactPoint.ContactPointUse.HOME)
                .setValue(patientData.getPhoneNumber());
        }
        
        // Set marital status
        if (patientData.getMaritalStatus() != null) {
            patient.setMaritalStatus(mapMaritalStatus(patientData.getMaritalStatus()));
        }
        
        String jsonContent = jsonParser.encodeResourceToString(patient);
        return new FhirResource(
            patient.getId(),
            "Patient",
            jsonContent,
            parsedData.getMessageControlId()
        );
    }

    private FhirResource transformVisitData(ParsedHl7Data parsedData) {
        VisitData visitData = parsedData.getVisitData();
        if (visitData == null) {
            return null;
        }

        Encounter encounter = new Encounter();
        
        // Set encounter ID
        if (visitData.getVisitNumber() != null) {
            encounter.setId(visitData.getVisitNumber());
            encounter.addIdentifier()
                .setSystem("http://hospital.example.org/visit-number")
                .setValue(visitData.getVisitNumber());
        }
        
        // Set status - default to finished for historical data
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        
        // Set class
        if (visitData.getPatientClass() != null) {
            encounter.setClass_(mapEncounterClass(visitData.getPatientClass()));
        }
        
        // Set patient reference
        if (parsedData.getPatientData() != null && parsedData.getPatientData().getPatientId() != null) {
            encounter.setSubject(new Reference("Patient/" + parsedData.getPatientData().getPatientId()));
        }
        
        // Set period
        Period period = new Period();
        if (visitData.getAdmitDateTime() != null) {
            period.setStart(Date.from(visitData.getAdmitDateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (visitData.getDischargeDateTime() != null) {
            period.setEnd(Date.from(visitData.getDischargeDateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (period.hasStart() || period.hasEnd()) {
            encounter.setPeriod(period);
        }
        
        // Set location
        if (visitData.getAssignedPatientLocation() != null || visitData.getRoom() != null || visitData.getBed() != null) {
            Encounter.EncounterLocationComponent location = encounter.addLocation();
            Location locationResource = new Location();
            
            StringBuilder locationName = new StringBuilder();
            if (visitData.getAssignedPatientLocation() != null) {
                locationName.append(visitData.getAssignedPatientLocation());
            }
            if (visitData.getRoom() != null) {
                if (locationName.length() > 0) locationName.append(" - ");
                locationName.append("Room ").append(visitData.getRoom());
            }
            if (visitData.getBed() != null) {
                if (locationName.length() > 0) locationName.append(" - ");
                locationName.append("Bed ").append(visitData.getBed());
            }
            
            locationResource.setName(locationName.toString());
            location.setLocation(new Reference().setDisplay(locationName.toString()));
        }
        
        String jsonContent = jsonParser.encodeResourceToString(encounter);
        return new FhirResource(
            encounter.getId(),
            "Encounter",
            jsonContent,
            parsedData.getMessageControlId()
        );
    }

    private FhirResource transformObservationData(ObservationData obsData, ParsedHl7Data parsedData) {
        if (obsData == null) {
            return null;
        }

        Observation observation = new Observation();
        
        // Set observation ID
        if (obsData.getObservationId() != null) {
            observation.setId(obsData.getObservationId());
        } else {
            observation.setId(UUID.randomUUID().toString());
        }
        
        // Set status
        observation.setStatus(mapObservationStatus(obsData.getObservationStatus()));
        
        // Set code
        if (obsData.getObservationCode() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://loinc.org")
                .setCode(obsData.getObservationCode())
                .setDisplay(obsData.getObservationName());
            observation.setCode(code);
        }
        
        // Set patient reference
        if (parsedData.getPatientData() != null && parsedData.getPatientData().getPatientId() != null) {
            observation.setSubject(new Reference("Patient/" + parsedData.getPatientData().getPatientId()));
        }
        
        // Set encounter reference
        if (parsedData.getVisitData() != null && parsedData.getVisitData().getVisitNumber() != null) {
            observation.setEncounter(new Reference("Encounter/" + parsedData.getVisitData().getVisitNumber()));
        }
        
        // Set effective date/time
        if (obsData.getObservationDateTime() != null) {
            observation.setEffective(new DateTimeType(Date.from(obsData.getObservationDateTime().atZone(ZoneId.systemDefault()).toInstant())));
        }
        
        // Set value
        if (obsData.getValue() != null) {
            if (isNumeric(obsData.getValue())) {
                Quantity quantity = new Quantity();
                quantity.setValue(Double.parseDouble(obsData.getValue()));
                if (obsData.getUnits() != null) {
                    quantity.setUnit(obsData.getUnits());
                    quantity.setSystem("http://unitsofmeasure.org");
                    quantity.setCode(obsData.getUnits());
                }
                observation.setValue(quantity);
            } else {
                observation.setValue(new StringType(obsData.getValue()));
            }
        }
        
        // Set reference range
        if (obsData.getReferenceRange() != null) {
            Observation.ObservationReferenceRangeComponent refRange = observation.addReferenceRange();
            refRange.setText(obsData.getReferenceRange());
        }
        
        String jsonContent = jsonParser.encodeResourceToString(observation);
        return new FhirResource(
            observation.getId(),
            "Observation",
            jsonContent,
            parsedData.getMessageControlId()
        );
    }

    private FhirResource transformOrderData(OrderData orderData, ParsedHl7Data parsedData) {
        if (orderData == null) {
            return null;
        }

        ServiceRequest serviceRequest = new ServiceRequest();
        
        // Set service request ID
        if (orderData.getOrderNumber() != null) {
            serviceRequest.setId(orderData.getOrderNumber());
            serviceRequest.addIdentifier()
                .setSystem("http://hospital.example.org/order-number")
                .setValue(orderData.getOrderNumber());
        }
        
        // Add placer and filler order numbers
        if (orderData.getPlacerOrderNumber() != null) {
            serviceRequest.addIdentifier()
                .setSystem("http://hospital.example.org/placer-order-number")
                .setValue(orderData.getPlacerOrderNumber());
        }
        if (orderData.getFillerOrderNumber() != null) {
            serviceRequest.addIdentifier()
                .setSystem("http://hospital.example.org/filler-order-number")
                .setValue(orderData.getFillerOrderNumber());
        }
        
        // Set status
        serviceRequest.setStatus(mapOrderStatus(orderData.getOrderStatus()));
        
        // Set intent
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        
        // Set code
        if (orderData.getOrderCode() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setSystem("http://loinc.org")
                .setCode(orderData.getOrderCode())
                .setDisplay(orderData.getOrderName());
            serviceRequest.setCode(code);
        }
        
        // Set patient reference
        if (parsedData.getPatientData() != null && parsedData.getPatientData().getPatientId() != null) {
            serviceRequest.setSubject(new Reference("Patient/" + parsedData.getPatientData().getPatientId()));
        }
        
        // Set encounter reference
        if (parsedData.getVisitData() != null && parsedData.getVisitData().getVisitNumber() != null) {
            serviceRequest.setEncounter(new Reference("Encounter/" + parsedData.getVisitData().getVisitNumber()));
        }
        
        // Set authored date
        if (orderData.getOrderDateTime() != null) {
            serviceRequest.setAuthoredOn(Date.from(orderData.getOrderDateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        
        // Set priority
        if (orderData.getPriority() != null) {
            serviceRequest.setPriority(mapOrderPriority(orderData.getPriority()));
        }
        
        String jsonContent = jsonParser.encodeResourceToString(serviceRequest);
        return new FhirResource(
            serviceRequest.getId(),
            "ServiceRequest",
            jsonContent,
            parsedData.getMessageControlId()
        );
    }

    // Helper methods for mapping HL7 values to FHIR enums
    private Enumerations.AdministrativeGender mapGender(String hl7Gender) {
        if (hl7Gender == null) return null;
        switch (hl7Gender.toUpperCase()) {
            case "M": case "MALE": return Enumerations.AdministrativeGender.MALE;
            case "F": case "FEMALE": return Enumerations.AdministrativeGender.FEMALE;
            case "O": case "OTHER": return Enumerations.AdministrativeGender.OTHER;
            case "U": case "UNKNOWN": return Enumerations.AdministrativeGender.UNKNOWN;
            default: return Enumerations.AdministrativeGender.UNKNOWN;
        }
    }

    private CodeableConcept mapMaritalStatus(String hl7MaritalStatus) {
        CodeableConcept maritalStatus = new CodeableConcept();
        if (hl7MaritalStatus != null) {
            maritalStatus.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                .setCode(hl7MaritalStatus)
                .setDisplay(hl7MaritalStatus);
        }
        return maritalStatus;
    }

    private Coding mapEncounterClass(String hl7PatientClass) {
        Coding encounterClass = new Coding();
        encounterClass.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        
        if (hl7PatientClass != null) {
            switch (hl7PatientClass.toUpperCase()) {
                case "I": case "INPATIENT":
                    encounterClass.setCode("IMP").setDisplay("inpatient encounter");
                    break;
                case "O": case "OUTPATIENT":
                    encounterClass.setCode("AMB").setDisplay("ambulatory");
                    break;
                case "E": case "EMERGENCY":
                    encounterClass.setCode("EMER").setDisplay("emergency");
                    break;
                default:
                    encounterClass.setCode("AMB").setDisplay("ambulatory");
            }
        }
        return encounterClass;
    }

    private Observation.ObservationStatus mapObservationStatus(String hl7Status) {
        if (hl7Status == null) return Observation.ObservationStatus.FINAL;
        switch (hl7Status.toUpperCase()) {
            case "P": case "PRELIMINARY": return Observation.ObservationStatus.PRELIMINARY;
            case "F": case "FINAL": return Observation.ObservationStatus.FINAL;
            case "C": case "CORRECTED": return Observation.ObservationStatus.CORRECTED;
            case "X": case "CANCELLED": return Observation.ObservationStatus.CANCELLED;
            default: return Observation.ObservationStatus.FINAL;
        }
    }

    private ServiceRequest.ServiceRequestStatus mapOrderStatus(String hl7Status) {
        if (hl7Status == null) return ServiceRequest.ServiceRequestStatus.ACTIVE;
        switch (hl7Status.toUpperCase()) {
            case "A": case "ACTIVE": return ServiceRequest.ServiceRequestStatus.ACTIVE;
            case "C": case "COMPLETED": return ServiceRequest.ServiceRequestStatus.COMPLETED;
            case "CA": case "CANCELLED": return ServiceRequest.ServiceRequestStatus.REVOKED;
            case "H": case "HOLD": return ServiceRequest.ServiceRequestStatus.ONHOLD;
            default: return ServiceRequest.ServiceRequestStatus.ACTIVE;
        }
    }

    private ServiceRequest.ServiceRequestPriority mapOrderPriority(String hl7Priority) {
        if (hl7Priority == null) return ServiceRequest.ServiceRequestPriority.ROUTINE;
        switch (hl7Priority.toUpperCase()) {
            case "S": case "STAT": return ServiceRequest.ServiceRequestPriority.STAT;
            case "A": case "ASAP": return ServiceRequest.ServiceRequestPriority.ASAP;
            case "R": case "ROUTINE": return ServiceRequest.ServiceRequestPriority.ROUTINE;
            case "U": case "URGENT": return ServiceRequest.ServiceRequestPriority.URGENT;
            default: return ServiceRequest.ServiceRequestPriority.ROUTINE;
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Inner class for configurable transformation rules
     */
    private static class TransformationRuleEngine {
        // This can be expanded to support configurable mapping rules
        // For now, it uses the default mappings defined in the transformer
        
        public TransformationRuleEngine() {
            // Initialize with default rules
        }
        
        // Future enhancement: load custom mapping rules from configuration
    }
}