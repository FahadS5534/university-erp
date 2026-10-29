BEGIN;

DO $migration$
BEGIN
    -- Preserve the Subject type data while correcting its misspelled column.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'subject' AND column_name = 'subejct_type'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'subject' AND column_name = 'subject_type'
    ) THEN
        ALTER TABLE public.subject RENAME COLUMN subejct_type TO subject_type;
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'subject' AND column_name = 'subject_type'
    ) THEN
        RAISE EXCEPTION 'Neither subject.subejct_type nor subject.subject_type exists';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.departments
        WHERE hod_faculty_id IS NOT NULL
        GROUP BY hod_faculty_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique HOD constraint: faculty is HOD for multiple departments';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.lecture
        WHERE classroom_id < -2147483648 OR classroom_id > 2147483647
    ) THEN
        RAISE EXCEPTION 'Cannot convert lecture.classroom_id to INTEGER: value is out of range';
    END IF;

    IF EXISTS (SELECT 1 FROM public.enrollment WHERE abs(final_cgpa) > 99.99)
       OR EXISTS (SELECT 1 FROM public.fee WHERE abs(amount) > 9999999999.99)
       OR EXISTS (SELECT 1 FROM public.fee_payment WHERE abs(total_amount) > 9999999999.99 OR abs(paid_amount) > 9999999999.99 OR abs(due_amount) > 9999999999.99)
       OR EXISTS (SELECT 1 FROM public.examination WHERE abs(max_marks) > 99999.99)
       OR EXISTS (SELECT 1 FROM public.grading WHERE abs(marks_obtained) > 99999.99 OR abs(grade_point) > 99.99)
    THEN
        RAISE EXCEPTION 'Cannot narrow numeric column precision: existing value is out of range';
    END IF;

    IF EXISTS (SELECT 1 FROM public.classroom WHERE timetable_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Cannot drop classroom.timetable_id: existing values need migration';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.library_transaction t
        LEFT JOIN public.library_book b ON b.id = t.book_id WHERE b.id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM public.library_transaction t
        LEFT JOIN public.student s ON s.student_id = t.student_id WHERE s.student_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot add library transaction foreign keys: orphaned references exist';
    END IF;

    ALTER TABLE public.enrollment ALTER COLUMN final_cgpa TYPE numeric(4,2) USING final_cgpa::numeric(4,2);
    ALTER TABLE public.fee ALTER COLUMN amount TYPE numeric(12,2) USING amount::numeric(12,2);
    ALTER TABLE public.fee_payment ALTER COLUMN total_amount TYPE numeric(12,2) USING total_amount::numeric(12,2);
    ALTER TABLE public.fee_payment ALTER COLUMN paid_amount TYPE numeric(12,2) USING paid_amount::numeric(12,2);
    ALTER TABLE public.fee_payment ALTER COLUMN due_amount TYPE numeric(12,2) USING due_amount::numeric(12,2);
    ALTER TABLE public.examination ALTER COLUMN max_marks TYPE numeric(7,2) USING max_marks::numeric(7,2);
    ALTER TABLE public.grading ALTER COLUMN marks_obtained TYPE numeric(7,2) USING marks_obtained::numeric(7,2);
    ALTER TABLE public.grading ALTER COLUMN grade_point TYPE numeric(4,2) USING grade_point::numeric(4,2);

    ALTER TABLE public.lecture DROP CONSTRAINT IF EXISTS fk_lecture_classroom;
    ALTER TABLE public.lecture ALTER COLUMN classroom_id TYPE integer USING classroom_id::integer;
    ALTER TABLE public.lecture ADD CONSTRAINT fk_lecture_classroom
        FOREIGN KEY (classroom_id) REFERENCES public.classroom(classroom_id);

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.departments'::regclass AND conname = 'uq_departments_hod_faculty_id'
    ) THEN
        ALTER TABLE public.departments ADD CONSTRAINT uq_departments_hod_faculty_id UNIQUE (hod_faculty_id);
    END IF;

    ALTER TABLE public.classroom DROP COLUMN IF EXISTS timetable_id;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.library_transaction'::regclass AND conname = 'fk_library_transaction_book'
    ) THEN
        ALTER TABLE public.library_transaction ADD CONSTRAINT fk_library_transaction_book
            FOREIGN KEY (book_id) REFERENCES public.library_book(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.library_transaction'::regclass AND conname = 'fk_library_transaction_student'
    ) THEN
        ALTER TABLE public.library_transaction ADD CONSTRAINT fk_library_transaction_student
            FOREIGN KEY (student_id) REFERENCES public.student(student_id);
    END IF;

    -- Timetable.subject_id remains UUID in the live schema and contains a UUID
    -- with no mapping to subject.subject_id (BIGINT). Do not guess a mapping.
END
$migration$;

COMMIT;
