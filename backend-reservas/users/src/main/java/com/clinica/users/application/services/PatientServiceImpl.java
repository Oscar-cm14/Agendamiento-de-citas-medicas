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

@Service
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientServiceImpl(UserRepository userRepository,
                              PatientRepository patientRepository,
                              PersonRepository personRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {

        // Si el paciente ya existe por identificación, retornar el existente
        Optional<Patient> existingPatient =
                patientRepository.findByIdentification(request.identification());

        if (existingPatient.isPresent()) {
            Patient p = existingPatient.get();
            String fullName = p.getFirstName() + " " + p.getLastName();
            // FIX: usar query eficiente en vez de findAll()
            String username = userRepository.findUsernameByPersonId(p.getId())
                    .orElse(p.getIdentification());
            return new PatientResponse(p.getId(), fullName, username, p.getEmail());
        }

        // Validar username único
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

        User savedUser = userRepository.save(user);
        Patient savedPatient = (Patient) savedUser.getPerson();
        String fullName = savedPatient.getFirstName() + " " + savedPatient.getLastName();

        return new PatientResponse(
                savedPatient.getId(),
                fullName,
                savedUser.getUsername(),
                savedPatient.getEmail()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientDetailResponse> findByIdentification(String identification) {
        return patientRepository.findByIdentification(identification)
                .map(p -> {
                    // FIX: usar query eficiente en vez de findAll()
                    String username = userRepository.findUsernameByPersonId(p.getId())
                            .orElse(p.getIdentification());
                    return new PatientDetailResponse(
                            p.getId(),
                            p.getIdentification(),
                            p.getFirstName(),
                            p.getLastName(),
                            p.getFirstName() + " " + p.getLastName(),
                            p.getEmail(),
                            p.getPhone(),
                            p.getGender(),
                            p.getBirthDate(),
                            username
                    );
                });
    }
}