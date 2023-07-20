package com.emr.moh.controller;

import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
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
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.val;

@RestController
@RequestMapping("/api")
public class PersonController {

    @PostMapping("/person")
    public ResponseEntity<?> addPerson(@RequestBody Person person) {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir/Patient";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        Patient patient = new Patient();

        patient.addIdentifier()
                .setSystem("http://acme.org/mrns")
                .setValue(UUID.randomUUID().toString());

        patient.addName()
                .setText(person.getFname())
                .addGiven(person.getLname())
                .addGiven("Jonah");

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(person.getMobileNo());
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(person.getEmail());

        patient.addAddress().setState(person.getState())
                .setCity(person.getCity())
                .setPostalCode(person.getPostalCode());

        patient.getManagingOrganization().setReference("http://example.com/base/Organization/FOO");

        switch (person.getGender()) {
            case "male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
            case "female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "others" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
        }

        String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        System.out.println(encoded);

        MethodOutcome outcome = client.create()
                .resource(encoded)
                .prettyPrint()
                .encodedJson()
                .execute();

        // IdDt id = (IdDt) outcome.getId();
        System.out.println(outcome);

        // patient.setId(outcome.getResource().getIdElement().getIdPartAsLong().toString());

        // newPerson.setEmail(person.getEmail());
        // newPerson.setFname(person.getFname());
        // newPerson.setLname(person.getLname());
        return new ResponseEntity<>(outcome.getCreated(), HttpStatus.CREATED);
    }

    @GetMapping("/persons")
    public ResponseEntity<?> getPatient() {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://localhost:8080/fhir/Patient";
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

}