package de.lemke.studiportal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    version = 2,
    entities = [
        ExamDb::class,
    ],
    exportSchema = true,
    autoMigrations = [],

)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //delete column id and set new primary key to examNumber + semester
        //Drop column isn't supported by SQLite, so the data must manually be moved
        with(db) {
            execSQL("""CREATE TABLE exam_backup (
                |examNumber TEXT NOT NULL, 
                |name TEXT NOT NULL, 
                |bonus TEXT NOT NULL, 
                |malus TEXT NOT NULL, 
                |ects TEXT NOT NULL, 
                |sws TEXT NOT NULL, 
                |semester TEXT NOT NULL, 
                |kind TEXT NOT NULL, 
                |tryCount TEXT NOT NULL, 
                |grade TEXT NOT NULL, 
                |state TEXT NOT NULL, 
                |comment TEXT NOT NULL, 
                |isResignated INTEGER NOT NULL, 
                |note TEXT NOT NULL, 
                |category TEXT NOT NULL, 
                |PRIMARY KEY(examNumber, semester)
                |)""".trimMargin())
            execSQL("DELETE FROM exam WHERE examNumber = 'separator'")
            execSQL("""INSERT INTO exam_backup SELECT examNumber, name, bonus, malus, ects, sws, semester, kind, tryCount, grade, state, comment, isResignated, note, category FROM exam""")
            execSQL("DROP TABLE exam")
            execSQL("ALTER TABLE exam_backup RENAME to exam")
        }
    }
}