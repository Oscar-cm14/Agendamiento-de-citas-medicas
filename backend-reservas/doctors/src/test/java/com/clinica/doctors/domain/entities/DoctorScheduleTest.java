package com.clinica.doctors.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DoctorScheduleTest {

    @Test
    void testDoctorScheduleGettersAndSetters() {
        // Arrange
        DoctorSchedule schedule = new DoctorSchedule();
        Long id = 1L;
        Long doctorId = 100L;
        Set<DayOfWeek> workingDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        Integer intervalMinutes = 30;

        // Act
        schedule.setId(id);
        schedule.setDoctorId(doctorId);
        schedule.setWorkingDays(workingDays);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setIntervalMinutes(intervalMinutes);

        // Assert
        assertEquals(id, schedule.getId());
        assertEquals(doctorId, schedule.getDoctorId());
        assertEquals(workingDays, schedule.getWorkingDays());
        assertEquals(startTime, schedule.getStartTime());
        assertEquals(endTime, schedule.getEndTime());
        assertEquals(intervalMinutes, schedule.getIntervalMinutes());
    }

    @Test
    void testDoctorScheduleDefaultConstructor() {
        // Arrange & Act
        DoctorSchedule schedule = new DoctorSchedule();

        // Assert
        assertNull(schedule.getId());
        assertNull(schedule.getDoctorId());
        assertNull(schedule.getWorkingDays());
        assertNull(schedule.getStartTime());
        assertNull(schedule.getEndTime());
        assertNull(schedule.getIntervalMinutes());
    }
}
