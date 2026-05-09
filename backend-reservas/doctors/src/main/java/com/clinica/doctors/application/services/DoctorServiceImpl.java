package com.clinica.doctors.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
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

    @Override
    @Transactional
    public DoctorResponse registerDoctor(DoctorRegistrationRequest request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists.");
        }
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IdentificationAlreadyExistsException("Identification already exists.");
        }

        // 1. Guardar en la base de datos local (H2)
        Doctor doctor = new Doctor();
        doctor.setIdentification(request.identification());
        doctor.setFirstName(request.firstName());
        doctor.setLastName(request.lastName());
        doctor.setEmail(request.email());
        doctor.setPhone(request.phone());
        doctor.setSpecialty(request.specialty());
        doctor.setLicenseNumber(request.licenseNumber());
        doctor.setActive(true);

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.DOCTOR);
        user.setPerson(doctor);

        User savedUser = userRepository.save(user);
        Doctor savedDoctor = (Doctor) savedUser.getPerson();

        // 2. Crear el usuario en Keycloak con rol DOCTOR
        keycloakAdminService.createUser(
                request.username(),
                request.password(),
                request.email(),
                request.firstName(),
                request.lastName(),
                "DOCTOR"
        );

        return new DoctorResponse(
                savedDoctor.getId(),
                savedDoctor.getFirstName() + " " + savedDoctor.getLastName(),
                savedDoctor.getSpecialty());
    }

   @Override
public List<DoctorResponse> listDoctors() {
    return doctorRepository.findAll() 
            .stream()
            .map(d -> new DoctorResponse(
                    d.getId(),
                    d.getFirstName() + " " + d.getLastName(),
                    d.getSpecialty()))
            .toList();
}

    @Override
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        return new DoctorResponse(
                doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }

    @Override
    public DoctorResponse getDoctorByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!(user.getPerson() instanceof Doctor doctor)) {
            throw new RuntimeException("El usuario no es un médico");
        }

        return new DoctorResponse(
                doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }

    @Override
    public DoctorResponse getDoctorByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        if (!(user.getPerson() instanceof Doctor doctor)) {
            throw new RuntimeException("El usuario no es un médico");
        }

        return new DoctorResponse(
                doctor.getId(),
                doctor.getFirstName() + " " + doctor.getLastName(),
                doctor.getSpecialty());
    }
}