package com.clinica.users.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchedulerTest {

    @Test
    void testSchedulerCreation() {
        // Arrange
        Scheduler scheduler = new Scheduler();

        // Act
        scheduler.setFirstName("Jane");
        scheduler.setLastName("Scheduler");
        scheduler.setEmail("jane@clinica.com");

        // Assert
        assertNotNull(scheduler);
        assertNotNull(scheduler.getFirstName());
    }
}
