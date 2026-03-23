package com.clinica.users.application.services;


import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
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

/**
 * Implementation of PatientService.
 * Handles RF3: patient self-registration from the web.
 */
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

    /**
     * RF3: Registers a new patient from the web.
     * Creates both the Patient entity and the User credentials.
     */
    @Override
    @Transactional
    public PatientResponse registerPatient(PatientRegistrationRequest request) {

        // 1. Validaciones
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException("El username ya existe.");
        }
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IdentificationAlreadyExistsException("La identificación ya existe.");
        }

        // 2. Crear entidad Patient
        Patient patient = new Patient();
        patient.setIdentification(request.identification());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate());

        // 3. Crear entidad User vinculada al paciente
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRole(UserRole.PATIENT);
        user.setPerson(patient);

        // 4. Guardar (cascade guarda el patient automáticamente)
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
}