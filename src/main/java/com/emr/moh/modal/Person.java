package com.emr.moh.modal;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

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
    private String firstName;
    private String lastName;
    private String otherName;
    // @Column(unique = true)
    private String email;
    private String phoneNumber;
    private String village;
    private String city;
    private String address;
    private String postalCode;
    private String gender;
    private Boolean deceased;
    private Date birthDate;
    private String maritalStatus;

    public static String extractNameFromFhirNames(List<StringType> stringType, int index) {
        boolean check = stringType.size() == index + 1; // check if index exists
        return check ? stringType.get(index).asStringValue() : "";
    }

    public static Person convertFHIRPatientToPerson(Patient patient) {
        Person person = new Person();
        person.setFirstName(patient.getNameFirstRep().getText());
        // person.setLname(patient.getNameFirstRep().getGiven().stream().findFirst().isPresent()
        // ?
        // patient.getNameFirstRep().getGiven().stream().findFirst().get().asStringValue()
        // : "");
        // person.setId(patient.getIdElement().primitiveValue());
        person.setLastName(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 0));
        person.setOtherName(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 1));
        person.setPhoneNumber(patient.getTelecom().get(0).getValue());
        person.setEmail(patient.getTelecom().get(1).getValue());
        person.setVillage(patient.getAddress().stream().findFirst().get().getState());
        person.setPostalCode(patient.getAddress().stream().findFirst().get().getPostalCode());
        person.setCity(patient.getAddress().stream().findFirst().get().getCity());
        person.setBirthDate(Date.from(patient.getBirthDate().toInstant()));
        person.setGender(patient.getGender().name());
        person.setDeceased(patient.getDeceasedBooleanType().getValue());
        return person;
    }

    public static Patient convertJsonToFHIR(Person person) {

        Patient patient = new Patient();

        patient.setId(UUID.randomUUID().toString());
        patient.addIdentifier().setSystem("http://acme.com/MRNs").setValue("frank-emr");
        patient.setActive(true);
        patient.setBirthDate(person.getBirthDate());

        patient.addName()
                .setText(person.getFirstName())
                .addGiven(person.getLastName())
                .addGiven(person.getOtherName());

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(person.getPhoneNumber());
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(person.getEmail());

        patient.addAddress().setState(person.getVillage())
                .setCity(person.getCity())
                .setPostalCode(person.getPostalCode());

        switch (person.getGender()) {
            case "male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
            case "female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "others" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
        }

        return patient;

    }
}