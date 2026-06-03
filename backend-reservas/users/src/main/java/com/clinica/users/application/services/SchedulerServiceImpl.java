package com.clinica.users.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.SchedulerDetailResponse;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.dto.SchedulerUpdateRequest;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.Scheduler;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.SchedulerRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // =========================================================
    // Registrar agendador
    // =========================================================
    @Override
    @Transactional
    public SchedulerResponse registerScheduler(SchedulerRegistrationRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IdentificationAlreadyExistsException("La identificación ya existe.");
        }

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

        keycloakAdminService.createUser(
                request.username(), request.password(),
                request.email(), request.firstName(), request.lastName(), "SCHEDULER"
        );

        return new SchedulerResponse(
                savedScheduler.getId(),
                savedScheduler.getFirstName() + " " + savedScheduler.getLastName(),
                savedUser.getUsername()
        );
    }

    // =========================================================
    // Listar todos
    // =========================================================
    @Override
    public List<SchedulerDetailResponse> listSchedulers() {
        return schedulerRepository.findAll().stream()
                .map(this::toDetailResponse)
                .toList();
    }

    // =========================================================
    // Detalle por ID
    // =========================================================
    @Override
    public SchedulerDetailResponse getSchedulerDetailById(Long id) {
        Scheduler scheduler = schedulerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendador no encontrado con id: " + id));
        return toDetailResponse(scheduler);
    }

    // =========================================================
    // Actualizar
    // =========================================================
    @Override
    @Transactional
    public SchedulerDetailResponse updateScheduler(Long id, SchedulerUpdateRequest request) {

        Scheduler scheduler = schedulerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendador no encontrado con id: " + id));

        if (request.firstName() != null) scheduler.setFirstName(request.firstName());
        if (request.lastName()  != null) scheduler.setLastName(request.lastName());
        if (request.email()     != null) scheduler.setEmail(request.email());
        if (request.phone()     != null) scheduler.setPhone(request.phone());

        if (request.identification() != null) {
            personRepository.findByIdentification(request.identification()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IdentificationAlreadyExistsException(
                        "La identificación '" + request.identification() + "' ya está en uso.");
                }
            });
            scheduler.setIdentification(request.identification());
        }

        return toDetailResponse(schedulerRepository.save(scheduler));
    }

    // =========================================================
    // NUEVO: Obtener agendador por username (para endpoint /me)
    // =========================================================
    @Override
    public SchedulerDetailResponse getSchedulerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
        if (!(user.getPerson() instanceof Scheduler scheduler)) {
            throw new RuntimeException("El usuario '" + username + "' no es agendador.");
        }
        return toDetailResponse(scheduler);
    }

    // =========================================================
    // Helper privado
    // =========================================================
    private SchedulerDetailResponse toDetailResponse(Scheduler s) {
        String username = userRepository.findAll().stream()
                .filter(u -> u.getPerson() != null && u.getPerson().getId().equals(s.getId()))
                .map(User::getUsername)
                .findFirst()
                .orElse("");

        return new SchedulerDetailResponse(
                s.getId(),
                s.getFirstName() + " " + s.getLastName(),
                s.getFirstName(),
                s.getLastName(),
                s.getIdentification(),
                s.getEmail(),
                s.getPhone(),
                username
        );
    }
}