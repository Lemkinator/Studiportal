package de.lemke.studiportal.data.database

import de.lemke.studiportal.domain.model.Exam

fun examFromDb(examDb: ExamDb): Exam {
    return Exam(
        examNumber = examDb.examNumber,
        name = examDb.name,
        bonus = examDb.bonus,
        malus = examDb.malus,
        ects = examDb.ects,
        sws = examDb.sws,
        semester = examDb.semester,
        kind = examDb.kind,
        tryCount = examDb.tryCount,
        grade = examDb.grade,
        state = Exam.State.fromString(examDb.state),
        comment = examDb.comment,
        isResignated = examDb.isResignated,
        note = Exam.Note.fromString(examDb.note),
        category = examDb.category,
    )
}

fun examToDb(exam: Exam): ExamDb {
    return ExamDb(
        examNumber = exam.examNumber,
        name = exam.name,
        bonus = exam.bonus,
        malus = exam.malus,
        ects = exam.ects,
        sws = exam.sws,
        semester = exam.semester,
        kind = exam.kind,
        tryCount = exam.tryCount,
        grade = exam.grade,
        state = exam.state.toString(),
        comment = exam.comment,
        isResignated = exam.isResignated,
        note = exam.note.toString(),
        category = exam.category,
    )
}