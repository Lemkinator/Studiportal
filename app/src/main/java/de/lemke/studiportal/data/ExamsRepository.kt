package de.lemke.studiportal.data

import de.lemke.studiportal.data.database.ExamDao
import de.lemke.studiportal.data.database.examFromDb
import de.lemke.studiportal.data.database.examToDb
import de.lemke.studiportal.domain.model.Exam
import javax.inject.Inject

class ExamsRepository @Inject constructor(
    private val examDao: ExamDao
) {
    suspend fun deleteAll() = examDao.deleteAll()

    suspend fun getExams(): List<Exam> = examDao.getAll().map { examFromDb(it) }

    suspend fun getExamsWithCategory(category: String): List<Exam> = examDao.getAll(category).map { examFromDb(it) }

    suspend fun getExam(examNumber: String?): Exam? = examDao.getByExamNumber(examNumber)?.let { examFromDb(it) }

    private suspend fun insertExams(exams: List<Exam>) = examDao.insert(exams.map { examToDb(it) })

    suspend fun updateExams(exams: List<Exam>) {
        deleteAll()
        insertExams(exams)
    }
}