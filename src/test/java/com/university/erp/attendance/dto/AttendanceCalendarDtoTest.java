package com.university.erp.attendance.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceCalendarDtoTest {

    @Test
    void testValidateMonthAndYear_Valid() {
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateMonthAndYear(2026, 10));
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateMonthAndYear(2026, 1));
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateMonthAndYear(2026, 12));
    }

    @Test
    void testValidateMonthAndYear_InvalidMonth() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class,
                () -> AttendanceCalendarValidator.validateMonthAndYear(2026, 0));
        assertTrue(ex0.getMessage().contains("Month must be between 1 and 12"));

        IllegalArgumentException ex13 = assertThrows(IllegalArgumentException.class,
                () -> AttendanceCalendarValidator.validateMonthAndYear(2026, 13));
        assertTrue(ex13.getMessage().contains("Month must be between 1 and 12"));
    }

    @Test
    void testValidateMonthAndYear_InvalidYear() {
        assertThrows(IllegalArgumentException.class, () -> AttendanceCalendarValidator.validateMonthAndYear(1899, 5));
        assertThrows(IllegalArgumentException.class, () -> AttendanceCalendarValidator.validateMonthAndYear(2201, 5));
    }

    @Test
    void testValidateDateRange_Valid() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateDateRange(start, end));

        // Same date
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateDateRange(start, start));

        // Cross-year boundary (December to January)
        LocalDate dec = LocalDate.of(2026, 12, 1);
        LocalDate jan = LocalDate.of(2027, 1, 31);
        assertDoesNotThrow(() -> AttendanceCalendarValidator.validateDateRange(dec, jan));
    }

    @Test
    void testValidateDateRange_Invalid() {
        LocalDate start = LocalDate.of(2026, 10, 31);
        LocalDate end = LocalDate.of(2026, 8, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AttendanceCalendarValidator.validateDateRange(start, end));
        assertTrue(ex.getMessage().contains("cannot be after endDate"));

        assertThrows(IllegalArgumentException.class, () -> AttendanceCalendarValidator.validateDateRange(null, end));
        assertThrows(IllegalArgumentException.class, () -> AttendanceCalendarValidator.validateDateRange(start, null));
    }

    @Test
    void testCalculatePercentage_NormalAndRounding() {
        assertEquals(100.0, AttendanceCalendarValidator.calculatePercentage(20, 0, 20));
        assertEquals(85.0, AttendanceCalendarValidator.calculatePercentage(16, 1, 20));
        assertEquals(83.33, AttendanceCalendarValidator.calculatePercentage(10, 0, 12));
        assertEquals(66.67, AttendanceCalendarValidator.calculatePercentage(2, 0, 3));
    }

    @Test
    void testCalculatePercentage_ZeroTotal() {
        assertEquals(0.0, AttendanceCalendarValidator.calculatePercentage(0, 0, 0));
        assertEquals(0.0, AttendanceCalendarValidator.calculatePercentage(5, 0, -1));
    }

    @Test
    void testMonthDateBoundaries() {
        // Leap year February (2024 has 29 days)
        assertEquals(LocalDate.of(2024, 2, 1), AttendanceCalendarValidator.getMonthStartDate(2024, 2));
        assertEquals(LocalDate.of(2024, 2, 29), AttendanceCalendarValidator.getMonthEndDate(2024, 2));

        // Non-leap year February (2026 has 28 days)
        assertEquals(LocalDate.of(2026, 2, 1), AttendanceCalendarValidator.getMonthStartDate(2026, 2));
        assertEquals(LocalDate.of(2026, 2, 28), AttendanceCalendarValidator.getMonthEndDate(2026, 2));

        // 31-day month (October)
        assertEquals(LocalDate.of(2026, 10, 1), AttendanceCalendarValidator.getMonthStartDate(2026, 10));
        assertEquals(LocalDate.of(2026, 10, 31), AttendanceCalendarValidator.getMonthEndDate(2026, 10));

        // 30-day month (November)
        assertEquals(LocalDate.of(2026, 11, 1), AttendanceCalendarValidator.getMonthStartDate(2026, 11));
        assertEquals(LocalDate.of(2026, 11, 30), AttendanceCalendarValidator.getMonthEndDate(2026, 11));
    }

    @Test
    void testGetMonthName() {
        assertEquals("OCTOBER", AttendanceCalendarValidator.getMonthName(10));
        assertEquals("JANUARY", AttendanceCalendarValidator.getMonthName(1));
        assertEquals("DECEMBER", AttendanceCalendarValidator.getMonthName(12));
    }

    @Test
    void testCalendarDayAttendance_Empty() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        CalendarDayAttendance empty = CalendarDayAttendance.empty(date);

        assertEquals(date, empty.date());
        assertEquals(15, empty.dayOfMonth());
        assertEquals("THURSDAY", empty.dayOfWeek());
        assertEquals(0, empty.totalLectures());
        assertEquals(0, empty.presentCount());
        assertEquals(0, empty.absentCount());
        assertEquals(0, empty.cWorkCount());
        assertTrue(empty.entries().isEmpty());
    }
}
