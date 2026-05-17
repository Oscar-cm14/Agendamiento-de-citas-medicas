package com.clinica.doctors.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorTest {

    @Test
    void testDoctorGettersAndSetters() {
        // Arrange
        Doctor doctor = new Doctor();
        String specialty = "Cardiology";
        String licenseNumber = "MED-12345";
        Boolean active = true;

        // Act
        doctor.setSpecialty(specialty);
        doctor.setLicenseNumber(licenseNumber);
        doctor.setActive(active);

        // Inherited from Person
        doctor.setFirstName("House");
        doctor.setLastName("Gregory");

        // Assert
        assertEquals(specialty, doctor.getSpecialty());
        assertEquals(licenseNumber, doctor.getLicenseNumber());
        assertTrue(doctor.getActive());
        assertEquals("House", doctor.getFirstName());
        assertEquals("Gregory", doctor.getLastName());
    }

    @Test
    void testDoctorDefaultConstructor() {
        // Arrange & Act
        Doctor doctor = new Doctor();

        // Assert
        assertNull(doctor.getSpecialty());
        assertNull(doctor.getLicenseNumber());
        assertNull(doctor.getActive());
    }
}
