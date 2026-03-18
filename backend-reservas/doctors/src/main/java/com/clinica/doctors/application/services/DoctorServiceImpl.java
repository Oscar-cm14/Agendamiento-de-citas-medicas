package com.clinica.doctors.application.services;

import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.domain.UserRole;
import com.clinica.users.domain.entities.Doctor;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.DoctorRepository;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the DoctorService handling the business logic
 * for Doctor domain operations.
 */
@Service
public class DoctorServiceImpl implements DoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PersonRepository personRepository;

    /**
     * Constructor injection for required repositories.
     *
     * @param userRepository   The user repository for saving authentication info.
     * @param doctorRepository The doctor repository for saving profile info.
     * @param personRepository The person repository for validation checks.
     */
    public DoctorServiceImpl(UserRepository userRepository, DoctorRepository doctorRepository, PersonRepository personRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.personRepository = personRepository;
    }

    /**
     * Registers a new Doctor and their associated User credentials.
     * Transactional ensures both or neither are saved.
     *
     * @param request Data containing the doctor details.
     * @return A summary record of the registered doctor.
     * @throws IllegalArgumentException if the username or identification already exists.
     */
    @Override
    @Transactional
    public DoctorResponse registerDoctor(DoctorRegistrationRequest request) {

        // 1. Validation checks
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (personRepository.findByIdentification(request.identification()).isPresent()) {
            throw new IllegalArgumentException("Identification already exists.");
        }

        // 2. Create the Doctor Entity
        Doctor doctor = new Doctor();
        doctor.setIdentification(request.identification());
        doctor.setFirstName(request.firstName());
        doctor.setLastName(request.lastName());
        doctor.setEmail(request.email());
        doctor.setPhone(request.phone());
        doctor.setSpecialty(request.specialty());
        doctor.setLicenseNumber(request.licenseNumber());
        doctor.setActive(true);

        // 3. Create the User Entity linked to the Doctor
        User user = new User();
        user.setUsername(request.username());
        // TODO: Encode with BCrypt
        user.setPassword(request.password());
        user.setEnabled(true);
        user.setRole(UserRole.DOCTOR);
        user.setPerson(doctor); // Establish the OneToOne relationship

        // 4. Save User (cascades the person/doctor since CascadeType.ALL is established in User.java)
        User savedUser = userRepository.save(user);

        // Retrieve the generated ID explicitly to return it
        Doctor savedDoctor = (Doctor) savedUser.getPerson();

        String fullName = savedDoctor.getFirstName() + " " + savedDoctor.getLastName();

        return new DoctorResponse(
                savedDoctor.getId(),
                fullName
        );
    }
}
