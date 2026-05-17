package com.clinica.users.domain.entities;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.entities.Person;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void testUserGettersAndSetters() {
        // Arrange
        User user = new User();
        Long id = 100L;
        String username = "johndoe";
        String password = "securepassword";
        Boolean enabled = true;
        UserRole role = UserRole.PATIENT;
        Person person = new Patient();
        person.setId(1L);
        person.setFirstName("John");

        // Act
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setEnabled(enabled);
        user.setRole(role);
        user.setPerson(person);

        // Assert
        assertEquals(id, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(password, user.getPassword());
        assertTrue(user.getEnabled());
        assertEquals(role, user.getRole());
        assertEquals(person, user.getPerson());
        assertEquals(1L, user.getPerson().getId());
    }

    @Test
    void testUserDefaultConstructor() {
        // Arrange & Act
        User user = new User();

        // Assert
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getEnabled());
        assertNull(user.getRole());
        assertNull(user.getPerson());
    }
}
