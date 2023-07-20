package com.emr.moh.modal;

import java.sql.Date;

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
}