package com.clinica.users.application.services;

import com.clinica.shared.domain.UserRole;
import com.clinica.shared.domain.exceptions.IdentificationAlreadyExistsException;
import com.clinica.shared.domain.exceptions.UsernameAlreadyExistsException;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of the PatientService handling the business logic
 * for Patient domain operations.
 */
@Service
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection for required repositories and password encoder.
     *
     * @param userRepository   The user repository.
     * @param personRepository The person repository.
     * @param passwordEncoder  The password encoder.
     */
    public PatientServiceImpl(UserRepository userRepository, PersonRepository personRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new Patient. If an email is provided, a User account
     * is created with the PATIENT role and a temporary password assigned.
     * If no email is provided, only the Patient entity is saved (e.g. booked via WhatsApp).
     *
     * @param request Data containing the patient details.
     */
    @Override
    @Transactional
    public void registerPatient(PatientRegistrationRequest request) {

        // 1. Validation check for Identification
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IdentificationAlreadyExistsException("Identification already exists.");
        }

        // 2. Create Patient Entity
        Patient patient = new Patient();
        patient.setIdentification(request.identification());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setEmail(request.email()); // Can be null
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate()); // Can be null

        // 3. User Account creation (Conditional)
        if (request.email() != null && !request.email().isBlank()) {
            // Check if username/email already exists as a user
            if (userRepository.findByUsername(request.email()).isPresent()) {
                throw new UsernameAlreadyExistsException("A user with this email already exists.");
            }

            User user = new User();
            user.setUsername(request.email());
            // Assign a temporary, random password and encode it
            String tempPassword = UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setEnabled(true);
            user.setRole(UserRole.PATIENT);
            user.setPerson(patient); // Link Patient to User

            // Saving the User will cascade and save the Patient entity due to CascadeType.ALL
            userRepository.save(user);
        } else {
            // No email provided, just save the Patient entity via the inheritance mapped table Person
            personRepository.save(patient);
        }
    }
}
