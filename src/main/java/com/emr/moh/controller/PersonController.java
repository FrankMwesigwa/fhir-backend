package com.emr.moh.controller;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emr.moh.modal.Person;
import com.emr.moh.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@RestController
@RequestMapping("/api")
public class PersonController {

    @Autowired
    PersonRepository personRepository;

    @PostMapping("/person")
    public ResponseEntity<?> addPerson(@RequestBody Person person) {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://165.232.114.52:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

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

        patient.addAddress()
                .setState(person.getVillage())
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
    public ResponseEntity<JsonNode> getPerson() {

        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://165.232.114.52:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);
        IParser parser = ctx.newJsonParser();

        try {
            Bundle response = client.search()
                    .forResource(Patient.class)
                    // .where(new StringClientParam("given").matches().value("oldman"))
                    // .where(Patient.GIVEN.matches().value("old"))
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println("Patients Size ================ " + response.getEntry().size());

            String responseString = parser.encodeResourceToString(response);
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode = null;

            try {
                jsonNode = objectMapper.readTree(responseString);
            } catch (Exception e) {
                // TODO: handle exception
            }

            return new ResponseEntity<>(jsonNode, HttpStatus.OK);

        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/persons")
    public ResponseEntity<List<Person>> getPersons() {
        FhirContext ctx = FhirContext.forR4();
        String serverBase = "http://165.232.114.52:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        try {
            Bundle response = client.search()
                    .forResource(Patient.class)
                    // .where(Patient.FAMILY.matches().values("Deo", "Katesigwa","Oldman"))
                    // .where(new StringClientParam("given").matches().value("oldman"))
                    .where(Patient.GIVEN.matches().value("old"))
                    .returnBundle(Bundle.class)
                    .execute();

            List<BundleEntryComponent> list = response.getEntry(); // extract entries

            List<Person> persons = list.stream()
                    .map(t -> (Patient) t.getResource()) // convert each entry to a resource inthis case its Patient
                    .map(Person::convertFHIRPatientToPerson) // convert each patient resource into our Person Modal
                    .toList(); // collection our converted Persons

            System.out.println(persons);
            personRepository.saveAll(persons);

            return new ResponseEntity<>(persons, HttpStatus.OK);
        } catch (Exception e) {
            throw e;
        }

    }
}