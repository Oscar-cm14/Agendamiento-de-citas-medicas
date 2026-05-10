package com.clinica.users.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
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
 * Implementation of PatientService.
 * Handles RF3: patient self-registration from the web.
 *
 * CORRECCIÓN: se implementó el método findByUsername(String) que
 * estaba declarado en la interfaz PatientService pero faltaba aquí,
 * lo que causaba el error de compilación:
 *   "PatientServiceImpl must implement the inherited abstract method
 *    PatientService.findByUsername(String)"
 */
@Service
public class PatientServiceImpl implements PatientService {

    private final UserRepository     userRepository;
    private final PatientRepository  patientRepository;
    private final PersonRepository   personRepository;
    private final PasswordEncoder    passwordEncoder;

    public PatientServiceImpl(UserRepository userRepository,
                              PatientRepository patientRepository,
                              PersonRepository personRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository    = userRepository;
        this.patientRepository = patientRepository;
        this.personRepository  = personRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    // =========================================================
    // RF3 – Registrar paciente desde la web
    // =========================================================

    /**
     * Registra un nuevo paciente desde la web.
     * Si la cédula ya existe, retorna los datos del paciente existente
     * en lugar de lanzar error, para que el agendador pueda reutilizarlo.
     */
    @Override
    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {

        // Si el paciente ya existe por identificación, retornar el existente
        Optional<Patient> existingPatient =
                patientRepository.findByIdentification(request.identification());

        if (existingPatient.isPresent()) {
            Patient p        = existingPatient.get();
            String fullName  = p.getFirstName() + " " + p.getLastName();
            String username  = userRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null
                              && u.getPerson().getId().equals(p.getId()))
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(p.getIdentification());
            return new PatientResponse(p.getId(), fullName, username, p.getEmail());
        }

        // Validar que el username no esté ya en uso
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }

        // Crear entidad Patient
        Patient patient = new Patient();
        patient.setIdentification(request.identification());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate());

        // Crear entidad User vinculada al paciente
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.PATIENT);
        user.setPerson(patient);

        // Guardar (cascade guarda el Patient automáticamente)
        User    savedUser    = userRepository.save(user);
        Patient savedPatient = (Patient) savedUser.getPerson();
        String  fullName     = savedPatient.getFirstName() + " " + savedPatient.getLastName();

        return new PatientResponse(
                savedPatient.getId(),
                fullName,
                savedUser.getUsername(),
                savedPatient.getEmail()
        );
    }

    // =========================================================
    // Buscar por cédula (identification)
    // =========================================================

    /**
     * Busca un paciente por cédula y retorna sus datos completos.
     * Usado para autocompletar el formulario en el panel del agendador.
     * Endpoint: GET /api/v1/patients/by-identification?identification=CEDULA
     */
    @Override
    public Optional<PatientDetailResponse> findByIdentification(String identification) {
        return patientRepository.findByIdentification(identification)
                .map(p -> construirDetailResponse(p));
    }

    // =========================================================
    // Buscar por username  ←  MÉTODO QUE FALTABA
    // =========================================================

    /**
     * Busca un paciente por su username (el login que usó al registrarse).
     *
     * En Keycloak el preferred_username puede ser la cédula o el email.
     * Este método busca al User por username en la tabla users y luego
     * verifica que sea un Patient (no un Doctor o Scheduler).
     *
     * Usado por el login del frontend para obtener el patientId numérico
     * y guardarlo en localStorage para las peticiones de citas.
     *
     * Endpoint: GET /api/v1/patients/by-username?username=CEDULA
     */
    @Override
    public Optional<PatientDetailResponse> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPerson() instanceof Patient)   // solo si es un paciente
                .map(u -> {
                    Patient p = (Patient) u.getPerson();
                    return construirDetailResponse(p);
                });
    }

    // =========================================================
    // Helper privado: construye el PatientDetailResponse
    // =========================================================

    /**
     * Construye un PatientDetailResponse a partir de un Patient.
     * Se usa tanto en findByIdentification como en findByUsername
     * para evitar duplicar el código de mapeo.
     * Patrón: extracción de método privado (principio DRY).
     */
    private PatientDetailResponse construirDetailResponse(Patient p) {
        String fullName = p.getFirstName() + " " + p.getLastName();

        // Buscar el username del User vinculado a este Patient
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