package com.emr.moh.controller;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.emr.moh.modal.Person;
import com.emr.moh.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;

@RestController
@RequestMapping("/api")
public class PersonController {

    private static CodeableConcept createMaritalStatus(String code, String display) {
        CodeableConcept maritalStatus = new CodeableConcept();
        maritalStatus.addCoding()
                .setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                .setCode(code)
                .setDisplay(display);
        return maritalStatus;
    }

    @Autowired
    PersonRepository personRepository;

    @PostMapping("/person")
    public ResponseEntity<?> addPerson(@RequestBody Person person) {

        FhirContext ctx = FhirContext.forR4();
        // String serverBase = "https://hapi-hris.health.go.ug/hapi/fhir";
        String serverBase = "http://165.232.114.52:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        Patient patient = new Patient();

        Identifier passport = new Identifier();
        Identifier nationId = new Identifier();
        Identifier systemId = new Identifier();
        Identifier patientId = new Identifier();

        patient.setActive(true);
        patient.setId(UUID.randomUUID().toString());

        passport.setSystem("https://passports.go.ug")
        .setValue(person.getPassport())
        .setUse(Identifier.IdentifierUse.OFFICIAL)
        .setType(new CodeableConcept().setText("Passport"));
        patient.addIdentifier(passport);

        nationId.setSystem("https://www.nira.go.ug")
        .setValue(person.getNationalId())
        .setUse(Identifier.IdentifierUse.USUAL)
        .setType(new CodeableConcept().setText("National ID No."));
        patient.addIdentifier(nationId);

        systemId.setSystem("http://acme.com/MRNs").setValue(person.getSystemId());
        patient.addIdentifier(systemId);

        patientId.setSystem("http://acme.com/MRNs").setValue(person.getPatientId());
        patient.addIdentifier(patientId);

        patient.setBirthDate(person.getBirthDate());

        patient.addName()
                // .setText(person.getSurname())
                .setFamily(person.getSurname())
                .addGiven(person.getGivenname())
                .addGiven(person.getOthername());

        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(person.getPhoneNumber());
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(person.getEmail());

        patient.addAddress()
                .addLine(person.getAddress())
                .setState(person.getVillage())
                .setCity(person.getSubCounty())
                .setDistrict(person.getDistrict())
                .setCountry(person.getParish())
                .setPostalCode(person.getPostalCode());

        switch (person.getGender()) {
            case "Male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
            case "Female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "Others" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
        }

        switch (person.getMaritalStatus()) {
            case "Married":
                patient.setMaritalStatus(createMaritalStatus("M", "Married"));
                break;
            case "Single":
                patient.setMaritalStatus(createMaritalStatus("S", "Single"));
                break;
            case "Divorced":
                patient.setMaritalStatus(createMaritalStatus("D", "Divorced"));
                break;
            default:
                break;
        }

        String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        System.out.println(encoded);

        MethodOutcome outcome = client.create()
                .resource(patient)
                .prettyPrint()
                .encodedJson()
                .execute();

        if (outcome.getCreated()) {
            System.out.println("New Patient created with ID: " + outcome.getId().getValue());
            return new ResponseEntity<>("Patient Resource Successfully Created", HttpStatus.CREATED);
        } else {
            System.out.println("Failed to create Patient resource.");
            return new ResponseEntity<>("Failed to create Patient resource.", HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("/person")
    public ResponseEntity<JsonNode> getPerson() {

        FhirContext ctx = FhirContext.forR4();
        // String serverBase = "https://hapi-hris.health.go.ug/hapi/fhir";
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
    public ResponseEntity<List<Person>> getPersons(@RequestParam("query") String query) {
        FhirContext ctx = FhirContext.forR4();
        // String serverBase = "https://hapi-hris.health.go.ug/hapi/fhir";
        String serverBase = "http://165.232.114.52:8080/fhir";
        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        try {
            Bundle response = client.search()
                    .forResource(Patient.class)
                    .where(new StringClientParam("name").matches().value(query))
                    // .where(new StringClientParam("given").matches().value("oldman"))
                    .returnBundle(Bundle.class)
                    .execute();

            List<BundleEntryComponent> list = response.getEntry(); // extract entries

            List<Person> persons = list.stream()
                    .map(t -> (Patient) t.getResource()) // convert each entry to a resource inthis case its Patient
                    .map(p -> {
                        Person person = new Person();
                       person = person.convertFHIRPatientToPerson(p);
                       return person;
                    }) // convert each patient resource into our Person Modal
                    .toList(); // collection our converted Persons

            // personRepository.saveAll(persons);

            return new ResponseEntity<>(persons, HttpStatus.OK);
        } catch (Exception e) {
            throw e;
        }

    }
}