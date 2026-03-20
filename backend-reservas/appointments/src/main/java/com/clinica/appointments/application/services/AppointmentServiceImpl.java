package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.shared.domain.AppointmentStatus;
import com.clinica.shared.dto.ManualAppointmentRequest;
import com.clinica.users.domain.entities.Doctor;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.domain.entities.Person;
import com.clinica.users.infrastructure.repositories.DoctorRepository;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the AppointmentService handling business logic for appointments.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PersonRepository personRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Constructor injection for required repositories.
     *
     * @param appointmentRepository The appointment repository.
     * @param personRepository      The person repository to find patients.
     * @param doctorRepository      The doctor repository.
     */
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PersonRepository personRepository, DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.personRepository = personRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Schedules a manual appointment respecting the business rules.
     *
     * @param request Data to schedule the appointment.
     */
    @Override
    @Transactional
    public void scheduleManualAppointment(ManualAppointmentRequest request) {
        
        // 1. Find Patient
        Person person = personRepository.findById(request.patientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + request.patientId()));
        
        if (!(person instanceof Patient)) {
            throw new IllegalArgumentException("The provided ID does not correspond to a Patient.");
        }
        Patient patient = (Patient) person;

        // 2. Find Doctor
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + request.doctorId()));

        // TODO: Validate that the doctor has schedules (intervals) that cover this specific dateTime and day of week.

        // 3. Validate Doctor Availability
        boolean isOccupied = appointmentRepository.existsByDoctorIdAndAppointmentDateTime(
                doctor.getId(), request.dateTime()
        );

        if (isOccupied) {
            throw new IllegalStateException("The doctor already has an appointment exactly at " + request.dateTime());
        }

        // 4. Create and Save Appointment
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(request.dateTime());
        appointment.setObservation(request.observation());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        appointmentRepository.save(appointment);
    }
}
