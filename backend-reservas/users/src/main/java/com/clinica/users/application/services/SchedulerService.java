package com.clinica.users.application.services;

import com.clinica.shared.dto.SchedulerDetailResponse;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.dto.SchedulerUpdateRequest;

import java.util.List;

public interface SchedulerService {

    /** Registrar un nuevo agendador. */
    SchedulerResponse registerScheduler(SchedulerRegistrationRequest request);

    /** Listar todos los agendadores (solo ADMIN). */
    List<SchedulerDetailResponse> listSchedulers();

    /** Detalle de un agendador por ID. */
    SchedulerDetailResponse getSchedulerDetailById(Long id);

    /** Actualizar datos de un agendador. */
    SchedulerDetailResponse updateScheduler(Long id, SchedulerUpdateRequest request);

    /** Obtener el agendador por username (para el endpoint /me). */
    SchedulerDetailResponse getSchedulerByUsername(String username);
}