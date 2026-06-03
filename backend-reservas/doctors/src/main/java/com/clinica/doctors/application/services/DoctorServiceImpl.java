package com.clinica.doctors.application.services;



import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.DoctorDetailResponse;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorUpdateRequest;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;

    public DoctorServiceImpl(UserRepository userRepository,
            DoctorRepository doctorRepository,
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAdminService = keycloakAdminService;
    }

    // ── Registrar médico  ────────────────────────
    @Override
    @Transactional
    public DoctorResponse registerDoctor(DoctorRegistrationRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent())
            throw new UsernameAlreadyExistsException("Username already exists.");
        if (personRepository.findByIdentification(request.identification()).isPresent())
            throw new IdentificationAlreadyExistsException("Identification already exists.");

        Doctor doctor = new Doctor();
        doctor.setIdentification(request.identification());
        doctor.setFirstName(request.firstName());
        doctor.setLastName(request.lastName());
        doctor.setEmail(request.email());
        doctor.setPhone(request.phone());
        doctor.setSpecialty(request.specialty());
        doctor.setLicenseNumber(request.licenseNumber());
        doctor.setActive(true);
        // skills se deja null al registrar; el admin las agrega luego en edición

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.DOCTOR);
        user.setPerson(doctor);

        User savedUser = userRepository.save(user);
        Doctor savedDoctor = (Doctor) savedUser.getPerson();

        keycloakAdminService.createUser(
                request.username(), request.password(),
                request.email(), request.firstName(), request.lastName(), "DOCTOR");

        return new DoctorResponse(savedDoctor.getId(),
                savedDoctor.getFirstName() + " " + savedDoctor.getLastName(),
                savedDoctor.getSpecialty());
    }

    // ── Listar  ──────────────────────────────────
    @Override
    public List<DoctorResponse> listDoctors() {
        return doctorRepository.findAll().stream()
                .map(d -> new DoctorResponse(d.getId(),
                        d.getFirstName() + " " + d.getLastName(),
                        d.getSpecialty()))
                .toList();
    }

    // ── Obtener por ID resumido ───────────────────────────────
    @Override
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado: " + id));
        return new DoctorResponse(doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }

    // ── Obtener por ID detallado (para formulario edición) ────
    @Override
    public DoctorDetailResponse getDoctorDetailById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado: " + id));
        return toDetailResponse(doctor);
    }

    // ── Obtener por userId ────────────────────────────────────
    @Override
    public DoctorResponse getDoctorByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        if (!(user.getPerson() instanceof Doctor doctor))
            throw new RuntimeException("El usuario " + userId + " no es médico.");
        return new DoctorResponse(doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }

    // ── Obtener por username ──────────────────────────────────
    @Override
    public DoctorResponse getDoctorByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
        if (!(user.getPerson() instanceof Doctor doctor))
            throw new RuntimeException("El usuario '" + username + "' no es médico.");
        return new DoctorResponse(doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }

    // ──  Actualizar datos del médico ─────────────
    /**
     * Partial-update: solo modifica los campos que llegan no-nulos.
     */
    @Override
    @Transactional
    public DoctorDetailResponse updateDoctor(Long id, DoctorUpdateRequest request) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado: " + id));

        if (request.firstName()     != null) doctor.setFirstName(request.firstName());
        if (request.lastName()      != null) doctor.setLastName(request.lastName());
        if (request.email()         != null) doctor.setEmail(request.email());
        if (request.phone()         != null) doctor.setPhone(request.phone());
        if (request.specialty()     != null) doctor.setSpecialty(request.specialty());
        if (request.licenseNumber() != null) doctor.setLicenseNumber(request.licenseNumber());

        
        // Se permite pasar "" para borrar todas las habilidades
        if (request.skills() != null) doctor.setSkills(request.skills());

        if (request.identification() != null) {
            personRepository.findByIdentification(request.identification()).ifPresent(existing -> {
                if (!existing.getId().equals(id))
                    throw new IdentificationAlreadyExistsException(
                            "La identificación '" + request.identification() + "' ya está en uso.");
            });
            doctor.setIdentification(request.identification());
        }

        return toDetailResponse(doctorRepository.save(doctor));
    }

    
    /**
     * Mapea todos los campos del médico al DTO de detalle.
     */
    private DoctorDetailResponse toDetailResponse(Doctor d) {
        return new DoctorDetailResponse(
                d.getId(),
                d.getFirstName() + " " + d.getLastName(),
                d.getFirstName(),
                d.getLastName(),
                d.getIdentification(),
                d.getEmail(),
                d.getPhone(),
                d.getSpecialty(),
                d.getLicenseNumber(),
                d.getSkills()   
        );
    }
}