package com.clinica.appointments.infrastructure.specifications;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.shared.domain.entities.AppointmentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón de Diseño GoF: BUILDER
 * 
 * Propósito: Este patrón permite construir de forma dinámica y paso a paso
 * el objeto Specification de Spring Data JPA que servirá para realizar
 * consultas (queries) complejas a la base de datos de manera programática,
 * evitando concatenar strings SQL de forma insegura.
 */
public class AppointmentSpecificationBuilder {

    private final List<Specification<Appointment>> specifications;

    public AppointmentSpecificationBuilder() {
        this.specifications = new ArrayList<>();
    }

    public AppointmentSpecificationBuilder withDoctorId(Long doctorId) {
        if (doctorId != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("doctorId"), doctorId));
        }
        return this;
    }

    public AppointmentSpecificationBuilder withPatientId(Long patientId) {
        if (patientId != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("patientId"), patientId));
        }
        return this;
    }

    public AppointmentSpecificationBuilder withStatus(AppointmentStatus status) {
        if (status != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        return this;
    }

    public AppointmentSpecificationBuilder withDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.between(root.get("date"), startDate, endDate));
        } else if (startDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
        } else if (endDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
        }
        return this;
    }
    
    public AppointmentSpecificationBuilder withExactDate(LocalDate exactDate) {
        if (exactDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("date"), exactDate));
        }
        return this;
    }

    /**
     * Construye y retorna la Specification final combinando dinámicamente 
     * todos los criterios (queries) agregados mediante AND.
     */
    public Specification<Appointment> build() {
        if (specifications.isEmpty()) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        Specification<Appointment> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = Specification.where(result).and(specifications.get(i));
        }
        return result;
    }
}
