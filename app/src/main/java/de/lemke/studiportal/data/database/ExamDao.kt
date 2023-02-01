package de.lemke.studiportal.data.database

import androidx.room.*

@Dao
interface ExamDao {

    @Transaction
    suspend fun replaceAll(exams: List<ExamDb>) {
        deleteAll()
        exams.forEach { insert(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamDb)

    @Query("SELECT * FROM exam WHERE category = :category")
    suspend fun getAll(category: String): List<ExamDb>

    @Query("SELECT * FROM exam WHERE examNumber = :examNumber")
    suspend fun getByExamNumber(examNumber: String?): ExamDb?

    @Query("SELECT * FROM exam;")
    suspend fun getAll(): List<ExamDb>

    @Query("DELETE FROM exam")
    suspend fun deleteAll()
}
