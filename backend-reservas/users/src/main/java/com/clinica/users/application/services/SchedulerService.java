package com.clinica.users.application.services;

import com.clinica.shared.dto.SchedulerDetailResponse;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.dto.SchedulerUpdateRequest;

import java.util.List;

/**
 * Contrato de operaciones para la gestión de agendadores.
 *
 * CAMBIOS respecto a la versión original:
 *  - listSchedulers()           → lista todos los agendadores con detalle completo.
 *  - getSchedulerDetailById()   → obtiene un agendador con todos sus campos.
 *  - updateScheduler()          → actualiza los datos de un agendador existente.
 */
public interface SchedulerService {

    /** Registrar un nuevo agendador (flujo existente). */
    SchedulerResponse registerScheduler(SchedulerRegistrationRequest request);

    /** NUEVO: Listar todos los agendadores con detalle completo. */
    List<SchedulerDetailResponse> listSchedulers();

    /** NUEVO: Obtener un agendador con todos los campos para el formulario de edición. */
    SchedulerDetailResponse getSchedulerDetailById(Long id);

    /**
     * NUEVO: Actualizar los datos de un agendador existente.
     * Solo modifica los campos no-nulos del request (partial update).
     */
    SchedulerDetailResponse updateScheduler(Long id, SchedulerUpdateRequest request);
}