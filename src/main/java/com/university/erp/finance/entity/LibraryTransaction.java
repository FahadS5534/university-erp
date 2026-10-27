package com.university.erp.finance.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.university.erp.student.entity.Student;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "library_transaction")
@Getter
@Setter
@NoArgsConstructor
public class LibraryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long bookId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private LibraryBook book;
    @Column(nullable = false)
    private UUID studentId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;
    @Column(nullable = false)
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status = "ISSUED";
}
