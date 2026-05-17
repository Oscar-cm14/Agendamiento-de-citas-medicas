package com.clinica.doctors.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.shared.domain.entities.Person;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private DoctorRegistrationRequest request;
    private Doctor mockDoctor;

    @BeforeEach
    void setUp() {
        request = new DoctorRegistrationRequest(
                "0987654321", "Gregory", "House", "1234567890", "house@example.com",
                "Diagnostician", "MED-111", "drhouse", "vicodin"
        );

        mockDoctor = new Doctor();
        mockDoctor.setId(10L);
        mockDoctor.setFirstName("Gregory");
        mockDoctor.setLastName("House");
        mockDoctor.setSpecialty("Diagnostician");
    }

    @Test
    void registerDoctor_Success() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(personRepository.findByIdentification(request.identification())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setUsername("drhouse");
        savedUser.setPerson(mockDoctor);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        DoctorResponse response = doctorService.registerDoctor(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Gregory House", response.fullName());
        assertEquals("Diagnostician", response.specialty());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerDoctor_UsernameExists_ThrowsException() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(new User()));

        assertThrows(UsernameAlreadyExistsException.class, () -> doctorService.registerDoctor(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerDoctor_IdentificationExists_ThrowsException() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        
        Person existingPerson = new Doctor();
        when(personRepository.findByIdentification(request.identification())).thenReturn(Optional.of(existingPerson));

        assertThrows(IdentificationAlreadyExistsException.class, () -> doctorService.registerDoctor(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listDoctors_ReturnsListOfDoctors() {
        when(doctorRepository.findAll()).thenReturn(List.of(mockDoctor));

        List<DoctorResponse> responses = doctorService.listDoctors();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Gregory House", responses.get(0).fullName());
    }

    @Test
    void getDoctorById_Exists_ReturnsDoctor() {
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(mockDoctor));

        DoctorResponse response = doctorService.getDoctorById(10L);

        assertNotNull(response);
        assertEquals("Gregory House", response.fullName());
    }

    @Test
    void getDoctorById_NotExists_ThrowsException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> doctorService.getDoctorById(99L));
    }
}
