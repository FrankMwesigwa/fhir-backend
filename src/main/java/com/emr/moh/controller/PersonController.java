package com.emr.moh.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/person")
    public ResponseEntity<?> getPerson() {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);
        IParser parser = ctx.newJsonParser();

        List<Patient> patients = new ArrayList<>();

        try {
            Bundle response = client.search()
                    .forResource(Patient.class)
                    // .where(Patient.FAMILY.matches().values("oldman"))
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println("Patients Size ================ " + response.getEntry().size());

            parser.encodeResourceToString(response);

            for (BundleEntryComponent entry : response.getEntry()) {
                Patient patient = (Patient) entry.getResource();
                // patientRepository.save(patient);

                String jsonEncoded = parser.setPrettyPrint(true).encodeResourceToString(patient);
                Patient person = parser.parseResource(Patient.class, jsonEncoded);

                String firstName = person.getNameFirstRep().getGivenAsSingleString();
                String city = patient.getAddressFirstRep().getCity();

                System.out.println("First Name =>>>>>>>>> " + firstName);
                System.out.println("City =>>>>>>>>> " + city);
            }

            return new ResponseEntity<>("Records Saved in DB", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Error!, Please try again", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}