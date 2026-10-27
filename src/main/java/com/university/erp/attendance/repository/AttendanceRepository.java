package com.university.erp.attendance.repository;

import com.university.erp.attendance.entity.Attendance;
import com.university.erp.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByLectureIdAndStudentId(Long lectureId, UUID studentId);

    boolean existsByLectureIdAndStudentIdAndIdNot(Long lectureId, UUID studentId, Long id);

    Optional<Attendance> findByLectureIdAndStudentId(Long lectureId, UUID studentId);

    List<Attendance> findByLectureId(Long lectureId);

    Page<Attendance> findByLectureId(Long lectureId, Pageable pageable);

    List<Attendance> findByStudentId(UUID studentId);

    Page<Attendance> findByStudentId(UUID studentId, Pageable pageable);

    List<Attendance> findByLectureIdAndStatus(Long lectureId, AttendanceStatus status);

    List<Attendance> findByStudentIdAndLectureId(UUID studentId, Long lectureId);

    long countByStudentId(UUID studentId);

    long countByStudentIdAndLectureId(UUID studentId, Long lectureId);

    long countByStudentIdAndStatus(UUID studentId, AttendanceStatus status);

    long countByStudentIdAndLectureIdAndStatus(UUID studentId, Long lectureId, AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE " + "(:lectureId IS NULL OR a.lectureId = :lectureId) AND "
            + "(:studentId IS NULL OR a.studentId = :studentId) AND " + "(:status IS NULL OR a.status = :status) AND "
            + "(:subjectId IS NULL OR a.lectureId IN (SELECT l.id FROM Lecture l WHERE l.subjectId = :subjectId)) AND "
            + "(:division IS NULL OR a.studentId IN (SELECT e.student.studentId FROM Enrollment e WHERE e.division = :division)) AND "
            + "(:startDateTime IS NULL OR a.markedAt >= :startDateTime) AND "
            + "(:endDateTime IS NULL OR a.markedAt <= :endDateTime)")
    List<Attendance> findWithFilters(@Param("lectureId") Long lectureId, @Param("studentId") UUID studentId,
            @Param("status") AttendanceStatus status, @Param("subjectId") Long subjectId,
            @Param("division") String division, @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query(value = "SELECT a FROM Attendance a WHERE " + "(:lectureId IS NULL OR a.lectureId = :lectureId) AND "
            + "(:studentId IS NULL OR a.studentId = :studentId) AND " + "(:status IS NULL OR a.status = :status) AND "
            + "(:subjectId IS NULL OR a.lectureId IN (SELECT l.id FROM Lecture l WHERE l.subjectId = :subjectId)) AND "
            + "(:division IS NULL OR a.studentId IN (SELECT e.student.studentId FROM Enrollment e WHERE e.division = :division)) AND "
            + "(:startDateTime IS NULL OR a.markedAt >= :startDateTime) AND "
            + "(:endDateTime IS NULL OR a.markedAt <= :endDateTime)", countQuery = "SELECT COUNT(a) FROM Attendance a WHERE "
                    + "(:lectureId IS NULL OR a.lectureId = :lectureId) AND "
                    + "(:studentId IS NULL OR a.studentId = :studentId) AND "
                    + "(:status IS NULL OR a.status = :status) AND "
                    + "(:subjectId IS NULL OR a.lectureId IN (SELECT l.id FROM Lecture l WHERE l.subjectId = :subjectId)) AND "
                    + "(:division IS NULL OR a.studentId IN (SELECT e.student.studentId FROM Enrollment e WHERE e.division = :division)) AND "
                    + "(:startDateTime IS NULL OR a.markedAt >= :startDateTime) AND "
                    + "(:endDateTime IS NULL OR a.markedAt <= :endDateTime)")
    Page<Attendance> findWithFilters(@Param("lectureId") Long lectureId, @Param("studentId") UUID studentId,
            @Param("status") AttendanceStatus status, @Param("subjectId") Long subjectId,
            @Param("division") String division, @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime, Pageable pageable);

    @Query("SELECT l.subjectId, " + "COUNT(a), "
            + "SUM(CASE WHEN a.status = com.university.erp.attendance.entity.AttendanceStatus.Present THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN a.status = com.university.erp.attendance.entity.AttendanceStatus.Absent THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN a.status = com.university.erp.attendance.entity.AttendanceStatus.C_WORK THEN 1L ELSE 0L END) "
            + "FROM Attendance a, Lecture l " + "WHERE a.lectureId = l.id AND a.studentId = :studentId "
            + "GROUP BY l.subjectId")
    List<Object[]> getSubjectAttendanceSummaryByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT a.studentId, " + "COUNT(a), "
            + "SUM(CASE WHEN a.status = com.university.erp.attendance.entity.AttendanceStatus.Present OR a.status = com.university.erp.attendance.entity.AttendanceStatus.C_WORK THEN 1L ELSE 0L END) "
            + "FROM Attendance a, Lecture l " + "WHERE a.lectureId = l.id "
            + "AND (:subjectId IS NULL OR l.subjectId = :subjectId) "
            + "AND (l.status IS NULL OR LOWER(l.status) != 'cancelled') " + "GROUP BY a.studentId")
    List<Object[]> getStudentOverallAttendanceCounts(@Param("subjectId") Long subjectId);
}
