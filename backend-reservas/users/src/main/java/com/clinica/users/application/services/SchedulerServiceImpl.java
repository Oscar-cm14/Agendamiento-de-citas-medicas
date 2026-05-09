package com.clinica.users.application.services;



import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.Scheduler;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.SchedulerRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SchedulerService.
 */
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private final UserRepository userRepository;
    private final SchedulerRepository schedulerRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    public SchedulerServiceImpl(UserRepository userRepository,
                                SchedulerRepository schedulerRepository,
                                PersonRepository personRepository,
                                PasswordEncoder passwordEncoder,
                                KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.schedulerRepository = schedulerRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Override
    @Transactional
    public SchedulerResponse registerScheduler(SchedulerRegistrationRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IdentificationAlreadyExistsException("La identificación ya existe.");
        }

        // 1. Guardar en la base de datos local (H2)
        Scheduler scheduler = new Scheduler();
        scheduler.setIdentification(request.identification());
        scheduler.setFirstName(request.firstName());
        scheduler.setLastName(request.lastName());
        scheduler.setPhone(request.phone());
        scheduler.setEmail(request.email());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.SCHEDULER);
        user.setPerson(scheduler);

        User savedUser = userRepository.save(user);
        Scheduler savedScheduler = (Scheduler) savedUser.getPerson();

        // 2. Crear el usuario en Keycloak con rol SCHEDULER
        keycloakAdminService.createUser(
                request.username(),
                request.password(),
                request.email(),
                request.firstName(),
                request.lastName(),
                "SCHEDULER"
        );

        return new SchedulerResponse(
                savedScheduler.getId(),
                savedScheduler.getFirstName() + " " + savedScheduler.getLastName(),
                savedUser.getUsername()
        );
    }
}