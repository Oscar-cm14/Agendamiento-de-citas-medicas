package com.clinica.users.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.domain.entities.Person;
import com.clinica.users.domain.entities.Scheduler;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.SchedulerRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchedulerRepository schedulerRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    private SchedulerRegistrationRequest request;

    @BeforeEach
    void setUp() {
        request = new SchedulerRegistrationRequest(
                "0987654321", "Jane", "Smith", "1234567890", "jane@example.com",
                "janesmith", "securepass"
        );
    }

    @Test
    void registerScheduler_Success() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(personRepository.findByIdentification(request.identification())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");

        User savedUser = new User();
        savedUser.setUsername("janesmith");
        Scheduler savedScheduler = new Scheduler();
        savedScheduler.setId(10L);
        savedScheduler.setFirstName("Jane");
        savedScheduler.setLastName("Smith");
        savedUser.setPerson(savedScheduler);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        SchedulerResponse response = schedulerService.registerScheduler(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Jane Smith", response.fullName());
        assertEquals("janesmith", response.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerScheduler_UsernameExists_ThrowsException() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(new User()));

        assertThrows(UsernameAlreadyExistsException.class, () -> schedulerService.registerScheduler(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerScheduler_IdentificationExists_ThrowsException() {
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        
        // Mock Person for repository return
        Person existingPerson = new Scheduler();
        when(personRepository.findByIdentification(request.identification())).thenReturn(Optional.of(existingPerson));

        assertThrows(IdentificationAlreadyExistsException.class, () -> schedulerService.registerScheduler(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
