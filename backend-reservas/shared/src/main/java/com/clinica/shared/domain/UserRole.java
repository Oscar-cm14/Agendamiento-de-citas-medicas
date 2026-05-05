package com.clinica.shared.domain;

/**
 * Represents the various roles a user can have within the clinic system.
 */
public enum UserRole {

    /**
     * Administrator role with full access to the system.
     */
    ADMIN,

    /**
     * Patient role representing an individual receiving care.
     */
    PATIENT,

    /**
     * Scheduler role responsible for managing appointments.
     */
    SCHEDULER,

    /**
     * Doctor role representing a medical professional providing care.
     */
    DOCTOR;
}
