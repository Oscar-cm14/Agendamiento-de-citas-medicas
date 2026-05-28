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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación de PatientService.
 *
 */
@Service
public class PatientServiceImpl implements PatientService {

    private final UserRepository       userRepository;
    private final PatientRepository    patientRepository;
    private final PersonRepository     personRepository;
    private final PasswordEncoder      passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    // Constructor con todas las dependencias que necesita este servicio
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

    // =========================================================
    // RF3 – Registrar paciente desde la web
    // =========================================================

    /**
     * Registra un nuevo paciente desde el formulario web.
     * Si la cédula ya existe retorna el paciente existente sin error,
     * para que el agendador pueda reutilizar el registro.
     */
    @Override
    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {

        // Si el paciente ya existe por cédula, retornar el existente
        Optional<Patient> existingPatient =
                patientRepository.findByIdentification(request.identification());

        if (existingPatient.isPresent()) {
            Patient p       = existingPatient.get();
            String fullName = p.getFirstName() + " " + p.getLastName();
            // Buscar el username del User vinculado a este Patient
            String username = userRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null
                              && u.getPerson().getId().equals(p.getId()))
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(p.getIdentification());
            return new PatientResponse(p.getId(), fullName, username, p.getEmail());
        }

        // Validar que el username no esté ya en uso por otro usuario
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }

        // Crear la entidad Patient con los datos del formulario
        Patient patient = new Patient();
        patient.setIdentification(request.identification());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate());

        // Crear la entidad User que autentica al paciente
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.PATIENT);
        user.setPerson(patient); // cascade guarda Patient automáticamente

        User    savedUser    = userRepository.save(user);
        Patient savedPatient = (Patient) savedUser.getPerson();
        String  fullName     = savedPatient.getFirstName() + " " + savedPatient.getLastName();

        // Registrar también en Keycloak para el login con JWT
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
                savedPatient.getEmail()
        );
    }

    // =========================================================
    // Buscar por cédula
    // =========================================================

    /**
     * Busca un paciente por su número de cédula.
     * Endpoint: GET /api/v1/patients/by-identification?identification=CEDULA
     * Usado por el panel del agendador para autocompletar el formulario.
     */
    @Override
    public Optional<PatientDetailResponse> findByIdentification(String identification) {
        return patientRepository.findByIdentification(identification)
                .map(this::construirDetailResponse);
    }

    // =========================================================
    // Buscar por username
    // =========================================================

    /**
     * Busca un paciente por su username de login.
     * Endpoint: GET /api/v1/patients/by-username?username=...
     * Usado al iniciar sesión para obtener el patientId numérico.
     */
    @Override
    public Optional<PatientDetailResponse> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPerson() instanceof Patient) // solo si es un paciente
                .map(u -> construirDetailResponse((Patient) u.getPerson()));
    }

    // =========================================================
    // Actualizar perfil del paciente
    // =========================================================

    /**
     * Actualiza los datos editables de un paciente.
     * Endpoint: PUT /api/v1/patients/{id}
     * Campos que NO se modifican: identification (cédula), username, password.
     *
     * Lanza RuntimeException si no existe el paciente → controller devuelve 500.
     * (Si se prefiere 404, cambiar a retornar Optional y ajustar el controller.)
     */
    @Override
    @Transactional
    public PatientDetailResponse updatePatient(Long patientId, PatientUpdateRequest request) {

        // Buscar el paciente; lanza error si no existe
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException(
                        "Paciente no encontrado con id: " + patientId));

        // Actualizar solo los campos permitidos
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setEmail(request.email());
        if (request.phone() != null)     patient.setPhone(request.phone());
        if (request.gender() != null)    patient.setGender(request.gender());
        if (request.birthDate() != null) patient.setBirthDate(request.birthDate());

        // Guardar y devolver el perfil actualizado
        patientRepository.save(patient);
        return construirDetailResponse(patient);
    }

    // =========================================================
    // Buscar por ID
    // =========================================================

    /**
     * Busca un paciente por su ID numérico.
     * Endpoint: GET /api/v1/patients/by-id/{id}
     */
    @Override
    public Optional<PatientDetailResponse> findById(Long id) {
        return patientRepository.findById(id)
                .map(this::construirDetailResponse);
    }

    // =========================================================
    // Helper privado: construye PatientDetailResponse
    // =========================================================

    /**
     * Mapea una entidad Patient al DTO de respuesta completo.
     * Reutilizado por todos los métodos de búsqueda para evitar duplicación.
     */
    private PatientDetailResponse construirDetailResponse(Patient p) {
        String fullName = p.getFirstName() + " " + p.getLastName();

        // Buscar el username del User vinculado a este Patient
        String username = userRepository.findAll().stream()
                .filter(u -> u.getPerson() != null
                          && u.getPerson().getId().equals(p.getId()))
                .map(User::getUsername)
                .findFirst()
                .orElse(p.getIdentification()); // fallback: usar la cédula

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