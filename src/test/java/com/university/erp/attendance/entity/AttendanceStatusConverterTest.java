package com.university.erp.attendance.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceStatusConverterTest {

    private AttendanceStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AttendanceStatusConverter();
    }

    @Test
    void testConvertToDatabaseColumn() {
        assertEquals("Present", converter.convertToDatabaseColumn(AttendanceStatus.Present));
        assertEquals("Absent", converter.convertToDatabaseColumn(AttendanceStatus.Absent));
        assertEquals("C-Work", converter.convertToDatabaseColumn(AttendanceStatus.C_WORK));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        assertEquals(AttendanceStatus.Present, converter.convertToEntityAttribute("Present"));
        assertEquals(AttendanceStatus.Present, converter.convertToEntityAttribute("present"));
        assertEquals(AttendanceStatus.Absent, converter.convertToEntityAttribute("Absent"));
        assertEquals(AttendanceStatus.Absent, converter.convertToEntityAttribute("absent"));
        assertEquals(AttendanceStatus.C_WORK, converter.convertToEntityAttribute("C-Work"));
        assertEquals(AttendanceStatus.C_WORK, converter.convertToEntityAttribute("c-work"));
        assertEquals(AttendanceStatus.C_WORK, converter.convertToEntityAttribute("C_WORK"));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void testConvertToEntityAttribute_InvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("INVALID_STATUS"));
    }
}
