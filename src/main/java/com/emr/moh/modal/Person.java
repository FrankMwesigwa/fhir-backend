package com.emr.moh.modal;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.GenericGenerator;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import com.fasterxml.jackson.databind.deser.impl.CreatorCandidate.Param;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "person")
@Data
public class Person {

    @Id
    @GeneratedValue(generator = "System-uuid")
    @GenericGenerator(name = "System-uuid", strategy = "uuid")

    private String id;
    private String uniquePatientId;
    private String nationalId;
    private String passport;
    private String systemId;
    private String patientId;
    private String surname;
    private String givenname;
    private String othername;
    // @Column(unique = true)
    private String email;
    private String phoneNumber;
    private String address;
    private String village;
    private String parish;
    private String subCounty;
    private String district;
    private String postalCode;
    private String gender;
    private boolean deceased;
    private Date birthDate;
    private String maritalStatus;

    private enum IdentifierType {
        PASSPORT("Passport"), NATIONAL_ID("National ID No.");

        private String type;

        IdentifierType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private enum TelecomType {
        PHONE("PHONE"), EMAIL("EMAIL");

        private String type;

        TelecomType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    // public String extractNameFromFhirNames(List<StringType> stringType, int index) {
    //     return stringType.isEmpty() ? stringType.get(index).asStringValue() : "";
    // }

    public static String extractNameFromFhirNames(List<StringType> stringType , int index){
        boolean check  =  stringType.size() >=0; // check if index exists
      return check ? stringType.get(index).asStringValue() : "";
      }

    private static Optional<Identifier> getPatientIdentifier(Patient patient, String identifierTpe) {
        return patient.getIdentifier().stream()
                .filter(identifer -> identifer.getType().getText().equalsIgnoreCase(identifierTpe)).findFirst();
    }

    private String getPatientIdentifierValue(Patient patient, String identifierTpe) {

        Optional<Identifier> passportOptional = getPatientIdentifier(patient, identifierTpe);
        return passportOptional.isPresent() ? passportOptional.get().getValue() : "";
    }

    private Optional<ContactPoint> getPatientTelecom(Patient patient, String type) {
        return patient.getTelecom().stream()
        .filter(contactPoint -> contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                .findFirst();

    }

    private String getPatientTelecomValue(Patient patient, String type) {
        Optional<ContactPoint> telecomOptional = getPatientTelecom(patient, type);
        return telecomOptional.isPresent() ? telecomOptional.get().getValue() : "";
    }

    public Person convertFHIRPatientToPerson(Patient patient) {

        Person person = new Person();
        person.setId(patient.getId());
        person.setPassport(getPatientIdentifierValue(patient, IdentifierType.PASSPORT.getType()));
        person.setNationalId(getPatientIdentifierValue(patient, IdentifierType.NATIONAL_ID.getType()));
        // person.setPatientId(patient.getIdentifier().get(3).getValue());
        person.setSurname(patient.getNameFirstRep().getFamily());
        person.setGivenname(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 0));
        // person.setOthername(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(),1));
        person.setPhoneNumber(patient.getTelecomFirstRep().getValue());
        // person.setAddress(patient.getAddress().get(0).getLine().stream().findFirst().get().getValue());
        person.setPostalCode(patient.getAddress().stream().findFirst().get().getPostalCode());
        person.setDistrict(patient.getAddress().stream().findFirst().get().getDistrict());
        person.setSubCounty(patient.getAddress().stream().findFirst().get().getCity());
        person.setVillage(patient.getAddress().stream().findFirst().get().getState());
        person.setParish(patient.getAddress().stream().findFirst().get().getCountry());
        person.setBirthDate(patient.getBirthDate());
        if (patient.getMaritalStatus().getCodingFirstRep() != null) {
            person.setMaritalStatus(patient.getMaritalStatus().getCodingFirstRep().getDisplay());
        }
        
        if (patient.getGender().toString() != null) {
            person.setGender(patient.getGender().toCode());
        }

          // person.setLname(patient.getNameFirstRep().getGiven().stream().findFirst().isPresent() ?
        // patient.getNameFirstRep().getGiven().stream().findFirst().get().asStringValue() : "");

        person.setEmail(getPatientTelecomValue(patient, TelecomType.EMAIL.getType()));
        // person.setPhoneNumber(getPatientTelecomValue(patient, TelecomType.PHONE.getType()));
        // if (patient.getTelecom().get(1) != null) {
        //person.setEmail(patient.getTelecom().stream().filter(contactPoint -> contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL));
        // }
        return person;
    }
}