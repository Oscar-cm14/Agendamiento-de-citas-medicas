package com.clinica.appointments.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemConfigurationTest {

    @Test
    void testSystemConfigurationGettersAndSetters() {
        // Arrange
        SystemConfiguration config = new SystemConfiguration();
        Long id = 1L;
        Integer appointmentWindowWeeks = 8;

        // Act
        config.setId(id);
        config.setAppointmentWindowWeeks(appointmentWindowWeeks);

        // Assert
        assertEquals(id, config.getId());
        assertEquals(appointmentWindowWeeks, config.getAppointmentWindowWeeks());
    }

    @Test
    void testSystemConfigurationDefaultConstructor() {
        // Arrange & Act
        SystemConfiguration config = new SystemConfiguration();

        // Assert
        assertNull(config.getId());
        assertEquals(4, config.getAppointmentWindowWeeks()); // Default value
    }
}
