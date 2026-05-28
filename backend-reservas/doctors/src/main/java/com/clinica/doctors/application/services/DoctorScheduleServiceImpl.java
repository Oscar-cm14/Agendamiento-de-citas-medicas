package com.clinica.doctors.application.services;

import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import com.clinica.shared.dto.DoctorScheduleRequest;
import com.clinica.shared.dto.DoctorScheduleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of DoctorScheduleService.
 * Handles RF4: admin configures doctor schedules.
 */
@Service
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public DoctorScheduleServiceImpl(DoctorScheduleRepository scheduleRepository,
                                     DoctorRepository doctorRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * RF4: Crea o actualiza el horario de un médico.
     */
    @Override
    @Transactional
    public DoctorScheduleResponse saveSchedule(DoctorScheduleRequest request) {

        // Verificar que el médico existe
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        // Si ya tiene horario lo actualiza, si no crea uno nuevo
        DoctorSchedule schedule = scheduleRepository
                .findByDoctorId(request.doctorId())
                .orElse(new DoctorSchedule());

        schedule.setDoctorId(request.doctorId());
        schedule.setWorkingDays(request.workingDays());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setIntervalMinutes(request.intervalMinutes());

        DoctorSchedule saved = scheduleRepository.save(schedule);

        return toResponse(saved, doctor);
    }

    /**
     * RF4: Obtiene el horario configurado de un médico
     */
    @Override
    public DoctorScheduleResponse getScheduleByDoctorId(Long doctorId) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        DoctorSchedule schedule = scheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Horario no configurado para este médico"));

        return toResponse(schedule, doctor);
    }

    /**
     * Convierte entidad a DTO de respuesta.
     */
    private DoctorScheduleResponse toResponse(DoctorSchedule schedule, Doctor doctor) {
        String doctorName = doctor.getFirstName() + " " + doctor.getLastName();
        return new DoctorScheduleResponse(
                schedule.getId(),
                schedule.getDoctorId(),
                doctorName,
                schedule.getWorkingDays(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getIntervalMinutes()
        );
    }
}