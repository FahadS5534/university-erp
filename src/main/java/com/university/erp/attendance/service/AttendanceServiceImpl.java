package com.university.erp.attendance.service;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.Attendance;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.mapper.AttendanceMapper;
import com.university.erp.attendance.repository.AttendanceRepository;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.repository.SubjectRepository;
import com.university.erp.lecture.entity.Lecture;
import com.university.erp.lecture.repository.LectureRepository;
import com.university.erp.student.entity.Enrollment;
import com.university.erp.student.entity.Student;
import com.university.erp.student.repository.EnrollmentRepository;
import com.university.erp.student.repository.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repository;
    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;

    public AttendanceServiceImpl(AttendanceRepository repository, LectureRepository lectureRepository,
            StudentRepository studentRepository, EnrollmentRepository enrollmentRepository,
            SubjectRepository subjectRepository) {
        this.repository = repository;
        this.lectureRepository = lectureRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    public AttendanceResponse create(AttendanceRequest r) {
        validateRequest(r);
        verifyLectureExists(r.lectureId());
        verifyStudentExists(r.studentId());
        validateClassMembership(r.studentId());

        if (repository.existsByLectureIdAndStudentId(r.lectureId(), r.studentId())) {
            throw conflict("Attendance is already marked for this student and lecture");
        }

        Attendance a = new Attendance();
        AttendanceMapper.apply(r, a);
        a.setCreatedBy(r.markedBy());

        try {
            return AttendanceMapper.toResponse(repository.saveAndFlush(a));
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Attendance is already marked for this student and lecture");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse get(Long id) {
        return AttendanceMapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(Long lectureId, UUID studentId, AttendanceStatus status) {
        return list(lectureId, studentId, status, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(Long lectureId, UUID studentId, AttendanceStatus status, Long subjectId,
            String division, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<Attendance> records = repository.findWithFilters(lectureId, studentId, status, subjectId, division,
                startDateTime, endDateTime);

        return records.stream().map(AttendanceMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> listPaged(Long lectureId, UUID studentId, AttendanceStatus status, Long subjectId,
            String division, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<Attendance> page = repository.findWithFilters(lectureId, studentId, status, subjectId, division,
                startDateTime, endDateTime, pageable);

        return page.map(AttendanceMapper::toResponse);
    }

    @Override
    public AttendanceResponse update(Long id, AttendanceRequest r) {
        validateRequest(r);
        Attendance a = find(id);

        verifyLectureExists(r.lectureId());
        verifyStudentExists(r.studentId());
        validateClassMembership(r.studentId());

        if ((!a.getLectureId().equals(r.lectureId()) || !a.getStudentId().equals(r.studentId()))
                && repository.existsByLectureIdAndStudentId(r.lectureId(), r.studentId())) {
            throw conflict("Attendance is already marked for this student and lecture");
        }

        AttendanceMapper.apply(r, a);
        try {
            return AttendanceMapper.toResponse(repository.saveAndFlush(a));
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Attendance is already marked for this student and lecture");
        }
    }

    @Override
    public void delete(Long id) {
        Attendance a = find(id);
        Lecture lecture = lectureRepository.findById(a.getLectureId()).orElse(null);
        if (lecture != null && "Archived".equalsIgnoreCase(lecture.getStatus())) {
            throw badRequest("Cannot delete attendance record for an archived lecture");
        }
        repository.delete(a);
    }

    @Override
    public List<AttendanceResponse> markBulk(List<AttendanceRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw badRequest("At least one attendance record is required");
        }

        Set<String> keys = new HashSet<>();
        for (AttendanceRequest r : requests) {
            validateRequest(r);
            if (!keys.add(r.lectureId() + ":" + r.studentId())) {
                throw badRequest("Duplicate student and lecture in batch: lectureId=" + r.lectureId() + ", studentId="
                        + r.studentId());
            }
        }

        Set<Long> lectureIds = requests.stream().map(AttendanceRequest::lectureId).collect(Collectors.toSet());
        for (Long lectureId : lectureIds) {
            verifyLectureExists(lectureId);
        }

        Set<UUID> studentIds = requests.stream().map(AttendanceRequest::studentId).collect(Collectors.toSet());
        for (UUID studentId : studentIds) {
            verifyStudentExists(studentId);
            validateClassMembership(studentId);
        }

        List<AttendanceResponse> responses = new ArrayList<>();
        try {
            for (AttendanceRequest r : requests) {
                Optional<Attendance> existing = repository.findByLectureIdAndStudentId(r.lectureId(), r.studentId());
                Attendance record;
                if (existing.isPresent()) {
                    record = existing.get();
                    AttendanceMapper.apply(r, record);
                } else {
                    record = new Attendance();
                    AttendanceMapper.apply(r, record);
                    record.setCreatedBy(r.markedBy());
                }
                responses.add(AttendanceMapper.toResponse(repository.save(record)));
            }
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Database integrity violation during bulk attendance marking");
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummary summary(UUID studentId, Long lectureId) {
        verifyStudentExists(studentId);
        if (lectureId != null) {
            verifyLectureExists(lectureId);
        }

        long total = lectureId == null ? repository.countByStudentId(studentId)
                : repository.countByStudentIdAndLectureId(studentId, lectureId);
        long present = lectureId == null ? repository.countByStudentIdAndStatus(studentId, AttendanceStatus.Present)
                : repository.countByStudentIdAndLectureIdAndStatus(studentId, lectureId, AttendanceStatus.Present);
        long absent = lectureId == null ? repository.countByStudentIdAndStatus(studentId, AttendanceStatus.Absent)
                : repository.countByStudentIdAndLectureIdAndStatus(studentId, lectureId, AttendanceStatus.Absent);
        long cwork = lectureId == null ? repository.countByStudentIdAndStatus(studentId, AttendanceStatus.C_WORK)
                : repository.countByStudentIdAndLectureIdAndStatus(studentId, lectureId, AttendanceStatus.C_WORK);

        double percentage = total == 0 ? 0.0 : Math.round((present + cwork) * 10000.0 / total) / 100.0;
        return new AttendanceSummary(studentId, total, present, absent, cwork, percentage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectAttendanceSummary> getSubjectSummaries(UUID studentId) {
        verifyStudentExists(studentId);
        List<Object[]> rows = repository.getSubjectAttendanceSummaryByStudentId(studentId);

        List<SubjectAttendanceSummary> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long subjectId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long present = ((Number) row[2]).longValue();
            long absent = ((Number) row[3]).longValue();
            long cwork = ((Number) row[4]).longValue();

            String code = null;
            String name = null;
            if (subjectId != null) {
                Subject subject = subjectRepository.findById(subjectId).orElse(null);
                if (subject != null) {
                    code = subject.getSubjectCode();
                    name = subject.getSubjectName();
                }
            }

            double percentage = total == 0 ? 0.0 : Math.round((present + cwork) * 10000.0 / total) / 100.0;
            result.add(new SubjectAttendanceSummary(subjectId, code, name, total, present, absent, cwork, percentage));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public LectureAttendanceRegister getLectureRegister(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));

        String subjectName = null;
        if (lecture.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(lecture.getSubjectId()).orElse(null);
            if (subject != null) {
                subjectName = subject.getSubjectName();
            }
        }

        List<Attendance> records = repository.findByLectureId(lectureId);
        long totalMarked = records.size();
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.Present).count();
        long absent = records.stream().filter(a -> a.getStatus() == AttendanceStatus.Absent).count();
        long cwork = records.stream().filter(a -> a.getStatus() == AttendanceStatus.C_WORK).count();
        double rate = totalMarked == 0 ? 0.0 : Math.round((present + cwork) * 10000.0 / totalMarked) / 100.0;

        List<AttendanceResponse> responseList = records.stream().map(AttendanceMapper::toResponse).toList();

        return new LectureAttendanceRegister(lecture.getId(), lecture.getSubjectId(), subjectName,
                lecture.getFacultyId(), lecture.getLectureDate(), lecture.getStartTime(), lecture.getEndTime(),
                lecture.getLectureType(), lecture.getTopic(), lecture.getStatus(), totalMarked, present, absent, cwork,
                rate, responseList);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceHistoryResponse getStudentHistory(UUID studentId, Long subjectId, LocalDate startDate,
            LocalDate endDate) {
        Student student = studentRepository.findById(studentId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(studentId);
        Enrollment enrollment = enrollments.stream().filter(e -> "Active".equalsIgnoreCase(e.getEnrollmentStatus()))
                .findFirst().orElse(enrollments.isEmpty() ? null : enrollments.get(0));

        String studentName = Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));

        Integer rollNo = enrollment != null ? enrollment.getRollNo() : null;
        String division = enrollment != null ? enrollment.getDivision() : null;

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<Attendance> attendances = repository.findWithFilters(null, studentId, null, subjectId, null, startDateTime,
                endDateTime);

        List<AttendanceHistoryRecord> historyRecords = new ArrayList<>();
        long total = attendances.size();
        long attended = 0;

        for (Attendance a : attendances) {
            Lecture lecture = lectureRepository.findById(a.getLectureId()).orElse(null);
            LocalDate lDate = lecture != null ? lecture.getLectureDate() : a.getMarkedAt().toLocalDate();
            LocalTime sTime = lecture != null ? lecture.getStartTime() : null;
            LocalTime eTime = lecture != null ? lecture.getEndTime() : null;
            Long subId = lecture != null ? lecture.getSubjectId() : null;
            String subName = null;

            if (subId != null) {
                Subject subject = subjectRepository.findById(subId).orElse(null);
                if (subject != null) {
                    subName = subject.getSubjectName();
                }
            }

            if (a.getStatus() == AttendanceStatus.Present || a.getStatus() == AttendanceStatus.C_WORK) {
                attended++;
            }

            historyRecords.add(new AttendanceHistoryRecord(a.getId(), a.getLectureId(), lDate, sTime, eTime, subId,
                    subName, a.getStatus(), a.getRemarks(), a.getMarkedAt(), a.getCreatedBy()));
        }

        double overallPercentage = total == 0 ? 0.0 : Math.round(attended * 10000.0 / total) / 100.0;

        return new StudentAttendanceHistoryResponse(student.getStudentId(), student.getEnrollmentNo(), studentName,
                rollNo, division, total, attended, overallPercentage, historyRecords);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDefaulterResponse> getDefaulters(Double threshold, Long subjectId, String division) {
        double cutOff = threshold != null ? threshold : 75.0;
        List<Object[]> rows = repository.getStudentOverallAttendanceCounts(subjectId);

        List<AttendanceDefaulterResponse> defaulters = new ArrayList<>();

        for (Object[] row : rows) {
            UUID studentId = (UUID) row[0];
            long total = ((Number) row[1]).longValue();
            long attended = ((Number) row[2]).longValue();

            double percentage = total == 0 ? 0.0 : Math.round(attended * 10000.0 / total) / 100.0;

            if (percentage < cutOff) {
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student == null) {
                    continue;
                }

                List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(studentId);
                Enrollment enrollment = enrollments.stream()
                        .filter(e -> "Active".equalsIgnoreCase(e.getEnrollmentStatus())).findFirst()
                        .orElse(enrollments.isEmpty() ? null : enrollments.get(0));

                String studentDivision = enrollment != null ? enrollment.getDivision() : null;
                if (division != null && !division.isBlank() && !division.equalsIgnoreCase(studentDivision)) {
                    continue;
                }

                String studentName = Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                        .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));

                String status = percentage < 50.0 ? "Critical" : "Warning";

                defaulters.add(new AttendanceDefaulterResponse(student.getStudentId(), student.getEnrollmentNo(),
                        studentName, enrollment != null ? enrollment.getRollNo() : null, studentDivision, total,
                        attended, percentage, status));
            }
        }

        defaulters.sort(Comparator.comparingDouble(AttendanceDefaulterResponse::percentage));
        return defaulters;
    }

    private Attendance find(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found: " + id));
    }

    private void verifyLectureExists(Long lectureId) {
        if (lectureId == null || !lectureRepository.existsById(lectureId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId);
        }
    }

    private void verifyStudentExists(UUID studentId) {
        if (studentId == null || !studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId);
        }
    }

    private void validateClassMembership(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentStudentId(studentId);
        if (enrollments != null && !enrollments.isEmpty()) {
            boolean hasActive = enrollments.stream().anyMatch(e -> "Active".equalsIgnoreCase(e.getEnrollmentStatus()));
            if (!hasActive) {
                throw badRequest("Student does not have an active enrollment: " + studentId);
            }
        }
    }

    private void validateRequest(AttendanceRequest r) {
        if (r == null || r.lectureId() == null || r.lectureId() < 1 || r.studentId() == null || r.status() == null) {
            throw badRequest("lectureId, studentId and status are required");
        }
    }

    private ResponseStatusException badRequest(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private ResponseStatusException conflict(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }
}
