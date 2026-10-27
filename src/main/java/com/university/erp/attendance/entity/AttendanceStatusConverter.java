package com.university.erp.attendance.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AttendanceStatusConverter implements AttributeConverter<AttendanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(AttendanceStatus status) {
        if (status == null) {
            return null;
        }
        return status == AttendanceStatus.C_WORK ? "C-Work" : status.name();
    }

    @Override
    public AttendanceStatus convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if ("C-Work".equalsIgnoreCase(trimmed) || "C_WORK".equalsIgnoreCase(trimmed)) {
            return AttendanceStatus.C_WORK;
        }
        if ("Present".equalsIgnoreCase(trimmed)) {
            return AttendanceStatus.Present;
        }
        if ("Absent".equalsIgnoreCase(trimmed)) {
            return AttendanceStatus.Absent;
        }
        return AttendanceStatus.valueOf(trimmed);
    }
}
