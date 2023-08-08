package com.emr.moh.modal;

import java.time.LocalDate;
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

    public static String extractNameFromFhirNames(List<StringType> stringType, int index) {
        boolean check = stringType.size() >= 0; // check if index exists
        return check ? stringType.get(index).asStringValue() : "";
    }

    public static Person convertFHIRPatientToPerson(Patient patient) {
        Person person = new Person();
        person.setId(patient.getId());
        person.setPassport(patient.getIdentifier().get(0).getValue());
        person.setNationalId(patient.getIdentifier().get(1).getValue());
        person.setSystemId(patient.getIdentifier().get(2).getValue());
        person.setPatientId(patient.getIdentifier().get(3).getValue());
        person.setSurname(patient.getNameFirstRep().getText());
        person.setGivenname(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 0));
        person.setOthername(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 1));
        person.setPhoneNumber(patient.getTelecomFirstRep().getValue());
        // person.setAddress(patient.getAddress().get(0).getLine().get(0).getValue(););
        person.setPostalCode(patient.getAddress().stream().findFirst().get().getPostalCode());
        person.setDistrict(patient.getAddress().stream().findFirst().get().getDistrict());
        person.setSubCounty(patient.getAddress().stream().findFirst().get().getCity());
        person.setVillage(patient.getAddress().stream().findFirst().get().getState());
        person.setParish(patient.getAddress().stream().findFirst().get().getCountry());
        person.setBirthDate(patient.getBirthDate());
        person.setMaritalStatus(patient.getMaritalStatus().getText());
        if (patient.getGender().toString() != null) {
           person.setGender(patient.getGender().toCode());
        }
        if (!patient.getTelecom().toString().isEmpty()) {
            person.setEmail(patient.getTelecom().get(1).getValue());
        }
        return person;
    }

    // public static Patient convertJsonToFHIR(Person person) {

    //     Patient patient = new Patient();

    //     patient.setId(UUID.randomUUID().toString());
    //     patient.addIdentifier().setSystem("http://acme.com/MRNs").setValue("frank-emr");
    //     patient.setActive(true);
    //     patient.setBirthDate(person.getBirthDate());

    //     patient.addName()
    //             .setText(person.getFirstName())
    //             .addGiven(person.getLastName())
    //             .addGiven(person.getOtherName());

    //     patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(person.getPhoneNumber());
    //     patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(person.getEmail());

    //     patient.addAddress().setState(person.getVillage())
    //             .setCity(person.getCity())
    //             .setPostalCode(person.getPostalCode());

    //     switch (person.getGender()) {
    //         case "male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
    //         case "female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
    //         case "others" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
    //     }

    //     return patient;

    // }
}