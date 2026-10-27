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

    @Override
    @Transactional(readOnly = true)
    public MonthlyAttendanceCalendarResponse getMonthlyCalendar(UUID studentId, int year, int month) {
        try {
            AttendanceCalendarValidator.validateMonthAndYear(year, month);
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        }
        Student student = findStudent(studentId);
        String studentName = formatStudentName(student);
        String enrollmentNo = student.getEnrollmentNo();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        int daysInMonth = startDate.lengthOfMonth();
        String monthName = AttendanceCalendarValidator.getMonthName(month);

        List<Object[]> records = repository.findMonthlyAttendanceRecords(studentId, startDate, endDate);

        Set<Long> subjectIds = records.stream()
                .map(r -> ((Lecture) r[1]).getSubjectId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Subject> subjectMap = subjectIds.isEmpty()
                ? Collections.emptyMap()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, s -> s));

        Map<LocalDate, List<DailyAttendanceItem>> itemsByDate = new LinkedHashMap<>();
        for (Object[] row : records) {
            Attendance a = (Attendance) row[0];
            Lecture l = (Lecture) row[1];
            Subject s = subjectMap.get(l.getSubjectId());
            String subjectName = s != null ? s.getSubjectName() : "Unknown";
            String subjectCode = s != null ? s.getSubjectCode() : null;

            DailyAttendanceItem item = new DailyAttendanceItem(
                    a.getId(),
                    l.getId(),
                    l.getLectureDate(),
                    l.getStartTime(),
                    l.getEndTime(),
                    l.getSubjectId(),
                    subjectCode,
                    subjectName,
                    a.getStatus(),
                    a.getRemarks(),
                    a.getMarkedAt(),
                    a.getCreatedBy()
            );

            itemsByDate.computeIfAbsent(l.getLectureDate(), k -> new ArrayList<>()).add(item);
        }

        List<CalendarDayAttendance> days = new ArrayList<>(daysInMonth);
        long presentCount = 0;
        long absentCount = 0;
        long cWorkCount = 0;
        int daysWithAttendance = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            List<DailyAttendanceItem> itemsForDay = itemsByDate.getOrDefault(date, Collections.emptyList());
            int dayPresent = 0;
            int dayAbsent = 0;
            int dayCWork = 0;

            if (!itemsForDay.isEmpty()) {
                daysWithAttendance++;
                for (DailyAttendanceItem item : itemsForDay) {
                    if (item.status() == AttendanceStatus.Present) {
                        dayPresent++;
                        presentCount++;
                    } else if (item.status() == AttendanceStatus.Absent) {
                        dayAbsent++;
                        absentCount++;
                    } else if (item.status() == AttendanceStatus.C_WORK) {
                        dayCWork++;
                        cWorkCount++;
                    }
                }
            }
            days.add(new CalendarDayAttendance(date, date.getDayOfMonth(), date.getDayOfWeek().name(),
                    itemsForDay.size(), dayPresent, dayAbsent, dayCWork, itemsForDay));
        }

        long totalMarked = presentCount + absentCount + cWorkCount;
        double percentage = AttendanceCalendarValidator.calculatePercentage(presentCount, cWorkCount, totalMarked);

        return new MonthlyAttendanceCalendarResponse(studentId, studentName, enrollmentNo, year, month, monthName,
                daysInMonth, daysWithAttendance, totalMarked, presentCount, absentCount, cWorkCount, percentage, days);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyAttendanceSummaryResponse getMonthlySummary(UUID studentId, int year, int month) {
        try {
            AttendanceCalendarValidator.validateMonthAndYear(year, month);
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        }
        Student student = findStudent(studentId);
        String studentName = formatStudentName(student);
        String enrollmentNo = student.getEnrollmentNo();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        String monthName = AttendanceCalendarValidator.getMonthName(month);

        List<Object[]> aggResults = repository.getMonthlySummaryAggregation(studentId, startDate, endDate);

        long total = 0L;
        long present = 0L;
        long absent = 0L;
        long cWork = 0L;
        int daysWithAttendance = 0;

        if (aggResults != null && !aggResults.isEmpty()) {
            Object[] row = aggResults.get(0);
            total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            present = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            absent = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            cWork = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            daysWithAttendance = row[4] != null ? ((Number) row[4]).intValue() : 0;
        }

        double percentage = AttendanceCalendarValidator.calculatePercentage(present, cWork, total);

        return new MonthlyAttendanceSummaryResponse(studentId, studentName, enrollmentNo, year, month, monthName,
                total, present, absent, cWork, percentage, daysWithAttendance);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceTrendsResponse getAttendanceTrends(UUID studentId, LocalDate startDate, LocalDate endDate) {
        try {
            AttendanceCalendarValidator.validateDateRange(startDate, endDate);
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        }
        Student student = findStudent(studentId);
        String studentName = formatStudentName(student);
        String enrollmentNo = student.getEnrollmentNo();

        List<Object[]> subjectAgg = repository.getSubjectTrendsAggregation(studentId, startDate, endDate);
        List<Object[]> monthlyAgg = repository.getSubjectMonthlyTrendsAggregation(studentId, startDate, endDate);

        Set<Long> subjectIds = new HashSet<>();
        if (subjectAgg != null) {
            for (Object[] row : subjectAgg) {
                if (row[0] != null) {
                    subjectIds.add(((Number) row[0]).longValue());
                }
            }
        }
        if (monthlyAgg != null) {
            for (Object[] row : monthlyAgg) {
                if (row[0] != null) {
                    subjectIds.add(((Number) row[0]).longValue());
                }
            }
        }

        Map<Long, Subject> subjectMap = subjectIds.isEmpty()
                ? Collections.emptyMap()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, s -> s));

        Map<Long, List<MonthlySubjectAttendanceTrend>> monthlyBySubject = new LinkedHashMap<>();
        if (monthlyAgg != null) {
            for (Object[] row : monthlyAgg) {
                Long subjId = ((Number) row[0]).longValue();
                int year = ((Number) row[1]).intValue();
                int month = ((Number) row[2]).intValue();
                long total = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                long present = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                long absent = row[5] != null ? ((Number) row[5]).longValue() : 0L;
                long cWork = row[6] != null ? ((Number) row[6]).longValue() : 0L;
                double pct = AttendanceCalendarValidator.calculatePercentage(present, cWork, total);
                String monthName = AttendanceCalendarValidator.getMonthName(month);

                MonthlySubjectAttendanceTrend mTrend = new MonthlySubjectAttendanceTrend(
                        year, month, monthName, total, present, absent, cWork, pct);
                monthlyBySubject.computeIfAbsent(subjId, k -> new ArrayList<>()).add(mTrend);
            }
        }

        List<SubjectAttendanceTrend> trends = new ArrayList<>();
        long overallTotal = 0L;
        long overallPresent = 0L;
        long overallCWork = 0L;

        if (subjectAgg != null) {
            for (Object[] row : subjectAgg) {
                Long subjId = ((Number) row[0]).longValue();
                long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long present = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                long absent = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                long cWork = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                double pct = AttendanceCalendarValidator.calculatePercentage(present, cWork, total);

                overallTotal += total;
                overallPresent += present;
                overallCWork += cWork;

                Subject subject = subjectMap.get(subjId);
                String subjectName = subject != null ? subject.getSubjectName() : "Unknown";
                String subjectCode = subject != null ? subject.getSubjectCode() : null;

                List<MonthlySubjectAttendanceTrend> monthlyBreakdown =
                        monthlyBySubject.getOrDefault(subjId, Collections.emptyList());

                trends.add(new SubjectAttendanceTrend(
                        subjId, subjectCode, subjectName, total, present, absent, cWork, pct, monthlyBreakdown));
            }
        }

        long overallAttended = overallPresent + overallCWork;
        double overallPct = AttendanceCalendarValidator.calculatePercentage(overallPresent, overallCWork, overallTotal);

        return new AttendanceTrendsResponse(studentId, studentName, enrollmentNo, startDate, endDate, overallTotal,
                overallAttended, overallPct, trends);
    }

    private Student findStudent(UUID studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId);
        }
        return studentRepository.findById(studentId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
    }

    private String formatStudentName(Student student) {
        if (student == null) {
            return null;
        }
        return Stream.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
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
