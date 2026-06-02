package com.clinica.users.application.services;

import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private PatientServiceImpl patientService;

    private PatientRegistrationRequest request;
    private Patient existingPatient;
    private User existingUser;

    @BeforeEach
    void setUp() {
        request = new PatientRegistrationRequest(
                "1234567890",
                "John",
                "Doe",
                "0987654321",
                "Hombre",
                LocalDate.of(1990, 1, 1),
                "john@example.com",
                "johndoe",
                "password123"
        );

        existingPatient = new Patient();
        existingPatient.setId(1L);
        existingPatient.setIdentification("1234567890");
        existingPatient.setFirstName("John");
        existingPatient.setLastName("Doe");
        existingPatient.setEmail("john@example.com");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("johndoe");
        existingUser.setPerson(existingPatient);
    }

    @Test
    void registerPatient_NewPatient_Success() {
        when(patientRepository.findByIdentification(request.identification()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.username()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encodedPassword");
        doNothing().when(keycloakAdminService).createUser(
                any(), any(), any(), any(), any(), any()
        );

        User savedUser = new User();
        savedUser.setUsername("johndoe");

        Patient newPatient = new Patient();
        newPatient.setId(2L);
        newPatient.setFirstName("John");
        newPatient.setLastName("Doe");
        newPatient.setEmail("john@example.com");
        savedUser.setPerson(newPatient);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        PatientResponse response = patientService.registerPatient(request);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("johndoe", response.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerPatient_ExistingPatient_ReturnsExisting() {

        // Sin contraseña → solo retorna el existente, no actualiza nada
        PatientRegistrationRequest requestSinPassword = new PatientRegistrationRequest(
                "1234567890",
                "John",
                "Doe",
                "0987654321",
                "Hombre",
                LocalDate.of(1990, 1, 1),
                "john@example.com",
                "johndoe",
                null  // ← sin contraseña
        );

        when(patientRepository.findByIdentification(requestSinPassword.identification()))
                .thenReturn(Optional.of(existingPatient));
        when(userRepository.findAll())
                .thenReturn(List.of(existingUser));

        PatientResponse response = patientService.registerPatient(requestSinPassword);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("johndoe", response.username());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPatient_UsernameExists_ThrowsException() {
        when(patientRepository.findByIdentification(request.identification()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.username()))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> patientService.registerPatient(request)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByIdentification_Exists_ReturnsDetail() {
        when(patientRepository.findByIdentification("1234567890"))
                .thenReturn(Optional.of(existingPatient));
        when(userRepository.findAll())
                .thenReturn(List.of(existingUser));

        Optional<PatientDetailResponse> result =
                patientService.findByIdentification("1234567890");

        assertTrue(result.isPresent());
        assertEquals("1234567890", result.get().identification());
        assertEquals("John Doe", result.get().fullName());
        assertEquals("johndoe", result.get().username());
    }

    @Test
    void findByIdentification_NotExists_ReturnsEmpty() {
        when(patientRepository.findByIdentification("9999999999"))
                .thenReturn(Optional.empty());

        Optional<PatientDetailResponse> result =
                patientService.findByIdentification("9999999999");

        assertFalse(result.isPresent());
    }
}