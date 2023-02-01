package de.lemke.studiportal.data.database

import androidx.room.Entity

@Entity(
    tableName = "exam",
    primaryKeys = ["examNumber", "semester"],
)
data class ExamDb(
    val examNumber: String,
    val name: String,
    val bonus: String,
    val malus: String,
    val ects: String,
    val sws: String,
    val semester: String,
    val kind: String,
    val tryCount: String,
    val grade: String,
    val state: String,
    val comment: String,
    val isResignated: Boolean,
    val note: String,
    val category: String,
)
