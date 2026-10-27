package com.university.erp.attendance.service;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceResponse create(AttendanceRequest request);

    AttendanceResponse get(Long id);

    List<AttendanceResponse> list(Long lectureId, UUID studentId, AttendanceStatus status);

    List<AttendanceResponse> list(Long lectureId, UUID studentId, AttendanceStatus status, Long subjectId,
            String division, LocalDate startDate, LocalDate endDate);

    Page<AttendanceResponse> listPaged(Long lectureId, UUID studentId, AttendanceStatus status, Long subjectId,
            String division, LocalDate startDate, LocalDate endDate, Pageable pageable);

    AttendanceResponse update(Long id, AttendanceRequest request);

    void delete(Long id);

    List<AttendanceResponse> markBulk(List<AttendanceRequest> requests);

    AttendanceSummary summary(UUID studentId, Long lectureId);

    List<SubjectAttendanceSummary> getSubjectSummaries(UUID studentId);

    LectureAttendanceRegister getLectureRegister(Long lectureId);

    StudentAttendanceHistoryResponse getStudentHistory(UUID studentId, Long subjectId, LocalDate startDate,
            LocalDate endDate);

    List<AttendanceDefaulterResponse> getDefaulters(Double threshold, Long subjectId, String division);
}
