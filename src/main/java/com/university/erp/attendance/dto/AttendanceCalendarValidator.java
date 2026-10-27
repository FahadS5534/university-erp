package com.university.erp.attendance.dto;

import java.time.LocalDate;
import java.time.YearMonth;

public final class AttendanceCalendarValidator {

    private AttendanceCalendarValidator() {
    }

    public static void validateMonthAndYear(int year, int month) {
        if (year < 1900 || year > 2200) {
            throw new IllegalArgumentException("Year must be between 1900 and 2200. Provided: " + year);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12. Provided: " + month);
        }
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate (" + startDate + ") cannot be after endDate (" + endDate + ")");
        }
    }

    public static double calculatePercentage(long presentCount, long cWorkCount, long totalLectures) {
        if (totalLectures <= 0) {
            return 0.0;
        }
        long attended = presentCount + cWorkCount;
        return Math.round(attended * 10000.0 / totalLectures) / 100.0;
    }

    public static LocalDate getMonthStartDate(int year, int month) {
        validateMonthAndYear(year, month);
        return YearMonth.of(year, month).atDay(1);
    }

    public static LocalDate getMonthEndDate(int year, int month) {
        validateMonthAndYear(year, month);
        return YearMonth.of(year, month).atEndOfMonth();
    }

    public static String getMonthName(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12. Provided: " + month);
        }
        return java.time.Month.of(month).name();
    }
}
