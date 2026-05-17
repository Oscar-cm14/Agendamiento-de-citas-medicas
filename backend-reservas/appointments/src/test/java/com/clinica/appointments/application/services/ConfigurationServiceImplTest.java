package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.SystemConfiguration;
import com.clinica.appointments.infrastructure.repositories.ConfigurationRepository;
import com.clinica.shared.dto.ConfigurationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceImplTest {

    @Mock
    private ConfigurationRepository configurationRepository;

    @InjectMocks
    private ConfigurationServiceImpl configurationService;

    private SystemConfiguration existingConfig;

    @BeforeEach
    void setUp() {
        existingConfig = new SystemConfiguration();
        existingConfig.setId(1L);
        existingConfig.setAppointmentWindowWeeks(4);
    }

    @Test
    void getGlobalConfiguration_Exists_ReturnsConfig() {
        when(configurationRepository.findAll()).thenReturn(List.of(existingConfig));

        SystemConfiguration result = configurationService.getGlobalConfiguration();

        assertNotNull(result);
        assertEquals(4, result.getAppointmentWindowWeeks());
        verify(configurationRepository, never()).save(any(SystemConfiguration.class));
    }

    @Test
    void getGlobalConfiguration_NotExists_CreatesDefault() {
        when(configurationRepository.findAll()).thenReturn(List.of());
        when(configurationRepository.save(any(SystemConfiguration.class))).thenAnswer(i -> i.getArguments()[0]);

        SystemConfiguration result = configurationService.getGlobalConfiguration();

        assertNotNull(result);
        assertEquals(4, result.getAppointmentWindowWeeks());
        verify(configurationRepository).save(any(SystemConfiguration.class));
    }

    @Test
    void updateWindowWeeks_Success() {
        ConfigurationRequest request = new ConfigurationRequest(8);
        when(configurationRepository.findAll()).thenReturn(List.of(existingConfig));
        when(configurationRepository.save(any(SystemConfiguration.class))).thenAnswer(i -> i.getArguments()[0]);

        SystemConfiguration result = configurationService.updateWindowWeeks(request);

        assertNotNull(result);
        assertEquals(8, result.getAppointmentWindowWeeks());
        verify(configurationRepository).save(existingConfig);
    }
}
