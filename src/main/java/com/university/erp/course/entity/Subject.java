package com.university.erp.course.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Entity
@Table(name = "subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Long id;

    @Column(name = "subject_code", nullable = false, unique = true)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "in_semester")
    private Short inSemester;

    @Column(nullable = false)
    private Short credits;

    @Column(name = "subject_type", nullable = false)
    private String subjectType;

    @Column(name = "subject_hours", nullable = false)
    private Integer subjectHours;
}
