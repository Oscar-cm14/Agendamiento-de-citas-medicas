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

/**
 * Implementación de SchedulerService.
 *
 * CAMBIOS respecto a la versión original:
 *  - Implementa listSchedulers(), getSchedulerDetailById(), updateScheduler().
 *  - Agrega helper privado toDetailResponse() que mapea Scheduler → SchedulerDetailResponse.
 *  - Busca el username del agendador consultando la tabla User para incluirlo en el DTO.
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

    // =========================================================
    // Registrar agendador (flujo original sin cambios)
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

    // =========================================================
    // NUEVO: Listar todos los agendadores
    // =========================================================

    @Override
    public List<SchedulerDetailResponse> listSchedulers() {
        return schedulerRepository.findAll()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    // =========================================================
    // NUEVO: Detalle de un agendador por ID
    // =========================================================

    @Override
    public SchedulerDetailResponse getSchedulerDetailById(Long id) {
        Scheduler scheduler = schedulerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendador no encontrado con id: " + id));
        return toDetailResponse(scheduler);
    }

    // =========================================================
    // NUEVO: Actualizar datos del agendador
    // =========================================================

    /**
     * Partial update: solo se modifican los campos no-nulos del request.
     * NO modifica username ni contraseña.
     */
    @Override
    @Transactional
    public SchedulerDetailResponse updateScheduler(Long id, SchedulerUpdateRequest request) {

        Scheduler scheduler = schedulerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendador no encontrado con id: " + id));

        if (request.firstName() != null) scheduler.setFirstName(request.firstName());
        if (request.lastName()  != null) scheduler.setLastName(request.lastName());
        if (request.email()     != null) scheduler.setEmail(request.email());
        if (request.phone()     != null) scheduler.setPhone(request.phone());

        // Si cambia la identificación, verificar que no esté en uso por OTRO registro
        if (request.identification() != null) {
            personRepository.findByIdentification(request.identification()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IdentificationAlreadyExistsException(
                        "La identificación '" + request.identification() + "' ya está en uso.");
                }
            });
            scheduler.setIdentification(request.identification());
        }

        Scheduler saved = schedulerRepository.save(scheduler);
        return toDetailResponse(saved);
    }

    // =========================================================
    // Helper privado: mapear Scheduler → SchedulerDetailResponse
    // =========================================================

    /**
     * Busca el username del agendador en la tabla User (relación inversa
     * Person → User) para incluirlo en el DTO de respuesta.
     */
    private SchedulerDetailResponse toDetailResponse(Scheduler s) {
        // Buscar el User cuyo person tiene el mismo id que este scheduler
        String username = userRepository.findAll().stream()
                .filter(u -> u.getPerson() != null && u.getPerson().getId().equals(s.getId()))
                .map(User::getUsername)
                .findFirst()
                .orElse("");

        return new SchedulerDetailResponse(
                s.getId(),
                s.getFirstName() + " " + s.getLastName(),  // fullName calculado
                s.getFirstName(),
                s.getLastName(),
                s.getIdentification(),
                s.getEmail(),
                s.getPhone(),
                username
        );
    }
}