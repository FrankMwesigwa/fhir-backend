package com.emr.moh.controller;

import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emr.moh.modal.Person;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@RestController
@RequestMapping("/api")
public class PersonController {

    @PostMapping("/person")
    public ResponseEntity<?> addPerson(@RequestBody Person person) {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        Patient patient = new Patient();

        patient.setId(UUID.randomUUID().toString());
        patient.addIdentifier().setSystem("http://acme.com/MRNs").setValue("frank-emr");
        patient.setActive(true);
        patient.setBirthDate(person.getBirthDate());

        patient.addName()
                .setText(person.getFname())
                .addGiven(person.getLname())
                .addGiven(person.getOtherNames());

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(person.getMobileNo());
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(person.getEmail());

        patient.addAddress().setState(person.getState())
                .setCity(person.getCity())
                .setPostalCode(person.getPostalCode());

        // patient.getManagingOrganization().setReference("http://example.com/base/Organization/FOO");

        switch (person.getGender()) {
            case "male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
            case "female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "others" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
        }

        String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        System.out.println(encoded);

        MethodOutcome outcome = client.create()
                .resource(patient)
                .prettyPrint()
                .encodedJson()
                .execute();

        System.out.println(patient);
        return new ResponseEntity<>(outcome.getCreated(), HttpStatus.CREATED);
    }

    @GetMapping("/persons")
    public ResponseEntity<?> getPatient() {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        try {
            Bundle results = client
                    .search()
                    .forResource(Patient.class)
                    .where(Patient.FAMILY.matches().value("Frank"))
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println(client);

            return new ResponseEntity<>(results.getEntry().size(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/person/name")
    public ResponseEntity<?> getPerson() {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);
        IParser parser = ctx.newJsonParser();

            Bundle response = client.search()
                    .forResource(Patient.class)
                    .where(Patient.FAMILY.matches().values("deo"))
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println("Found " + response.getTotal());

            for (BundleEntryComponent entry : response.getEntry()) {
			
                String jsonEncoded = parser.encodeResourceToString(entry.getResource());
    
                System.out.println("-------------------------------------------------------------------");
                System.out.println(jsonEncoded);
                
                Patient patient = FhirContext.forR4().newJsonParser().parseResource(Patient.class, jsonEncoded);
                
                System.out.println(patient.getName().toString());
                System.out.println(patient.getGender());
                System.out.println(patient.getBirthDate());
                System.out.println(patient.getAddress());
                
                String formatedName = patient.getNameFirstRep().getGivenAsSingleString()+" "+patient.getNameFirstRep().getNameAsSingleString();
                
                String formatedAdderess = 
                        patient.getAddressFirstRep().getCountry()+ ", "+
                        patient.getAddressFirstRep().getCity()+ " "+
                        "("+patient.getAddressFirstRep().getPostalCode()+")";
    
                String formatedID = patient.getIdentifierFirstRep().getValue();	
                
                
                // result+= "ID: "+formatedID+System.lineSeparator();
                // result+= "Active: "+patient.getActive()+System.lineSeparator();
                // result+= "Name: "+formatedName+System.lineSeparator();
                // result+= "Birth Date: "+patient.getBirthDate()+System.lineSeparator();
                // result+= "Aderess: "+formatedAdderess+System.lineSeparator();	
            }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}