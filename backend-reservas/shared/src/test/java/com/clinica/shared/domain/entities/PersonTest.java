package com.clinica.shared.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonTest {

    // Concrete implementation for testing the abstract class
    private static class TestPerson extends Person {
    }

    @Test
    void testPersonGettersAndSetters() {
        // Arrange
        Person person = new TestPerson();
        Long id = 1L;
        String identification = "1234567890";
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@example.com";
        String phone = "0987654321";

        // Act
        person.setId(id);
        person.setIdentification(identification);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setEmail(email);
        person.setPhone(phone);

        // Assert
        assertEquals(id, person.getId());
        assertEquals(identification, person.getIdentification());
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
        assertEquals(email, person.getEmail());
        assertEquals(phone, person.getPhone());
    }

    @Test
    void testPersonDefaultConstructor() {
        // Arrange & Act
        Person person = new TestPerson();

        // Assert
        assertNull(person.getId());
        assertNull(person.getIdentification());
        assertNull(person.getFirstName());
        assertNull(person.getLastName());
        assertNull(person.getEmail());
        assertNull(person.getPhone());
    }
}
