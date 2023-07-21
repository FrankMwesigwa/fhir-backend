package com.emr.moh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.emr.moh.modal.Person;
import java.util.List;


public interface PersonRepository extends JpaRepository<Person, Long> {
    
    List<Person> findByEmail(String email);
    boolean existsByEmail(String email);
}