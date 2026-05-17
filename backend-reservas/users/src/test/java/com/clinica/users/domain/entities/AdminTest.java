package com.clinica.users.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminTest {

    @Test
    void testAdminCreation() {
        // Arrange
        Admin admin = new Admin();

        // Act
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        admin.setEmail("admin@clinica.com");

        // Assert
        assertNotNull(admin);
        assertNotNull(admin.getFirstName());
    }
}
