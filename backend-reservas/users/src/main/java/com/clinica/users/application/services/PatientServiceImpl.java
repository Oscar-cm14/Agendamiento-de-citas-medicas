package com.clinica.users.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
import com.clinica.shared.dto.PatientUpdateRequest;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PatientServiceImpl implements PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceImpl.class);

    private final UserRepository       userRepository;
    private final PatientRepository    patientRepository;
    private final PersonRepository     personRepository;
    private final PasswordEncoder      passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    public PatientServiceImpl(UserRepository userRepository,
                              PatientRepository patientRepository,
                              PersonRepository personRepository,
                              PasswordEncoder passwordEncoder,
                              KeycloakAdminService keycloakAdminService) {
        this.userRepository       = userRepository;
        this.patientRepository    = patientRepository;
        this.personRepository     = personRepository;
        this.passwordEncoder      = passwordEncoder;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Override
    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {

        Optional<Patient> existingPatient =
                patientRepository.findByIdentification(request.identification());

        if (existingPatient.isPresent()) {
            Patient p       = existingPatient.get();
            String fullName = p.getFirstName() + " " + p.getLastName();

            String username = userRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null
                              && u.getPerson().getId().equals(p.getId()))
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(p.getIdentification());

            if (request.password() != null && !request.password().isBlank()) {

                // 1. Intentar actualizar contraseña en Keycloak
                boolean keycloakActualizado = false;
                try {
                    keycloakAdminService.updatePassword(username, request.password());
                    keycloakActualizado = true;
                    log.info("Re-sync: contraseña actualizada en Keycloak para '{}'", username);
                } catch (Exception e) {
                    log.warn("Re-sync: no se pudo actualizar contraseña en Keycloak para '{}': {}", username, e.getMessage());
                }

                // 2. Si no existía en Keycloak, crearlo ahora
                if (!keycloakActualizado) {
                    try {
                        keycloakAdminService.createUser(
                                username,
                                request.password(),
                                p.getEmail() != null ? p.getEmail() : "",
                                p.getFirstName(),
                                p.getLastName(),
                                "PATIENT"
                        );
                        log.info("Re-sync: usuario '{}' creado en Keycloak.", username);
                        keycloakActualizado = true;
                    } catch (Exception e) {
                        log.error("Re-sync: no se pudo crear '{}' en Keycloak: {}", username, e.getMessage());
                    }
                }

                // 3. Actualizar contraseña en H2
                userRepository.findAll().stream()
                        .filter(u -> u.getPerson() != null
                                  && u.getPerson().getId().equals(p.getId()))
                        .findFirst()
                        .ifPresent(u -> {
                            u.setPassword(passwordEncoder.encode(request.password()));
                            userRepository.save(u);
                        });

                if (!keycloakActualizado) {
                    log.error("Re-sync FALLIDO para '{}': el usuario no pudo sincronizarse con Keycloak.", username);
                }
            }

            return new PatientResponse(p.getId(), fullName, username, p.getEmail(), true);
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }

        Patient patient = new Patient();
        patient.setIdentification(request.identification());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.PATIENT);
        user.setPerson(patient);

        User    savedUser    = userRepository.save(user);
        Patient savedPatient = (Patient) savedUser.getPerson();
        String  fullName     = savedPatient.getFirstName() + " " + savedPatient.getLastName();

        keycloakAdminService.createUser(
                request.username(),
                request.password(),
                request.email(),
                request.firstName(),
                request.lastName(),
                "PATIENT"
        );

        return new PatientResponse(
                savedPatient.getId(),
                fullName,
                savedUser.getUsername(),
                savedPatient.getEmail(),
                false
        );
    }

    @Override
    public Optional<PatientDetailResponse> findByIdentification(String identification) {
        return patientRepository.findByIdentification(identification)
                .map(this::construirDetailResponse);
    }

    @Override
    public Optional<PatientDetailResponse> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPerson() instanceof Patient)
                .map(u -> construirDetailResponse((Patient) u.getPerson()));
    }

    @Override
    @Transactional
    public PatientDetailResponse updatePatient(Long patientId, PatientUpdateRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException(
                        "Paciente no encontrado con id: " + patientId));

        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setEmail(request.email());
        if (request.phone() != null)     patient.setPhone(request.phone());
        if (request.gender() != null)    patient.setGender(request.gender());
        if (request.birthDate() != null) patient.setBirthDate(request.birthDate());

        patientRepository.save(patient);
        return construirDetailResponse(patient);
    }

    @Override
    public Optional<PatientDetailResponse> findById(Long id) {
        return patientRepository.findById(id)
                .map(this::construirDetailResponse);
    }

    private PatientDetailResponse construirDetailResponse(Patient p) {
        String fullName = p.getFirstName() + " " + p.getLastName();

        String username = userRepository.findAll().stream()
                .filter(u -> u.getPerson() != null
                          && u.getPerson().getId().equals(p.getId()))
                .map(User::getUsername)
                .findFirst()
                .orElse(p.getIdentification());

        return new PatientDetailResponse(
                p.getId(),
                p.getIdentification(),
                p.getFirstName(),
                p.getLastName(),
                fullName,
                p.getEmail(),
                p.getPhone(),
                p.getGender(),
                p.getBirthDate(),
                username
        );
    }
}