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
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
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
    private String city;
    private String country;
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
        PASSPORT("Passport"), PATIENTID("Patient Unique  ID Code (UIC)"), NATIONAL_ID("National ID No.");

        private String type;

        IdentifierType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static String extractNameFromFhirNames(List<StringType> stringType, int index) {
        boolean check = stringType.size() > index; // check if index exists
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

    private String getPatientEmail(Patient patient) {
        Optional<ContactPoint> optional = patient.getTelecom().stream()
                .filter(contactPoint -> contactPoint.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                .findFirst();
        return optional.isPresent() ? optional.get().getValue() : "";
    }

    private String getPatientPhone(Patient patient) {
        Optional<ContactPoint> optional = patient.getTelecom().stream()
                .filter(contactPoint -> contactPoint.getSystem() == ContactPoint.ContactPointSystem.PHONE)
                .findFirst();
        return optional.isPresent() ? optional.get().getValue() : "";
    }

    // private Optional<Extension> findExtensionByUrl(List<Extension> extensions , String extensionUrl) {
    //     return extensions.stream().filter(e -> e.getUrl().equalsIgnoreCase(extensionUrl) ).findFirst();
    // }

    //     Optional<Extension> optional = findExtensionByUrl(extensions , extensionUrl);
    //     return optional.isPresent() ? optional.get().getValueAsPrimitive().getValueAsString() : "";
    // }

    public Person convertFHIRPatientToPerson(Patient patient) {
        Optional<Address> optionalAddress = patient.getAddress().stream().findFirst();
    //    List<Extension> addressExtensions =  optionalAddress.get().getExtensionByUrl("http://fhir.openmrs.org/ext/address").getExtension();

    //    System.out.println("ADRESS SIZE "+addressExtensions.size());


        Person person = new Person();
        person.setId(patient.getId());
        person.setPassport(getPatientIdentifierValue(patient, IdentifierType.PASSPORT.getType()));
        person.setNationalId(getPatientIdentifierValue(patient, IdentifierType.NATIONAL_ID.getType()));
        person.setUniquePatientId(getPatientIdentifierValue(patient, IdentifierType.PATIENTID.getType()));
        person.setSurname(patient.getNameFirstRep().getFamily());
        person.setGivenname(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 0));
        person.setOthername(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 1));
        person.setPhoneNumber(patient.getTelecomFirstRep().getValue());
        // person.setAddress(patient.getAddress().get(0).getLine().stream().findFirst().get().getValue());
        person.setPostalCode(optionalAddress.isPresent() ? optionalAddress.get().getPostalCode() : "");
        person.setDistrict(optionalAddress.isPresent() ? optionalAddress.get().getDistrict() : "");
        person.setCity(optionalAddress.isPresent() ? optionalAddress.get().getCity() : "");
        
        // person.setSubCounty(findExtensionValue(addressExtensions , "http://fhir.openmrs.org/ext/address#subcounty"));
        // person.setParish(findExtensionValue(addressExtensions , "http://fhir.openmrs.org/ext/address#parish"));
        // person.setVillage(findExtensionValue(addressExtensions , "http://fhir.openmrs.org/ext/address#village"));

        person.setCountry(optionalAddress.isPresent() ? optionalAddress.get().getCountry() : "");
        person.setBirthDate(patient.getBirthDate());
        if (patient.getMaritalStatus().getCodingFirstRep() != null) {
            person.setMaritalStatus(patient.getMaritalStatus().getCodingFirstRep().getDisplay());
        }

        if (patient.getGender().toString() != null) {
            person.setGender(patient.getGender().toCode());
        }

        person.setEmail(getPatientEmail(patient));
        person.setPhoneNumber(getPatientPhone(patient));
        return person;
    }
}