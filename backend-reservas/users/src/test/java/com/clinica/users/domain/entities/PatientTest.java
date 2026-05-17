package com.clinica.users.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PatientTest {

    @Test
    void testPatientGettersAndSetters() {
        // Arrange
        Patient patient = new Patient();
        String gender = "Hombre";
        LocalDate birthDate = LocalDate.of(1990, 5, 15);

        // Act
        patient.setGender(gender);
        patient.setBirthDate(birthDate);

        // Inherited from Person
        patient.setFirstName("John");
        patient.setLastName("Doe");

        // Assert
        assertEquals(gender, patient.getGender());
        assertEquals(birthDate, patient.getBirthDate());
        assertEquals("John", patient.getFirstName());
        assertEquals("Doe", patient.getLastName());
    }

    @Test
    void testPatientDefaultConstructor() {
        // Arrange & Act
        Patient patient = new Patient();

        // Assert
        assertNull(patient.getGender());
        assertNull(patient.getBirthDate());
    }
}
