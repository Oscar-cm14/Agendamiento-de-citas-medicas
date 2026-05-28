package com.clinica.doctors.application.services;

import com.clinica.shared.dto.DoctorDetailResponse;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorUpdateRequest;

import java.util.List;

/**
 * Contrato de operaciones disponibles para la gestión de médicos.
 *
 * CAMBIOS respecto a la versión original:
 *  - getDoctorDetailById(Long id) → devuelve TODOS los campos del médico
 *    para precargar el formulario de edición en el panel admin.
 *  - updateDoctor(Long id, DoctorUpdateRequest request) → actualiza los datos
 *    del médico y retorna el detalle actualizado.
 */
public interface DoctorService {

    /** Registrar un nuevo médico (flujo existente). */
    DoctorResponse registerDoctor(DoctorRegistrationRequest request);

    /** Listar todos los médicos (solo id, fullName, specialty). */
    List<DoctorResponse> listDoctors();

    /** Obtener médico por id con respuesta resumida (legacy). */
    DoctorResponse getDoctorById(Long id);

    /**
     * Obtener médico por id con TODOS los campos para el formulario de edición.
     * Retorna: id, fullName, firstName, lastName, identification,
     *          email, phone, specialty, licenseNumber.
     */
    DoctorDetailResponse getDoctorDetailById(Long id);

    /** Obtener médico a partir del id del usuario (legacy). */
    DoctorResponse getDoctorByUserId(Long userId);

    /** Obtener médico a partir del username de Keycloak. */
    DoctorResponse getDoctorByUsername(String username);

    /**
     * Actualizar los datos personales/profesionales de un médico.
     * Solo modifica los campos que vienen no-nulos en el request.
     * NO cambia username ni contraseña (eso se gestiona en Keycloak aparte).
     */
    DoctorDetailResponse updateDoctor(Long id, DoctorUpdateRequest request);
}