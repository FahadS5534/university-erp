package com.university.erp.lecture.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.university.erp.course.entity.Subject;
import com.university.erp.department.entity.Faculty;
import com.university.erp.classroom.entity.Classroom;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "lecture")
@Getter
@Setter
@NoArgsConstructor
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long id;
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
    @Column(name = "faculty_id", nullable = false)
    private Integer facultyId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", insertable = false, updatable = false)
    private Faculty faculty;
    @Column(name = "classroom_id")
    private Integer classroomId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;
    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(name = "lecture_type")
    private String lectureType;
    private String topic;
    private String status = "Scheduled";
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
