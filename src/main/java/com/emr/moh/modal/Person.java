package com.emr.moh.modal;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import lombok.Data;

@Data
public class Person {

    private String id;
    private String fname;
    private String lname;
    private String otherNames;
    private String email;
    private String mobileNo;
    private String state;
    private String city;
    private String postalCode;
    private String gender; 
    private Boolean deceased;
    private Date birthDate;

    public static String extractNameFromFhirNames(List<StringType> stringType , int index){
      boolean check  =  stringType.size() == index+1; // check if index exists
    return check ? stringType.get(index).asStringValue() : "";
    }

    public static Person convertFHIRPatientToPerson(Patient patient){
        Person person = new Person();
        person.setFname(patient.getNameFirstRep().getFamily());
        // person.setLname(patient.getNameFirstRep().getGiven().stream().findFirst().isPresent() ?
        // patient.getNameFirstRep().getGiven().stream().findFirst().get().asStringValue() : "");
       // person.setId(patient.getIdElement().primitiveValue());
        person.setLname(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 0));
        person.setOtherNames(extractNameFromFhirNames(patient.getNameFirstRep().getGiven(), 1));
        person.setEmail(patient.getTelecom().get(0).getValue());
        person.setMobileNo(patient.getTelecom().get(1).getValue());
        person.setState(patient.getAddress().stream().findFirst().get().getState());
        person.setPostalCode(patient.getAddress().stream().findFirst().get().getPostalCode());
        person.setCity(patient.getAddress().stream().findFirst().get().getCity());
        person.setBirthDate(Date.from(patient.getBirthDate().toInstant()));
        person.setGender(patient.getGender().name());
        person.setDeceased(patient.getDeceasedBooleanType().getValue());
        return person;
    }
}

